package com.dragonlords.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/** Smooth, momentum-based flight steering. Velocity eases toward the target and yaw trails behind, which produces banked arcs. */
public class DragonMoveControl extends MoveControl {
    private final DragonEntity dragon;

    public DragonMoveControl(DragonEntity d) { super(d); this.dragon = d; }

    @Override
    public void tick() {
        if (dragon.isVehicle() || dragon.isCharging()) return;
        if (!dragon.isFlying()) { super.tick(); return; }
        if (operation != Operation.MOVE_TO) return;

        Vec3 delta = new Vec3(wantedX - dragon.getX(), wantedY - dragon.getY(), wantedZ - dragon.getZ());
        double dist = delta.length();
        if (dist < 1.5) { operation = Operation.WAIT; return; }

        double speed = dragon.getAttributeValue(Attributes.FLYING_SPEED) * speedModifier;
        Vec3 desired = delta.scale(speed / dist);
        desired = new Vec3(desired.x, Mth.clamp(desired.y, -0.55, 0.5), desired.z);
        Vec3 nv = dragon.getDeltaMovement().lerp(desired, 0.08);
        dragon.setDeltaMovement(nv);

        if (nv.horizontalDistanceSqr() > 0.0025) {
            float yaw = (float) (Mth.atan2(nv.z, nv.x) * 57.2957763671875) - 90.0f;
            dragon.setYRot(Mth.approachDegrees(dragon.getYRot(), yaw, 3.0f));
            dragon.yBodyRot = dragon.getYRot();
        }
    }
}
