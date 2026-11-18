package com.dragonlords.entity;

import com.dragonlords.DragonType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class DragonGoals {
    private DragonGoals() {}

    private static boolean clear(DragonEntity d, Vec3 to) {
        return d.level().clip(new ClipContext(d.getEyePosition(), to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, d))
                .getType() == HitResult.Type.MISS;
    }

    /** Free soaring: circles a centre point at varied altitude, avoids terrain via raycasts, lands after a while. */
    static class FlightGoal extends Goal {
        private final DragonEntity d;
        private Vec3 target, center;
        private int timer, stuck;
        private boolean landing;
        private double angle;

        FlightGoal(DragonEntity d) { this.d = d; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() {
            if (d.isVehicle() || d.isInSittingPose() || d.getTarget() != null) return false;
            if (d.isFlying()) return true;
            return d.getRandom().nextInt(d.isTame() ? 500 : 200) == 0 && (!d.isTame() || d.getOwner() != null);
        }

        @Override public boolean canContinueToUse() {
            return !d.isVehicle() && !d.isInSittingPose() && d.getTarget() == null && d.isFlying();
        }

        @Override public void start() {
            if (!d.isFlying()) d.takeOff();
            center = d.isTame() && d.getOwner() != null ? d.getOwner().position() : d.position();
            landing = false; stuck = 0;
            timer = (d.isTame() ? 200 : 400) + d.getRandom().nextInt(d.isTame() ? 450 : 900);
            angle = d.getRandom().nextDouble() * Math.PI * 2;
            pickTarget(0);
        }

        @Override public void tick() {
            timer--;
            LivingEntity owner = d.getOwner();
            if (d.isTame() && owner != null && center.distanceToSqr(owner.position()) > 256) center = owner.position();

            if (!landing && timer <= 0) { landing = true; target = findLanding(); }
            if (landing) {
                if (d.onGround() || d.groundDistance() < 2.5) { d.land(); return; }
                if (d.horizontalCollision) target = findLanding();
                d.getMoveControl().setWantedPosition(target.x, target.y, target.z, 0.7);
                return;
            }
            if (target == null || d.position().distanceToSqr(target) < 16) {
                angle += 0.7 + d.getRandom().nextDouble() * 0.5;
                pickTarget(0);
            }
            if (d.horizontalCollision || d.verticalCollision) stuck++; else stuck = Math.max(0, stuck - 1);
            if (stuck > 8) { pickTarget(10); stuck = 0; }
            d.getMoveControl().setWantedPosition(target.x, target.y, target.z, 0.9);
        }

        private void pickTarget(int extraUp) {
            var rnd = d.getRandom();
            for (int i = 0; i < 8; i++) {
                double r = 14 + rnd.nextDouble() * 20;
                double a = angle + (rnd.nextDouble() - 0.5) + i * 0.8;
                double x = center.x + Math.cos(a) * r, z = center.z + Math.sin(a) * r;
                if (!d.level().hasChunkAt(BlockPos.containing(x, 0, z))) continue;
                int gy = d.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                double y = Math.min(gy + 9 + rnd.nextDouble() * 16 + extraUp, d.level().getMaxBuildHeight() - 8);
                Vec3 t = new Vec3(x, y, z);
                if (clear(d, t)) { target = t; return; }
            }
            target = d.position().add(0, 10, 0);
        }

        private Vec3 findLanding() {
            var rnd = d.getRandom();
            for (int i = 0; i < 8; i++) {
                double x = d.getX() + (rnd.nextDouble() - 0.5) * 24, z = d.getZ() + (rnd.nextDouble() - 0.5) * 24;
                if (!d.level().hasChunkAt(BlockPos.containing(x, 0, z))) continue;
                int gy = d.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                boolean water = !d.level().getFluidState(new BlockPos((int) Math.floor(x), gy - 1, (int) Math.floor(z))).isEmpty();
                if (water && d.dragonType != DragonType.OCEAN) continue;
                return new Vec3(x, gy + 0.5, z);
            }
            return d.position().add(0, -d.groundDistance() + 0.5, 0);
        }
    }

    /** Tamed dragons: fly to the owner, loiter above them, teleport if left far behind. */
    static class FollowOwnerGoal extends Goal {
        private final DragonEntity d;
        private double angle;

        FollowOwnerGoal(DragonEntity d) { this.d = d; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            LivingEntity o = d.getOwner();
            return o != null && d.isTame() && !o.isSpectator() && !d.isInSittingPose() && !d.isVehicle()
                    && d.getTarget() == null && d.distanceToSqr(o) > 15 * 15;
        }

        @Override public boolean canContinueToUse() {
            LivingEntity o = d.getOwner();
            return o != null && !d.isInSittingPose() && !d.isVehicle() && d.getTarget() == null && d.distanceToSqr(o) > 6 * 6;
        }

        @Override public void start() {
            LivingEntity o = d.getOwner();
            if (o != null && !d.isFlying() && d.distanceToSqr(o) > 22 * 22) d.takeOff();
        }

        @Override public void tick() {
            LivingEntity o = d.getOwner();
            if (o == null) return;
            d.getLookControl().setLookAt(o, 10, 10);
            if (d.distanceToSqr(o) > 70 * 70) {
                d.teleportTo(o.getX() + 3, o.getY() + 3, o.getZ() + 3);
                d.takeOff();
                return;
            }
            if (d.isFlying()) {
                angle += 0.05;
                double horiz = d.distanceToSqr(o.getX(), d.getY(), o.getZ());
                if (horiz < 64 && o.onGround() && !o.isFallFlying()) {
                    if (d.onGround() || d.groundDistance() < 2.5) { d.land(); return; }
                    d.getMoveControl().setWantedPosition(o.getX() + 3, o.getY() + 0.5, o.getZ() + 3, 0.7);
                } else {
                    Vec3 t = o.position().add(Math.cos(angle) * 6, 6 + Math.sin(angle * 2) * 2, Math.sin(angle) * 6);
                    d.getMoveControl().setWantedPosition(t.x, t.y, t.z, 1.0);
                }
            } else {
                if (d.distanceToSqr(o) > 24 * 24) d.takeOff();
                else d.getNavigation().moveTo(o, 1.2);
            }
        }

        @Override public void stop() { d.getNavigation().stop(); }
    }

    /** Orbits the target while breathing; charges from range; bites and breathes when grounded. */
    static class CombatGoal extends Goal {
        private final DragonEntity d;
        private double angle;

        CombatGoal(DragonEntity d) { this.d = d; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            LivingEntity t = d.getTarget();
            return t != null && t.isAlive() && !d.isVehicle() && !d.isInSittingPose();
        }

        @Override public void start() {
            angle = d.getRandom().nextDouble() * Math.PI * 2;
            LivingEntity t = d.getTarget();
            if (!d.isFlying() && t != null && (d.distanceTo(t) > 12 || d.getRandom().nextBoolean())) d.takeOff();
        }

        @Override public void tick() {
            LivingEntity t = d.getTarget();
            if (t == null) return;
            d.getLookControl().setLookAt(t, 40, 40);
            double dist = d.distanceTo(t);
            boolean los = d.hasLineOfSight(t);

            if (d.isFlying()) {
                angle += 0.06;
                Vec3 g = t.position().add(Math.cos(angle) * 12, 7 + Math.sin(angle * 0.5) * 2, Math.sin(angle) * 12);
                d.getMoveControl().setWantedPosition(g.x, g.y, g.z, 1.0);
                if (dist < 24 && los) d.startBreath(4);
                if (dist > 10 && dist < 34 && los && d.getRandom().nextInt(50) == 0)
                    d.startCharge(t.getBoundingBox().getCenter().subtract(d.position().add(0, d.getBbHeight() / 2, 0)));
            } else {
                d.getNavigation().moveTo(t, 1.2);
                if (dist < 22 && los) d.startBreath(4);
                if (dist <= 5.5 && d.meleeCooldown <= 0) { d.doHurtTarget(t); d.swing(InteractionHand.MAIN_HAND); d.meleeCooldown = 20; }
                if (dist > 30 && d.getRandom().nextInt(40) == 0) d.takeOff();
            }
        }

        @Override public void stop() { d.getNavigation().stop(); }
    }
}
