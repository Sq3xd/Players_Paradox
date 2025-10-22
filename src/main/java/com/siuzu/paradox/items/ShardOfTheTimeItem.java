package com.siuzu.paradox.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class ShardOfTheTimeItem extends Item {
    public ShardOfTheTimeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }
}
