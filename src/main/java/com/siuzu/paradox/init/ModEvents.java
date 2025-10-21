package com.siuzu.paradox.init;

import com.siuzu.paradox.Config;
import com.siuzu.paradox.ParadoxMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ParadoxMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event) {
        RandomSource random = RandomSource.create();
        Player player = event.player;

        if (event.player.getUUID().toString().equals("bd594a1e-8708-4c78-bfcd-869638df63bf")) {
            if (random.nextInt(1, 5) == 1 && player.level().isClientSide && player.isSprinting()) {
                player.level().addParticle(ParticleTypes.WITCH, player.getX(), player.getY()+0.15f, player.getZ(), 1f, 1f, 1f);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // Check if the player already has a "behavior" tag
        if (!player.getPersistentData().contains("behavior")) {
            player.getPersistentData().putInt("behavior", Config.COMMON.defaultBehavior.get());
        }
    }

    @SubscribeEvent
    public void tradeEvent(TradeWithVillagerEvent event) {
        Player player = event.getEntity();

        if (player != null && !player.level().isClientSide) {
            player.getPersistentData().putInt(
                    "behavior",
                    player.getPersistentData().getInt("behavior") + Config.COMMON.tradeBehavior.get()
            );
        }
    }

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        if (event.getSource().getEntity() instanceof Player player) {
            if (!player.level().isClientSide) {
                int behavior = player.getPersistentData().getInt("behavior");
                player.getPersistentData().putInt("behavior", behavior - Config.COMMON.mobKillBehavior.get());
                if (event.getEntity() instanceof Villager) {
                    player.getPersistentData().putInt("behavior", behavior - Config.COMMON.villagerKillBehavior.get());
                }
            }
        }
    }
}
