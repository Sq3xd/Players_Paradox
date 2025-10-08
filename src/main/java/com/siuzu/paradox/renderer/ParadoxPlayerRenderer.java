package com.siuzu.paradox.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.siuzu.paradox.entity.ParadoxPlayerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;


public class ParadoxPlayerRenderer extends HumanoidMobRenderer<ParadoxPlayerEntity, PlayerModel<ParadoxPlayerEntity>> {

    private final PlayerModel<ParadoxPlayerEntity> defaultModel;
    private final PlayerModel<ParadoxPlayerEntity> slimModel;



    public ParadoxPlayerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);

        this.defaultModel = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public void render(ParadoxPlayerEntity entity, float yaw, float partialTicks, PoseStack stack,
                       MultiBufferSource buffer, int packedLight) {
        GameProfile profile = entity.getProfile();
        String modelType = "default";

        if (profile != null) {
            UUID uuid = profile.getId();
            modelType = DefaultPlayerSkin.getSkinModelName(uuid);
        }

        this.model = "slim".equals(modelType) ? slimModel : defaultModel;

        // Scale model
        stack.pushPose();
        float scale = 0.94F; // 1.7
        stack.scale(scale, scale, scale);

        super.render(entity, yaw, partialTicks, stack, buffer, packedLight);

        stack.popPose();
    }


    @Override
    public ResourceLocation getTextureLocation(ParadoxPlayerEntity entity) {
        GameProfile profile = entity.getProfile();
        if (profile != null) {
            Minecraft mc = Minecraft.getInstance();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map =
                    mc.getSkinManager().getInsecureSkinInformation(profile);

            if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                return mc.getSkinManager().registerTexture(
                        map.get(MinecraftProfileTexture.Type.SKIN),
                        MinecraftProfileTexture.Type.SKIN
                );
            } else {
                return DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(profile));
            }
        }
        return new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    }
}