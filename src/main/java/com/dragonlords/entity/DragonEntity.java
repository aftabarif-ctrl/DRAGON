package com.dragonlords.entity;

import com.dragonlords.DragonType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class DragonEntity extends TamableAnimal {
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BREATHING = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);

    public final DragonType dragonType;

    // gameplay state (server)
    private int breathTicks, breathSustain, breathCooldown, chargeTicks, chargeCooldown, takeoffTicks, flyingTime;
    int meleeCooldown;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<Integer> chargeHit = new HashSet<>();

    // animation state (both sides)
    public float flapPhase, flapPhaseO, flapAmp, flapAmpO, flyAnim, flyAnimO, bank, bankO, bodyPitch, bodyPitchO, jaw, jawO;

    public DragonEntity(EntityType<? extends DragonEntity> type, Level level) {
        super(type, level);
        this.dragonType = DragonType.byEntityPath(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
        this.moveControl = new DragonMoveControl(this);
        this.xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes(DragonType t) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, t.health)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FLYING_SPEED, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.STEP_HEIGHT, 1.6);
    }

    public static boolean checkDragonSpawn(EntityType<DragonEntity> t, ServerLevelAccessor lvl, MobSpawnType reason, BlockPos pos, RandomSource rnd) {
        if (reason == MobSpawnType.SPAWNER || reason == MobSpawnType.SPAWN_EGG) return true;
        return lvl.getBlockState(pos).isAir() && rnd.nextInt(3) == 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FLYING, false);
        b.define(SADDLED, false);
        b.define(BREATHING, false);
        b.define(CHARGING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new DragonGoals.CombatGoal(this));
        goalSelector.addGoal(3, new DragonGoals.FollowOwnerGoal(this));
        goalSelector.addGoal(4, new DragonGoals.FlightGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return !DragonEntity.this.isFlying() && !DragonEntity.this.isVehicle() && super.canUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, e -> isTame()));
    }

    // ------------------------------------------------------------------ state accessors
    public boolean isFlying() { return entityData.get(FLYING); }
    public void setFlying(boolean f) { entityData.set(FLYING, f); }
    public boolean isSaddled() { return entityData.get(SADDLED); }
    public void setSaddled(boolean s) { entityData.set(SADDLED, s); }
    public boolean isBreathing() { return entityData.get(BREATHING); }
    public boolean isCharging() { return entityData.get(CHARGING); }

    public float flapPhaseAt(float pt) { return Mth.lerp(pt, flapPhaseO, flapPhase); }
    public float flapAmpAt(float pt) { return Mth.lerp(pt, flapAmpO, flapAmp); }
    public float flyAnimAt(float pt) { return Mth.lerp(pt, flyAnimO, flyAnim); }
    public float bankAt(float pt) { return Mth.lerp(pt, bankO, bank); }
    public float pitchAt(float pt) { return Mth.lerp(pt, bodyPitchO, bodyPitch); }
    public float jawAt(float pt) { return Mth.lerp(pt, jawO, jaw); }

    @Override public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("Saddled", isSaddled());
        t.putBoolean("Flying", isFlying());
    }
    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setSaddled(t.getBoolean("Saddled"));
        setFlying(t.getBoolean("Flying"));
    }

    @Nullable @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, diff, type, data);
        if (random.nextFloat() < 0.6f || !level.getBlockState(blockPosition().below()).blocksMotion()) {
            setFlying(true);
            setDeltaMovement(0, 0.3, 0);
        }
        return data;
    }

    // ------------------------------------------------------------------ taming / riding
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean client = level().isClientSide;
        if (!isTame()) {
            if (stack.is(dragonType.tameFood)) {
                if (!client) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    if (random.nextInt(3) == 0) {
                        tame(player);
                        navigation.stop();
                        setTarget(null);
                        setOrderedToSit(false);
                        level().broadcastEntityEvent(this, (byte) 7);
                    } else level().broadcastEntityEvent(this, (byte) 6);
                }
                return InteractionResult.sidedSuccess(client);
            }
            return InteractionResult.PASS;
        }
        if (!isOwnedBy(player)) return InteractionResult.PASS;
        if (stack.is(Items.SADDLE) && !isSaddled()) {
            if (!client) {
                setSaddled(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                playSound(SoundEvents.HORSE_SADDLE, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (stack.is(dragonType.tameFood) && getHealth() < getMaxHealth()) {
            if (!client) { heal(40); if (!player.getAbilities().instabuild) stack.shrink(1); }
            return InteractionResult.sidedSuccess(client);
        }
        if (player.isSecondaryUseActive()) {
            if (!client) {
                setOrderedToSit(!isOrderedToSit());
                navigation.stop();
                setTarget(null);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (isSaddled() && !isVehicle()) {
            if (!client) player.startRiding(this);
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;
    }

    @Nullable @Override
    public LivingEntity getControllingPassenger() {
        if (isSaddled() && getFirstPassenger() instanceof Player p && isOwnedBy(p)) return p;
        return null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return new Vec3(0, dragonType.scale * 1.375 + 0.2, 0);
    }

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, src, recentlyHit);
        if (isSaddled()) spawnAtLocation(Items.SADDLE);
    }

    // ------------------------------------------------------------------ flight helpers
    public void takeOff() {
        if (isFlying()) return;
        setFlying(true);
        takeoffTicks = 30;
        setOnGround(false);
        setDeltaMovement(getDeltaMovement().add(0, 0.75, 0));
        hasImpulse = true;
    }

    public void land() { setFlying(false); }

    /** Distance from feet to the ground (or water surface) below, 99 if > 6 blocks. */
    public double groundDistance() {
        Vec3 s = position();
        BlockHitResult r = level().clip(new ClipContext(s, s.add(0, -6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        return r.getType() == HitResult.Type.MISS ? 99 : s.y - r.getLocation().y;
    }

    @Override
    public void travel(Vec3 input) {
        LivingEntity rider = getControllingPassenger();
        if (isCharging()) { chargeTravel(rider); return; }
        if (rider != null) { rideTravel(rider, input); return; }
        if (isFlying()) {
            Vec3 dm = getDeltaMovement();
            move(MoverType.SELF, dm);
            setDeltaMovement(dm.scale(0.99));
            return;
        }
        super.travel(input);
    }

    private void chargeTravel(@Nullable LivingEntity rider) {
        Vec3 dir = (rider != null ? rider.getLookAngle() : chargeDir);
        if (dir.lengthSqr() < 1.0E-4) dir = calculateViewVector(0, getYRot());
        Vec3 v = dir.normalize().scale(2.0);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        float yaw = (float) (Mth.atan2(v.z, v.x) * 57.2957763671875) - 90.0f;
        setYRot(Mth.approachDegrees(getYRot(), yaw, 14.0f));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void rideTravel(LivingEntity rider, Vec3 input) {
        setYRot(Mth.approachDegrees(getYRot(), rider.getYRot(), isFlying() ? 5.0f : 14.0f));
        yBodyRot = getYRot();
        yHeadRot = rider.getYRot();
        setXRot(rider.getXRot());
        float fwd = rider.zza;

        if (!isFlying()) {
            setSpeed((float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.15f);
            super.travel(new Vec3(rider.xxa * 0.5f, input.y, fwd));
            return;
        }
        Vec3 dir = calculateViewVector(rider.getXRot(), getYRot());
        double max = getAttributeValue(Attributes.FLYING_SPEED) * 1.4;
        Vec3 desired; double rate;
        if (fwd > 0) {
            double dive = Math.max(0, rider.getXRot()) / 90.0;
            desired = dir.scale(max * (1.0 + dive * 0.6));
            rate = 0.05 + dive * 0.03;
        } else if (fwd < 0) {
            desired = new Vec3(0, -0.03, 0);
            rate = 0.12;
        } else {
            Vec3 h = new Vec3(dir.x, 0, dir.z);
            desired = (h.lengthSqr() > 1e-4 ? h.normalize().scale(max * 0.2) : Vec3.ZERO).add(0, -0.03, 0);
            rate = 0.06;
        }
        Vec3 nv = getDeltaMovement().lerp(desired, rate);
        if (onGround() && fwd > 0 && rider.getXRot() < -5 && nv.y < 0.4) nv = new Vec3(nv.x, 0.45, nv.z); // lift-off
        setDeltaMovement(nv);
        move(MoverType.SELF, nv);
    }

    @Override public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isFood(ItemStack s) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob o) { return null; }
    @Override public boolean canBreatheUnderwater() { return dragonType == DragonType.OCEAN; }
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(dragonType.scale * 2.6); }

    @Override
    public boolean isInvulnerableTo(DamageSource s) {
        return super.isInvulnerableTo(s)
                || s.is(DamageTypes.IN_WALL)
                || (dragonType == DragonType.STORM && s.is(DamageTypes.LIGHTNING_BOLT))
                || (dragonType == DragonType.FIRE && s.is(DamageTypeTags.IS_FIRE))
                || (dragonType == DragonType.ICE && s.is(DamageTypes.FREEZE));
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isFlying() && horizontalCollision && getControllingPassenger() == null && !isCharging())
            setDeltaMovement(getDeltaMovement().add(0, 0.12, 0)); // auto-climb obstacles: never stuck
    }

    @Override
    public void tick() {
        super.tick();
        animationTick();
        if (!level().isClientSide) serverTick();
    }

    private void serverTick() {
        flyingTime = isFlying() ? flyingTime + 1 : 0;
        if (breathCooldown > 0) breathCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;
        if (meleeCooldown > 0) meleeCooldown--;
        if (takeoffTicks > 0) takeoffTicks--;

        LivingEntity rider = getControllingPassenger();
        if (rider != null) {
            if (!isFlying() && rider.zza > 0 && rider.getXRot() < -22 && !isInWater()) { setFlying(true); takeoffTicks = 30; flyingTime = 0; }
            else if (isFlying() && flyingTime > 25 && onGround() && (rider.zza <= 0 || rider.getXRot() > -5)) setFlying(false);
        } else if (isInSittingPose() && isFlying()) setFlying(false);

        setNoGravity(isFlying());
        tickBreath();
        tickCharge();
    }

    private void animationTick() {
        flapPhaseO = flapPhase; flapAmpO = flapAmp; flyAnimO = flyAnim; bankO = bank; bodyPitchO = bodyPitch; jawO = jaw;
        Vec3 dm = getDeltaMovement();
        boolean fly = isFlying();
        double h = dm.horizontalDistance();
        flyAnim += ((fly ? 1f : 0f) - flyAnim) * 0.07f;

        float targetAmp, rate;
        if (fly) {
            boolean gliding = dm.y < -0.06 && h > 0.5;
            if (isCharging()) targetAmp = 0.2f;
            else if (gliding) targetAmp = 0.12f;
            else {
                targetAmp = Mth.clamp(0.6f + (float) dm.y * 2.5f - (float) h * 0.2f, 0.35f, 1.0f);
                if (h > 1.0) targetAmp = Math.min(targetAmp, 0.55f);
            }
            rate = 0.30f + 0.22f * targetAmp;
        } else { targetAmp = 0f; rate = 0.12f; }
        flapAmp += (targetAmp - flapAmp) * 0.12f;
        flapPhase += rate;

        float yawDelta = Mth.wrapDegrees(getYRot() - yRotO);
        bank += ((fly ? Mth.clamp(yawDelta * 5f, -40f, 40f) : 0f) - bank) * 0.12f;
        float tp = fly ? Mth.clamp((float) (Mth.atan2(-dm.y, Math.max(h, 0.25)) * 57.29578) * 0.85f, -50f, 65f) : 0f;
        bodyPitch += (tp - bodyPitch) * 0.1f;
        jaw += ((isBreathing() ? 0.75f : 0f) - jaw) * 0.25f;

        if (level().isClientSide) {
            if (fly && flapAmp > 0.3f && (int) ((flapPhase - 1.0f) / Mth.TWO_PI) != (int) ((flapPhaseO - 1.0f) / Mth.TWO_PI))
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, getSoundSource(), 2.0f, 0.8f + random.nextFloat() * 0.2f, false);
            if (random.nextInt(2) == 0)
                level().addParticle(dragonType.ambient, getRandomX(0.9), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.9), 0, 0.02, 0);
            if (fly && h > 0.4) {
                Vec3 back = calculateViewVector(0, getYRot()).scale(-2.6 * dragonType.scale);
                level().addParticle(dragonType.ambient, getX() + back.x, getY() + 1.0 * dragonType.scale, getZ() + back.z, 0, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------ actions
    public void performAction(int action) {
        LivingEntity rider = getControllingPassenger();
        if (rider == null) return;
        if (action == 0) startBreath(6);
        else if (action == 1) startCharge(rider.getLookAngle());
    }

    public void startBreath(int ticks) { if (breathCooldown <= 0) breathTicks = Math.max(breathTicks, ticks); }

    public boolean startCharge(Vec3 dir) {
        if (chargeCooldown > 0 || chargeTicks > 0 || level().isClientSide) return false;
        chargeDir = dir.normalize();
        chargeTicks = 22;
        chargeHit.clear();
        entityData.set(CHARGING, true);
        if (!isFlying()) { setFlying(true); takeoffTicks = 20; flyingTime = 0; }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0f, dragonType.voicePitch + 0.2f);
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + dragonType.scale, getZ(), 1, 0, 0, 0, 0);
        return true;
    }

    private Vec3 mouthPos() {
        Vec3 fwd = calculateViewVector(0, getYHeadRot());
        return position().add(0, 1.15 * dragonType.scale, 0).add(fwd.scale(2.4 * dragonType.scale));
    }

    private Vec3 breathDir(Vec3 origin) {
        LivingEntity rider = getControllingPassenger();
        if (rider != null) {
            Vec3 eye = rider.getEyePosition();
            Vec3 look = rider.getLookAngle();
            Vec3 end = eye.add(look.scale(60));
            BlockHitResult r = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
            Vec3 aim = r.getType() == HitResult.Type.MISS ? end : r.getLocation();
            Vec3 d = aim.subtract(origin);
            return d.lengthSqr() > 16 ? d.normalize() : look;
        }
        LivingEntity t = getTarget();
        if (t != null) return t.getBoundingBox().getCenter().subtract(origin).normalize();
        return calculateViewVector(getXRot(), getYHeadRot());
    }

    private boolean isAlly(LivingEntity e) {
        if (e == this || hasPassenger(e) || e.isPassengerOfSameVehicle(this)) return true;
        if (isTame() && e instanceof Player) return true;
        LivingEntity o = getOwner();
        return o != null && (e == o || (e instanceof TamableAnimal t && t.isTame() && t.getOwner() == o));
    }

    private void tickBreath() {
        boolean active = breathTicks > 0 && breathCooldown <= 0;
        if (active) {
            breathTicks--;
            breathSustain++;
            doBreath();
            if (breathSustain >= 80) { breathCooldown = 40; breathTicks = 0; breathSustain = 0; active = false; }
        } else {
            if (breathSustain > 0) { breathCooldown = Math.max(breathCooldown, 8 + breathSustain / 4); breathSustain = 0; }
            breathTicks = 0;
        }
        entityData.set(BREATHING, active);
    }

    private void doBreath() {
        ServerLevel sl = (ServerLevel) level();
        Vec3 origin = mouthPos();
        Vec3 dir = breathDir(origin);
        Vec3 end = origin.add(dir.scale(28));
        BlockHitResult hit = sl.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 stop = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        double len = origin.distanceTo(stop);

        for (double d = 1.0; d < len; d += 1.5) {
            Vec3 p = origin.add(dir.scale(d));
            float spread = (float) (0.1 + d * 0.025);
            sl.sendParticles(dragonType.breath, p.x, p.y, p.z, 2, spread, spread, spread, 0.01);
            if (((int) d) % 3 == 0) sl.sendParticles(dragonType.breathAux, p.x, p.y, p.z, 1, spread, spread, spread, 0.02);
        }
        if (breathSustain % 4 == 1)
            sl.playSound(null, origin.x, origin.y, origin.z, dragonType.breathSound, SoundSource.HOSTILE, 2.0f, dragonType.voicePitch + random.nextFloat() * 0.2f);

        if (breathSustain % 5 == 1) {
            AABB box = new AABB(origin, stop).inflate(2.5);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !isAlly(e))) {
                if (segDist(e.getBoundingBox().getCenter(), origin, stop) < 1.6 + e.getBbWidth() * 0.5)
                    breathHit(e, dir, dragonType.breathDamage);
            }
            if (hit.getType() == HitResult.Type.BLOCK) impact(sl, hit.getLocation(), dir);
        }
    }

    private static double segDist(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = Mth.clamp(p.subtract(a).dot(ab) / Math.max(1e-6, ab.lengthSqr()), 0, 1);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    private void breathHit(LivingEntity e, Vec3 dir, float dmg) {
        e.invulnerableTime = 0;
        e.hurt(damageSources().mobAttack(this), dmg);
        switch (dragonType) {
            case FIRE -> e.setRemainingFireTicks(200);
            case ICE -> { e.setTicksFrozen(e.getTicksRequiredToFreeze() + 80); e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2)); }
            case FOREST -> { e.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 2)); e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1)); }
            case OCEAN -> { e.clearFire(); e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0)); e.push(dir.x * 0.9, 0.2, dir.z * 0.9); e.hurtMarked = true; }
            case STORM -> { e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3)); e.push(dir.x * 0.4, 0.1, dir.z * 0.4); e.hurtMarked = true; }
        }
    }

    // ------------------------------------------------------------------ terrain destruction
    private void impact(ServerLevel sl, Vec3 pos, Vec3 dir) {
        BlockPos c = BlockPos.containing(pos.add(dir.scale(0.4)));
        destroySphere(sl, c, dragonType == DragonType.STORM ? 3.4 : 3.0, 30f);
        terrainEffect(sl, c, pos);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c).inflate(3.5), e -> e.isAlive() && !isAlly(e)))
            breathHit(e, dir, dragonType.breathDamage * 0.6f);
        sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0.5, 0.5, 0.5, 0);
        if (random.nextInt(3) == 0)
            sl.playSound(null, c, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.8f + random.nextFloat() * 0.3f);
    }

    private int destroySphere(ServerLevel sl, BlockPos c, double r, float maxHardness) {
        if (!EventHooks.canEntityGrief(sl, this)) return 0;
        int n = 0, ir = (int) Math.ceil(r);
        for (int dx = -ir; dx <= ir; dx++) for (int dy = -ir; dy <= ir; dy++) for (int dz = -ir; dz <= ir; dz++) {
            if (dx * dx + dy * dy + dz * dz > r * r) continue;
            n += tryDestroy(sl, c.offset(dx, dy, dz), maxHardness) ? 1 : 0;
        }
        return n;
    }

    private boolean tryDestroy(ServerLevel sl, BlockPos p, float maxHardness) {
        BlockState s = sl.getBlockState(p);
        if (s.isAir() || s.getBlock() instanceof LiquidBlock) return false;
        float h = s.getDestroySpeed(sl, p);
        if (h < 0 || h > maxHardness) return false;
        return sl.destroyBlock(p, random.nextFloat() < 0.06f, this);
    }

    private void terrainEffect(ServerLevel sl, BlockPos c, Vec3 pos) {
        if (!EventHooks.canEntityGrief(sl, this)) return;
        int r = 3;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            if (p.distSqr(c) > r * r) continue;
            BlockState s = sl.getBlockState(p);
            BlockPos below = p.below();
            boolean floor = s.isAir() && sl.getBlockState(below).isFaceSturdy(sl, below, Direction.UP);
            boolean soil = s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD);
            switch (dragonType) {
                case FIRE -> {
                    if (floor && random.nextFloat() < 0.25f) sl.setBlockAndUpdate(p.immutable(), BaseFireBlock.getState(sl, p));
                    else if (soil && random.nextFloat() < 0.10f) sl.setBlockAndUpdate(p.immutable(), Blocks.MAGMA_BLOCK.defaultBlockState());
                }
                case ICE -> {
                    if (s.is(Blocks.WATER)) sl.setBlockAndUpdate(p.immutable(), Blocks.ICE.defaultBlockState());
                    else if (floor && random.nextFloat() < 0.3f) sl.setBlockAndUpdate(p.immutable(), Blocks.SNOW.defaultBlockState());
                    else if (soil && random.nextFloat() < 0.15f) sl.setBlockAndUpdate(p.immutable(), Blocks.PACKED_ICE.defaultBlockState());
                }
                case FOREST -> { if (soil && random.nextFloat() < 0.2f) sl.setBlockAndUpdate(p.immutable(), Blocks.PODZOL.defaultBlockState()); }
                case OCEAN -> { if (s.getBlock() instanceof BaseFireBlock) sl.setBlockAndUpdate(p.immutable(), Blocks.AIR.defaultBlockState()); }
                case STORM -> { }
            }
        }
        if (dragonType == DragonType.FOREST && random.nextFloat() < 0.3f) {
            AreaEffectCloud cloud = new AreaEffectCloud(sl, pos.x, pos.y, pos.z);
            cloud.setOwner(this);
            cloud.setRadius(3.5f);
            cloud.setDuration(120);
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            sl.addFreshEntity(cloud);
        }
        if (dragonType == DragonType.STORM && random.nextFloat() < 0.35f && pos.distanceToSqr(position()) > 64) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) { bolt.moveTo(Vec3.atBottomCenterOf(c.above())); bolt.setCause(null); sl.addFreshEntity(bolt); }
        }
    }

    // ------------------------------------------------------------------ charge
    private void tickCharge() {
        if (chargeTicks <= 0) { if (isCharging()) entityData.set(CHARGING, false); return; }
        chargeTicks--;
        ServerLevel sl = (ServerLevel) level();
        LivingEntity rider = getControllingPassenger();
        Vec3 dir = (rider != null ? rider.getLookAngle() : chargeDir).normalize();
        Vec3 step = dir.scale(2.6);

        AABB box = getBoundingBox().inflate(0.7).expandTowards(step);
        if (EventHooks.canEntityGrief(sl, this)) {
            BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ), max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
            for (BlockPos p : BlockPos.betweenClosed(min, max)) tryDestroy(sl, p.immutable(), 30f);
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box.inflate(0.8), e -> e.isAlive() && !isAlly(e) && !chargeHit.contains(e.getId()))) {
            chargeHit.add(e.getId());
            e.invulnerableTime = 0;
            e.hurt(damageSources().mobAttack(this), dragonType.chargeDamage);
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * 2.5, 0.6, dir.z * 2.5));
            e.hurtMarked = true;
        }
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + dragonType.scale, getZ(), 6, 1.0, 0.8, 1.0, 0.05);
        sl.sendParticles(dragonType.breath, getX(), getY() + dragonType.scale, getZ(), 8, 1.2, 0.9, 1.2, 0.08);
        if (chargeTicks % 6 == 0) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3.0f, 0.6f);
        if (chargeTicks == 0) { chargeCooldown = 80; entityData.set(CHARGING, false); }
    }

    // ------------------------------------------------------------------ sounds
    @Nullable @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDER_DRAGON_GROWL; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENDER_DRAGON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }
    @Override protected float getSoundVolume() { return 2.5f; }
    @Override public float getVoicePitch() { return dragonType.voicePitch + (random.nextFloat() - 0.5f) * 0.1f; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.IRON_GOLEM_STEP, 1.2f, 0.6f); }
}
