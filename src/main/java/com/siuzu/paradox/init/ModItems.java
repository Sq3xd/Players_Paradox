package com.siuzu.paradox.init;

import com.siuzu.paradox.ParadoxMod;
import com.siuzu.paradox.items.OneHandedSwordItem;
import com.siuzu.paradox.items.ShardOfTheTimeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ParadoxMod.MODID);

    public static RegistryObject<Item> THE_SHARD_OF_THE_TIME = ITEMS.register("the_shard_of_the_time", () ->
            new Item(new Item.Properties()));

    public static RegistryObject<Item> THE_AMULET_OF_THE_TIME = ITEMS.register("the_amulet_of_the_time", () ->
            new Item(new Item.Properties()));

    public static RegistryObject<Item> ONE_HANDED_SWORD = ITEMS.register("one_handed_sword", () ->
            new OneHandedSwordItem(ModTiers.FUTURISTIC, 1, -1.6f, new Item.Properties().fireResistant()));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
