package com.siuzu.paradox.ai.goal;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Random;

public class AggressiveAttackGoal extends MeleeAttackGoal {
    private final Mob mob;
    private final Random random = new Random();

    public AggressiveAttackGoal(PathfinderMob mob, double speed, boolean longMemory) {
        super(mob, speed, longMemory);
        this.mob = mob;
    }


    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTargetSqr) {
        double reachSqr = this.getAttackReachSqr(target);

        if (distToTargetSqr <= reachSqr && this.getTicksUntilNextAttack() <= 0) {
            this.resetAttackCooldown();

            // Perform attack
            mob.swing(mob.getUsedItemHand());
            boolean hit = mob.doHurtTarget(target);

            // Critical chance
            if (hit && random.nextFloat() < 0.3F) {
                float extraDamage = 3.0F;
                target.hurt(mob.damageSources().mobAttack(mob), extraDamage);

                // Play crit sound and spawn particles
                mob.level().playSound(null, mob.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_CRIT, mob.getSoundSource(), 1.2F, 1.0F);

                if (mob.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.CRIT,
                            target.getX(),
                            target.getY(0.5),
                            target.getZ(),
                            10,
                            0.3, 0.3, 0.3, 0.1
                    );
                }
            }

            // Normal attack feedback
            mob.level().playSound(null, mob.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_STRONG, mob.getSoundSource(), 1.0F, 1.0F);
        }
    }
}