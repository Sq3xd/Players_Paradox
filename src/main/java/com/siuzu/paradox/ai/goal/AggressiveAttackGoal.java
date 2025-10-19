package com.siuzu.paradox.ai.goal;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class AggressiveAttackGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;
    private int attackCooldown = 0;

    public AggressiveAttackGoal(PathfinderMob mob, double speed, boolean longMemory) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && !target.isRemoved();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && !target.isRemoved();
    }

    @Override
    public void start() {
        System.out.println("[Echo AttackGoal] Started attacking: " +
                (mob.getTarget() != null ? mob.getTarget().getName().getString() : "null"));
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            System.out.println("[Echo AttackGoal] Lost target!");
            return;
        }

        double distSq = mob.distanceToSqr(target);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (distSq > 3.0D) {
            mob.getNavigation().moveTo(target, speed);
        } else {
            mob.getNavigation().stop();
        }

        if (attackCooldown > 0) attackCooldown--;

        if (distSq <= 3.5D && attackCooldown <= 0) {
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            mob.doHurtTarget(target);
            Vec3 look = mob.getLookAngle();
            mob.setDeltaMovement(mob.getDeltaMovement().add(look.x * 0.2, 0.0, look.z * 0.2));
            attackCooldown = 20;
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        System.out.println("[Echo AttackGoal] Stopped");
    }
}