package com.siuzu.paradox.init;

import com.siuzu.paradox.ParadoxMod;
import com.siuzu.paradox.items.ShardOfTheTimeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ParadoxMod.MODID);

    public static RegistryObject<Item> SHARD_OF_THE_TIME = ITEMS.register("shard_of_the_time", () ->
            new ShardOfTheTimeItem(new Item.Properties().rarity(Rarity.RARE)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
