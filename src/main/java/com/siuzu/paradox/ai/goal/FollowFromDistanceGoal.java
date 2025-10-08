package com.siuzu.paradox.ai.goal;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class FollowFromDistanceGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;
    private final double minDistance;
    private final double maxDistance;
    private Player targetPlayer;
    private final Random random = new Random();

    private float lastYRot = 0f;

    public FollowFromDistanceGoal(PathfinderMob mob, double speed, double minDistance, double maxDistance) {
        this.mob = mob;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        List<Player> players = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(maxDistance));
        if (players.isEmpty()) return false;
        targetPlayer = players.get(0);
        return true;
    }

    @Override
    public void tick() {
        if (targetPlayer == null) return;

        double dist = mob.distanceTo(targetPlayer);

        // Keep a smooth rotation memory so we don’t jitter while airborne
        boolean isGrounded = mob.onGround();

        if (dist < minDistance) {
            // Flee logic
            double dx = mob.getX() - targetPlayer.getX();
            double dz = mob.getZ() - targetPlayer.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.001) return;

            double fleeX = mob.getX() + (dx / length) * 5;
            double fleeZ = mob.getZ() + (dz / length) * 5;

            // Smoothly rotate toward flee direction
            float targetRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
            mob.setYRot(lerpRotation(mob.getYRot(), targetRot, 6f)); // turn speed
            mob.setYBodyRot(mob.getYRot());
            mob.setYHeadRot(mob.getYRot());

            mob.getNavigation().moveTo(fleeX, mob.getY(), fleeZ, speed * 1.3);

            if (isGrounded && random.nextFloat() < 0.15f) {
                mob.getJumpControl().jump();

                // Add realistic forward impulse when jumping
                Vec3 look = mob.getLookAngle();
                double impulse = 0.28D;
                mob.setDeltaMovement(
                        mob.getDeltaMovement().add(look.x * impulse, 0, look.z * impulse)
                );
            }

        } else if (dist > maxDistance) {
            // Follow logic (but keep movement smooth)
            mob.getNavigation().moveTo(targetPlayer, speed);

            // Smooth head turn
            float targetRot = (float) (Mth.atan2(
                    targetPlayer.getZ() - mob.getZ(),
                    targetPlayer.getX() - mob.getX()) * (180F / Math.PI)) - 90F;
            mob.setYRot(lerpRotation(mob.getYRot(), targetRot, 4f));
            mob.setYBodyRot(mob.getYRot());
            mob.setYHeadRot(mob.getYRot());

        } else {
            // Idle and watch
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(targetPlayer);
        }

        lastYRot = mob.getYRot();
    }

    private float lerpRotation(float current, float target, float maxTurn) {
        float delta = Mth.wrapDegrees(target - current);
        if (delta > maxTurn) delta = maxTurn;
        if (delta < -maxTurn) delta = -maxTurn;
        return current + delta;
    }
}