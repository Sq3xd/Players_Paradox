package com.siuzu.paradox;

import com.mojang.logging.LogUtils;
import com.siuzu.paradox.entity.ParadoxPlayerEntity;
import com.siuzu.paradox.init.ModCommands;
import com.siuzu.paradox.init.ModEntities;
import com.siuzu.paradox.renderer.ParadoxPlayerRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(ParadoxMod.MODID)
public class ParadoxMod
{
    public static final String MODID = "paradox";
    private static final Logger LOGGER = LogUtils.getLogger();


    public ParadoxMod(FMLJavaModLoadingContext context)
    {
        IEventBus eventBus = context.getModEventBus();

        eventBus.addListener(this::commonSetup);


        MinecraftForge.EVENT_BUS.register(this);

        ModEntities.ENTITY_TYPES.register(eventBus);


        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = ParadoxMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.PARADOX_PLAYER.get(),
                    ParadoxPlayerRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = ParadoxMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public class ModEventBusEvents {
        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.PARADOX_PLAYER.get(), ParadoxPlayerEntity.createAttributes().build());
        }
    }


    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}
