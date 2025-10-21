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
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;


public class ParadoxPlayerRenderer extends HumanoidMobRenderer<ParadoxPlayerEntity, PlayerModel<ParadoxPlayerEntity>> {

    public ParadoxPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false), 0.5F);

        // Armor layers
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));

        // Item in hand layer
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ParadoxPlayerEntity entity) {
        return entity.getSkinLocation() != null
                ? entity.getSkinLocation()
                : DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
    }

    @Override
    protected void setupRotations(ParadoxPlayerEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
    }

    @Override
    public void render(ParadoxPlayerEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack,
                       MultiBufferSource buffer, int packedLight) {

        PlayerModel<ParadoxPlayerEntity> model = this.getModel();

        // Setup pose
        model.attackTime = entity.getAttackAnim(partialTicks);
        model.riding = entity.isPassenger();
        model.young = entity.isBaby();
        model.crouching = entity.isCrouching();

        model.rightArmPose = getArmPose(entity);
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;

        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    private HumanoidModel.ArmPose getArmPose(ParadoxPlayerEntity entity) {
        if (entity.swinging) {
            return HumanoidModel.ArmPose.THROW_SPEAR;
        } else if (entity.isUsingItem()) {
            return HumanoidModel.ArmPose.ITEM;
        } else if (!entity.getMainHandItem().isEmpty()) {
            return HumanoidModel.ArmPose.ITEM;
        } else {
            return HumanoidModel.ArmPose.EMPTY;
        }
    }
}