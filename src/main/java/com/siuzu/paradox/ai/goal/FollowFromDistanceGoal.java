package com.siuzu.paradox.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class FollowFromDistanceGoal extends Goal {
    private enum BehaviourState {
        FOLLOW,
        RETREAT,
        OBSERVE
    }

    private final PathfinderMob mob;
    private final double speed;
    private final double minDistance;
    private final double maxDistance;
    private Player targetPlayer;
    private final Random random = new Random();

    private BehaviourState currentState = BehaviourState.OBSERVE;
    private int stateTicks = 0;
    private int decisionCooldown = 0;
    private double circleDirection = 1.0D;
    private double circleRadius = 4.0D;

    public FollowFromDistanceGoal(PathfinderMob mob, double speed, double minDistance, double maxDistance) {
        this.mob = mob;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        List<Player> players = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(maxDistance + 6.0D));
        Player nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (Player candidate : players) {
            if (candidate.isSpectator() || !candidate.isAlive()) continue;
            double dist = mob.distanceToSqr(candidate);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = candidate;
            }
        }

        if (nearest == null) {
            return false;
        }

        targetPlayer = nearest;
        currentState = BehaviourState.OBSERVE;
        stateTicks = 0;
        decisionCooldown = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetPlayer != null && targetPlayer.isAlive() && mob.distanceToSqr(targetPlayer) <= (maxDistance + 32.0D) * (maxDistance + 32.0D);
    }

    @Override
    public void stop() {
        targetPlayer = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPlayer == null) return;

        double dist = mob.distanceTo(targetPlayer);
        decisionCooldown--;
        if (decisionCooldown <= 0) {
            BehaviourState desiredState = selectStateForDistance(dist);
            if (desiredState != currentState) {
                currentState = desiredState;
                stateTicks = 0;
            }
            decisionCooldown = 15 + random.nextInt(20);
        }

        stateTicks++;

        switch (currentState) {
            case RETREAT -> performRetreatBehaviour(dist);
            case FOLLOW -> performFollowBehaviour(dist);
            case OBSERVE -> performObserveBehaviour(dist);
        }

        tryJumpObstacles();
    }

    private float lerpRotation(float current, float target, float maxTurn) {
        float delta = Mth.wrapDegrees(target - current);
        if (delta > maxTurn) delta = maxTurn;
        if (delta < -maxTurn) delta = -maxTurn;
        return current + delta;
    }

    private BehaviourState selectStateForDistance(double distance) {
        double comfortableMin = minDistance * 1.1D;
        double comfortableMax = maxDistance * 0.85D;

        if (distance < comfortableMin) {
            return BehaviourState.RETREAT;
        }

        if (distance > maxDistance) {
            return BehaviourState.FOLLOW;
        }

        if (distance > comfortableMax) {
            return BehaviourState.FOLLOW;
        }

        if (currentState == BehaviourState.OBSERVE && stateTicks < 40) {
            return BehaviourState.OBSERVE;
        }

        return BehaviourState.OBSERVE;
    }

    private void performFollowBehaviour(double distance) {
        Vec3 playerMotion = targetPlayer.getDeltaMovement();
        Vec3 predictedPosition = targetPlayer.position().add(playerMotion.scale(6.0D));

        mob.getNavigation().moveTo(predictedPosition.x(), predictedPosition.y(), predictedPosition.z(), speed * (distance > maxDistance + 4.0D ? 1.35D : 1.0D));
        lookAtPlayer(4.0F);

        if (stateTicks > 40 && distance <= maxDistance * 0.9D) {
            currentState = BehaviourState.OBSERVE;
            stateTicks = 0;
        }
    }

    private void performRetreatBehaviour(double distance) {
        double dx = mob.getX() - targetPlayer.getX();
        double dz = mob.getZ() - targetPlayer.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) {
            length = 0.001D;
        }

        double fleeX = mob.getX() + (dx / length) * 6.0D;
        double fleeZ = mob.getZ() + (dz / length) * 6.0D;

        double strafe = (random.nextDouble() - 0.5D) * 4.0D;
        double perpX = -dz / length;
        double perpZ = dx / length;
        fleeX += perpX * strafe;
        fleeZ += perpZ * strafe;

        float targetRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        mob.setYRot(lerpRotation(mob.getYRot(), targetRot, 6.0F));
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());

        mob.getNavigation().moveTo(fleeX, mob.getY(), fleeZ, speed * 1.28D);

        if (mob.onGround() && random.nextFloat() < 0.25F && !currentState.equals(BehaviourState.OBSERVE)) {
            mob.getJumpControl().jump();
        }

        if (distance > minDistance * 1.4D) {
            currentState = BehaviourState.OBSERVE;
            stateTicks = 0;
        }
    }

    private void performObserveBehaviour(double distance) {
        mob.getNavigation().stop();
        lookAtPlayer(6.0F);

        if (mob.onGround() && random.nextFloat() < 0.02F) {
            mob.getJumpControl().jump();
        }

        if (stateTicks % 40 == 0 && random.nextFloat() < 0.3F) {
            Vec3 slightOffset = targetPlayer.position().add((random.nextDouble() - 0.5D) * 2.5D, 0, (random.nextDouble() - 0.5D) * 2.5D);
            mob.getNavigation().moveTo(slightOffset.x(), slightOffset.y(), slightOffset.z(), speed * 0.85D);
        }

        if (distance > maxDistance) {
            currentState = BehaviourState.FOLLOW;
            stateTicks = 0;
        } else if (distance < minDistance) {
            currentState = BehaviourState.RETREAT;
            stateTicks = 0;
        }
    }

    private void lookAtPlayer(float maxTurn) {
        float targetRot = (float) (Mth.atan2(
                targetPlayer.getZ() - mob.getZ(),
                targetPlayer.getX() - mob.getX()) * (180F / Math.PI)) - 90F;
        mob.setYRot(lerpRotation(mob.getYRot(), targetRot, maxTurn));
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());
        mob.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);
    }

    private Vec3 findNavigablePosition(Vec3 desired) {
        BlockPos pos = BlockPos.containing(desired);
        BlockState state = mob.level().getBlockState(pos);
        if (!state.blocksMotion()) {
            return desired;
        }

        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI / 2) * i;
            double offsetX = Math.cos(angle) * 1.3D;
            double offsetZ = Math.sin(angle) * 1.3D;
            Vec3 alternative = desired.add(offsetX, 0, offsetZ);
            BlockState alternativeState = mob.level().getBlockState(BlockPos.containing(alternative));
            if (!alternativeState.blocksMotion()) {
                return alternative;
            }
        }

        return desired;
    }

    private void tryJumpObstacles() {
        if (!mob.onGround()) return;

        Vec3 look = mob.getLookAngle();
        double checkX = mob.getX() + look.x * 0.8D;
        double checkY = mob.getY();
        double checkZ = mob.getZ() + look.z * 0.8D;
        BlockPos frontPos = BlockPos.containing(checkX, Math.floor(checkY + 0.1D), checkZ);
        BlockState frontState = mob.level().getBlockState(frontPos);

        boolean obstacleAhead = frontState.blocksMotion();

        if (!obstacleAhead) {
            BlockPos aboveFront = frontPos.above();
            obstacleAhead = mob.level().getBlockState(aboveFront).blocksMotion();
        }

        if (obstacleAhead || mob.horizontalCollision) {
            mob.getJumpControl().jump();
        }
    }
}
