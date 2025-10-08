package com.siuzu.paradox.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.siuzu.paradox.ai.goal.FollowFromDistanceGoal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ParadoxPlayerEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(ParadoxPlayerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OWNER_UUID =
            SynchedEntityData.defineId(ParadoxPlayerEntity.class, EntityDataSerializers.STRING);

    private int lifetime = 20 * 60; // 1 minute in ticks
    private int hideCooldown = 0;
    private final Random random = new Random();

    @Nullable
    private GameProfile cachedProfile;

    public ParadoxPlayerEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        updateBehavior();
    }


    @Override
    protected void registerGoals() {
        updateBehavior();
    }

    private void updateBehavior() {
        this.goalSelector.getAvailableGoals().clear();
        this.targetSelector.getAvailableGoals().clear();

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 120.0F));

        String type = getCharacterType();

        switch (type) {
            case "aggressive" -> {
                this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
                this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
            }
            case "passive" -> {
                this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 15.0F, 1.0D, 1.2D));
            }
            default -> { // neutral
                this.goalSelector.addGoal(2, new FollowFromDistanceGoal(this, 1.0D, 42.0D, 50.0D));
                this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Decrease lifetime
        lifetime--;

        if (lifetime <= 0 && !isVisibleToAnyPlayer()) {
            this.discard(); // vanish silently
            return;
        }

    }

    private boolean isVisibleToAnyPlayer() {
        for (Player player : this.level().players()) {
            if (player.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    private void tryToHide() {
        Player nearest = this.level().getNearestPlayer(this, 40);
        if (nearest == null) return;

        if (nearest.hasLineOfSight(this)) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double xOffset = Math.cos(angle) * 4;
            double zOffset = Math.sin(angle) * 4;

            var nav = this.getNavigation();
            nav.moveTo(getX() + xOffset, getY(), getZ() + zOffset, 1.1D);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    private static final EntityDataAccessor<String> OWNER_MODEL =
            SynchedEntityData.defineId(ParadoxPlayerEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> CHARACTER_TYPE =
            SynchedEntityData.defineId(ParadoxPlayerEntity.class, EntityDataSerializers.STRING);


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(OWNER_UUID, "");
        this.entityData.define(OWNER_MODEL, "default");
        this.entityData.define(CHARACTER_TYPE, "neutral");
    }


    public void setSummoner(GameProfile profile) {
        this.entityData.set(OWNER_NAME, profile.getName());
        this.entityData.set(OWNER_UUID, profile.getId().toString());
        this.entityData.set(OWNER_MODEL, "default");
        this.cachedProfile = profile;
    }

    public String getModelType() {
        return this.entityData.get(OWNER_MODEL);
    }

    public void setCharacterType(String type) {
        this.entityData.set(CHARACTER_TYPE, type.toLowerCase());
        updateBehavior(); // refresh goals when changed
    }

    public String getCharacterType() {
        return this.entityData.get(CHARACTER_TYPE);
    }


    @Nullable
    public GameProfile getProfile() {
        if (cachedProfile == null) {
            String name = this.entityData.get(OWNER_NAME);
            String uuidStr = this.entityData.get(OWNER_UUID);
            if (!name.isEmpty() && !uuidStr.isEmpty()) {
                try {
                    cachedProfile = new GameProfile(UUID.fromString(uuidStr), name);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return cachedProfile;
    }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CharacterType", this.entityData.get(CHARACTER_TYPE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CharacterType"))
            this.entityData.set(CHARACTER_TYPE, tag.getString("CharacterType"));
    }
}