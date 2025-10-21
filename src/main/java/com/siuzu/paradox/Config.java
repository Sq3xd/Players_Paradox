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

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("behavior_values");

            defaultBehavior = builder
                    .comment("Default behavior value when player first joins.")
                    .defineInRange("defaultBehavior", 0, -999999999, 999999999);

            tradeBehavior = builder
                    .comment("Behavior change when trading with a villager.")
                    .defineInRange("tradeBehavior", 10, -999999999, 999999999);

            mobKillBehavior = builder
                    .comment("Behavior change when killing a mob.")
                    .defineInRange("mobKillBehavior", 1, -999999999, 999999999);

            villagerKillBehavior = builder
                    .comment("Behavior change when killing a villager.")
                    .defineInRange("villagerKillBehavior", 15, -999999999, 999999999);

            builder.pop();
        }
    }
}