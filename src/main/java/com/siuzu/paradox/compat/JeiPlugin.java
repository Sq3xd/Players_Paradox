package com.siuzu.paradox.compat;

import com.siuzu.paradox.ParadoxMod;
import com.siuzu.paradox.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
    Component one_handed_sword = Component.translatable("gui.jei.description.one_handed_sword");

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(ParadoxMod.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(new ItemStack(ModItems.ONE_HANDED_SWORD.get()), VanillaTypes.ITEM_STACK, one_handed_sword);
    }
}