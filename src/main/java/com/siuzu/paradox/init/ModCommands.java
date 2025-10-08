package com.siuzu.paradox.init;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.siuzu.paradox.entity.ParadoxPlayerEntity;
import com.siuzu.paradox.init.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public class ModCommands {

    private static final List<String> CHARACTER_TYPES = List.of("neutral", "aggressive", "passive");

    private static final SuggestionProvider<CommandSourceStack> CHARACTER_SUGGESTIONS = (ctx, builder) -> {
        for (String type : CHARACTER_TYPES) {
            if (type.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(type);
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("summonparadox")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("character", StringArgumentType.word())
                                .suggests(CHARACTER_SUGGESTIONS)
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    String character = StringArgumentType.getString(ctx, "character").toLowerCase();

                                    if (!CHARACTER_TYPES.contains(character)) {
                                        ctx.getSource().sendFailure(Component.literal("Invalid character type! Use neutral, aggressive, or passive."));
                                        return 0;
                                    }

                                    ServerLevel level = target.serverLevel();
                                    BlockPos pos = target.blockPosition().offset(1, 0, 0);

                                    ParadoxPlayerEntity echo = ModEntities.PARADOX_PLAYER.get().create(level);
                                    if (echo != null) {
                                        echo.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                        echo.setSummoner(target.getGameProfile());
                                        echo.setCustomName(target.getName());
                                        echo.setCustomNameVisible(true);
                                        echo.setCharacterType(character);

                                        level.addFreshEntity(echo);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Spawned a " + character + " Paradox Player for " + target.getName().getString()), true);
                                    }
                                    return 1;
                                })
                        ))
        );
    }
}