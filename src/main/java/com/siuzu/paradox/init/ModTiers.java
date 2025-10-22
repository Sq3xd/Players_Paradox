package com.siuzu.paradox.init;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

public class ModTiers {
    public static final ForgeTier FUTURISTIC = new ForgeTier(1, 3280, 0f,
            9.5f, 18, BlockTags.NEEDS_DIAMOND_TOOL, () ->
            Ingredient.of(ModItems.THE_SHARD_OF_THE_TIME.get()));
}
