package com.siuzu.paradox.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "paradox");

    public static final RegistryObject<CreativeModeTab> PARADOX_TAB =
            TABS.register("paradox_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.paradox_tab"))
                    .icon(() -> new ItemStack(ModItems.THE_AMULET_OF_THE_TIME.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.THE_SHARD_OF_THE_TIME.get());
                        output.accept(ModItems.THE_AMULET_OF_THE_TIME.get());
                        output.accept(ModItems.ONE_HANDED_SWORD.get());
                    })
                    .build());
}
