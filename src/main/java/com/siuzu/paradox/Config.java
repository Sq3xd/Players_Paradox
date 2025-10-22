package com.siuzu.paradox;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = ParadoxMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        COMMON = new Common(builder);

        COMMON_CONFIG = builder.build();
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue defaultBehavior;
        public final ForgeConfigSpec.IntValue tradeBehavior;
        public final ForgeConfigSpec.IntValue mobKillBehavior;
        public final ForgeConfigSpec.IntValue villagerKillBehavior;
        public final ForgeConfigSpec.IntValue playerKillBehavior;
        public final ForgeConfigSpec.IntValue tameAnimalBehavior;
        public final ForgeConfigSpec.IntValue breedAnimalBehavior;

        public final ForgeConfigSpec.IntValue passiveMinSpawnBehavior;
        public final ForgeConfigSpec.IntValue neutralMinSpawnBehavior;

        public final ForgeConfigSpec.IntValue echoSpawnChance;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("behavior_values");

            defaultBehavior = builder
                    .comment("Default behavior value when player first joins.")
                    .defineInRange("defaultBehavior", 0, -10000, 10000);

            tradeBehavior = builder
                    .comment("Behavior change when trading with a villager.")
                    .defineInRange("tradeBehavior", 10, -10000, 10000);

            mobKillBehavior = builder
                    .comment("Behavior change when killing a mob.")
                    .defineInRange("mobKillBehavior", -1, -10000, 10000);

            villagerKillBehavior = builder
                    .comment("Behavior change when killing a villager.")
                    .defineInRange("villagerKillBehavior", -15, -10000, 10000);

            playerKillBehavior = builder
                    .comment("Behavior change when killing another player.")
                    .defineInRange("playerKillBehavior", -25, -10000, 10000);

            tameAnimalBehavior = builder
                    .comment("Behavior change when taming an animal.")
                    .defineInRange("tameAnimalBehavior", 5, -10000, 10000);

            breedAnimalBehavior = builder
                    .comment("Behavior change when breeding animals.")
                    .defineInRange("breedAnimalBehavior", 2, -10000, 10000);

            passiveMinSpawnBehavior = builder
                    .comment("Minimum behavior required to spawn passive Echo.")
                    .defineInRange("passiveMinSpawnBehavior", 50, -1000000, 1000000);

            neutralMinSpawnBehavior = builder
                    .comment("Minimum behavior required to spawn neutral Echo")
                    .defineInRange("neutralMinSpawnBehavior", -50, -1000000, 1000000);

            echoSpawnChance = builder
                    .comment("Spawn chance of echo after event is triggered.")
                    .defineInRange("echoSpawnChance", 10, 0, 100);

            builder.pop();
        }
    }
}