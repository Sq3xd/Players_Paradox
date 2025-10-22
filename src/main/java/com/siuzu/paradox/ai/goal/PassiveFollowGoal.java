package com.siuzu.paradox.ai.goal;

import com.siuzu.paradox.ParadoxPhrases;
import com.siuzu.paradox.entity.ParadoxPlayerEntity;
import com.siuzu.paradox.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class PassiveFollowGoal extends Goal {
    private final ParadoxPlayerEntity mob;
    private Player targetPlayer;
    private final double approachSpeed;
    private final double maxDistance = 12.0D;
    private final double interactDistance = 3.5D;
    private final Random random = new Random();

    private int timer = 0;
    private int phase = 0; // 0=approach, 1=greet, 2=dialogue, 3=gift, 4=goodbye
    private boolean hasGifted = false;
    private boolean confused = false;
    private int confusedTimer = 0;
    private boolean completed = false;

    private static final List<ItemStack> GIFTS = List.of(
            new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
            new ItemStack(Items.NETHERITE_INGOT),
            new ItemStack(Items.EMERALD, 12),
            new ItemStack(Items.ENDER_PEARL, 8),
            new ItemStack(Items.EXPERIENCE_BOTTLE, 16),
            new ItemStack(Items.DIAMOND, 4),
            new ItemStack(Items.DIAMOND, 8),
            new ItemStack(Items.NETHER_STAR, 1),
            new ItemStack(ModItems.ONE_HANDED_SWORD.get()),
            new ItemStack(ModItems.THE_SHARD_OF_THE_TIME.get())
    );

    public PassiveFollowGoal(ParadoxPlayerEntity mob, double speed) {
        this.mob = mob;
        this.approachSpeed = speed;
    }

    @Override
    public boolean canUse() {
        if (completed) return false;
        if (!mob.getCharacterType().equals("passive")) return false;

        Player nearest = mob.level().getNearestPlayer(mob, 64);
        if (nearest != null && !nearest.isSpectator() && !nearest.isCreative()) {
            this.targetPlayer = nearest;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        timer = 0;
        phase = 0;
        hasGifted = false;
        confused = false;
    }

    @Override
    public void tick() {
        if (completed) return;
        if (targetPlayer == null || !targetPlayer.isAlive()) return;
        if (!(mob.level() instanceof ServerLevel level)) return;

        timer++;
        double dist = mob.distanceTo(targetPlayer);

        mob.getLookControl().setLookAt(targetPlayer, 32.0F, 32.0F);

        if ((phase >= 1 && phase < 4) && dist > maxDistance && !confused) {
            say(level, ParadoxPhrases.randomConfusedPhrase());
            confused = true;
            confusedTimer = 0;
        }
        if (confused) {
            confusedTimer++;
            mob.getNavigation().moveTo(targetPlayer, approachSpeed);
            if (dist <= interactDistance || confusedTimer > 100) {
                confused = false;
            }
            return;
        }

        switch (phase) {
            case 0 -> { // approach
                if (dist > interactDistance) {
                    mob.getNavigation().moveTo(targetPlayer, approachSpeed);
                } else {
                    mob.getNavigation().stop();
                    say(level, ParadoxPhrases.randomGreetingPhrase());
                    phase = 1;
                    timer = 0;
                }
            }
            case 1 -> { // dialogue
                if (timer > 55) {
                    say(level, ParadoxPhrases.randomDialoguePhrase());
                    phase = 2;
                    timer = 0;
                }
            }
            case 2 -> { // gift
                if (!hasGifted && timer > 80) {
                    giveGift(level);
                    say(level, ParadoxPhrases.randomGiftPhrase());
                    hasGifted = true;
                    phase = 3;
                }
            }
            case 3 -> { // goodbye
                if (timer > 120) {
                    say(level, ParadoxPhrases.randomGoodbyePhrase());
                    moveFarAway(level);
                    phase = 4;
                    completed = true;
                    vanishWithEffect(level);
                }
            }
        }
    }

    private void vanishWithEffect(ServerLevel level) {
        for (int i = 0; i < 80; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 1.5;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 1.5;
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    mob.getX() + offsetX,
                    mob.getY() + 1.0 + offsetY,
                    mob.getZ() + offsetZ,
                    1,
                    0, 0, 0, 0.0
            );
        }

        level.playSound(null, mob.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT,
                mob.getSoundSource(),
                1.5F,
                0.6F + random.nextFloat() * 0.4F);

        mob.discard();
    }

    @Override
    public boolean canContinueToUse() {
        return !completed && phase < 4 && targetPlayer != null && targetPlayer.isAlive();
    }

    @Override
    public void stop() {
        targetPlayer = null;
    }

    private void say(ServerLevel level, Component text) {
        double radius = 32.0D;
        level.players().forEach(p -> {
            if (p.distanceTo(mob) <= radius) {
                p.displayClientMessage(
                        Component.literal("<" + mob.getName().getString() + "> ").append(text),
                        false
                );
            }

            level.playSound(null, this.mob.getOnPos(),
                    SoundEvents.VILLAGER_HURT, SoundSource.PLAYERS, 1.0F, 1f);
        });
    }

    private void giveGift(ServerLevel level) {
        ItemStack gift = GIFTS.get(random.nextInt(GIFTS.size()));
        Vec3 from = mob.position().add(0, 1.5, 0);
        Vec3 to = targetPlayer.position().add(0, 1.0, 0);
        Vec3 dir = to.subtract(from).normalize().scale(0.4);

        ItemEntity thrown = new ItemEntity(level, from.x, from.y, from.z, gift);
        thrown.setDeltaMovement(dir.x, dir.y + 0.1, dir.z);
        thrown.setDefaultPickUpDelay();

        level.addFreshEntity(thrown);
    }

    private void moveFarAway(ServerLevel level) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 900 + random.nextDouble() * 600;
        double x = mob.getX() + Math.cos(angle) * distance;
        double z = mob.getZ() + Math.sin(angle) * distance;

        mob.getNavigation().moveTo(10000, mob.getY(), z, 1.25D);
    }
}