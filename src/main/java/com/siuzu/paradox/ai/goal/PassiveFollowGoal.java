package com.siuzu.paradox.ai.goal;

import com.siuzu.paradox.entity.ParadoxPlayerEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class PassiveFollowGoal extends Goal {
    private final ParadoxPlayerEntity mob;
    private Player target;
    private final double speed;
    private final double stopDistance;

    public PassiveFollowGoal(ParadoxPlayerEntity mob, double speed, double stopDistance) {
        this.mob = mob;
        this.speed = speed;
        this.stopDistance = stopDistance;
    }

    @Override
    public boolean canUse() {
        Player nearest = mob.level().getNearestPlayer(mob, 32);
        if (nearest != null && !nearest.isSpectator() && !nearest.isCreative()) {
            target = nearest;
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        double distSq = mob.distanceToSqr(target);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (distSq > stopDistance * stopDistance) {
            mob.getNavigation().moveTo(target, speed);
        } else {
            mob.getNavigation().stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        target = null;
    }
}