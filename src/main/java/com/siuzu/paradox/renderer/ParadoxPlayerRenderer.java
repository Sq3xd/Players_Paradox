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
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;


public class ParadoxPlayerRenderer extends LivingEntityRenderer<ParadoxPlayerEntity, PlayerModel<ParadoxPlayerEntity>> {
    private final PlayerModel<ParadoxPlayerEntity> modelDefault;
    private final PlayerModel<ParadoxPlayerEntity> modelSlim;

    public ParadoxPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), /* slim= */ false), 0.5f);
        this.modelDefault = this.model;
        this.modelSlim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), /* slim= */ true);

        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ParadoxPlayerEntity entity) {
        ResourceLocation skinLoc = entity.getSkinLocation();
        if (skinLoc != null) {
            return skinLoc;
        } else {
            GameProfile profile = entity.getProfile();
            if (profile != null && profile.getId() != null) {
                return DefaultPlayerSkin.getDefaultSkin(profile.getId());
            }
            return DefaultPlayerSkin.getDefaultSkin(profile.getId());
        }
    }

    @Override
    public void render(ParadoxPlayerEntity entity, float yaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack matrixStack,
                       MultiBufferSource buffer, int packedLight) {

        if (entity.getSkinLocation() == null) {
            GameProfile profile = entity.getProfile();
            if (profile != null) {
                Minecraft.getInstance().getSkinManager().registerSkins(profile, (type, location, profileTexture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN) {
                        boolean slim = (profileTexture != null && "slim".equals(profileTexture.getMetadata("model")));
                        entity.setSkin(location, slim);
                    }
                }, true);
            }
        }

        boolean slim = entity.isSlimModel();
        this.model = slim ? this.modelSlim : this.modelDefault;

        model.rightArmPose = getArmPose(entity);
        model.leftArmPose  = HumanoidModel.ArmPose.EMPTY;

        float scale = 0.94F;
        matrixStack.scale(scale, scale, scale);

        super.render(entity, yaw, partialTicks, matrixStack, buffer, packedLight);
    }

    private HumanoidModel.ArmPose getArmPose(ParadoxPlayerEntity entity) {
        if (entity.getMainHandItem().getItem().getDefaultInstance().isDamageableItem()) {
            return HumanoidModel.ArmPose.THROW_SPEAR;
        } else {
            return HumanoidModel.ArmPose.EMPTY;
        }
    }

    @Override
    protected boolean isBodyVisible(ParadoxPlayerEntity entity) {
        return true;
    }

    @Override
    protected boolean isShaking(ParadoxPlayerEntity entity) {
        return false;
    }
}