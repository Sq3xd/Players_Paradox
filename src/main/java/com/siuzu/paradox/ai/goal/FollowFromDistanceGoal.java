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
    private int wanderCooldown = 0; // delay between random moves

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
        boolean isGrounded = mob.onGround();

        if (dist < minDistance) {
            // --- FLEE LOGIC ---
            double dx = mob.getX() - targetPlayer.getX();
            double dz = mob.getZ() - targetPlayer.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.001) return;

            double fleeX = mob.getX() + (dx / length) * 5;
            double fleeZ = mob.getZ() + (dz / length) * 5;

            float targetRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
            mob.setYRot(lerpRotation(mob.getYRot(), targetRot, 6f));
            mob.setYBodyRot(mob.getYRot());
            mob.setYHeadRot(mob.getYRot());

            mob.getNavigation().moveTo(fleeX, mob.getY(), fleeZ, speed * 1.32);

            if (isGrounded && random.nextFloat() < 0.15f) {
                mob.getJumpControl().jump();

                Vec3 look = mob.getLookAngle();
                double impulse = 0.42D;
                mob.setDeltaMovement(mob.getDeltaMovement().add(look.x * impulse, 0, look.z * impulse));
            }

        } else if (dist > maxDistance) {
            // --- FOLLOW LOGIC ---
            mob.getNavigation().moveTo(targetPlayer, speed);

            float targetRot = (float) (Mth.atan2(
                    targetPlayer.getZ() - mob.getZ(),
                    targetPlayer.getX() - mob.getX()) * (180F / Math.PI)) - 90F;
            mob.setYRot(lerpRotation(mob.getYRot(), targetRot, 4f));
            mob.setYBodyRot(mob.getYRot());
            mob.setYHeadRot(mob.getYRot());

        } else {
            // --- IDLE + NATURAL WANDERING ---
            mob.getLookControl().setLookAt(targetPlayer);

            if (--wanderCooldown <= 0) {
                wanderCooldown = 80 + random.nextInt(100); // wander every 4–9 seconds

                // Pick a random nearby offset
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 2.5 + random.nextDouble() * 3.0; // move 2.5–5.5 blocks
                double offsetX = mob.getX() + Math.cos(angle) * radius;
                double offsetZ = mob.getZ() + Math.sin(angle) * radius;

                mob.getNavigation().moveTo(offsetX, mob.getY(), offsetZ, speed * 0.9);
            }
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