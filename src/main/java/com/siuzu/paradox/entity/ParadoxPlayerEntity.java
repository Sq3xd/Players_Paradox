package com.siuzu.paradox.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.siuzu.paradox.ai.goal.AggressiveAttackGoal;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private boolean transformed = false;
    private int transformationTimer = 0;
    private boolean isEating = false;
    private int eatingTimer = 0;
    private boolean isRetreating = false;
    private int retreatTimer = 0;

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


        String type = getCharacterType();

        switch (type) {
            case "aggressive" -> {
                this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
                this.goalSelector.addGoal(2, new AggressiveAttackGoal(this, 1.25D, true));
            }
            case "passive" -> {
                this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 15.0F, 1.0D, 1.2D));
            }
            default -> { // neutral
                this.goalSelector.addGoal(2, new FollowFromDistanceGoal(this, 1.0D, 33.0D, 50.0D));
                this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // === Despawn logic ===
        lifetime--;
        if (lifetime <= 0 && !isVisibleToAnyPlayer()) {
            this.discard();
            return;
        }

        if (this.level().isClientSide) return;

        Player nearest = this.level().getNearestPlayer(this, 32);
        if (nearest == null) return;

        double dist = this.distanceTo(nearest);

        // === RETREAT & EATING LOGIC (for aggressive only) ===
        if (getCharacterType().equals("aggressive")) {
            if (!isEating && !isRetreating && this.getHealth() <= this.getMaxHealth() * 0.3F) {
                // Start retreating if low HP
                isRetreating = true;
                retreatTimer = 0;
                this.getNavigation().stop();
            }

            if (isRetreating) {
                retreatTimer++;

                // Move away from player
                double dx = this.getX() - nearest.getX();
                double dz = this.getZ() - nearest.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) {
                    double fleeX = this.getX() + (dx / len) * 6;
                    double fleeZ = this.getZ() + (dz / len) * 6;
                    this.getNavigation().moveTo(fleeX, this.getY(), fleeZ, 1.45D);
                }

                // When far enough, stop & eat
                if (dist > 10.0F || retreatTimer > 60) {
                    isRetreating = false;
                    startEating();
                    return;
                }
            }
        }

        // === EATING LOGIC ===
        if (isEating) {
            eatingTimer++;

            // Play eating sound + crumbs
            if (eatingTimer % 4 == 0) {
                float pitch = 0.9F + random.nextFloat() * 0.2F;
                this.level().playSound(null, this.blockPosition(),
                        SoundEvents.GENERIC_EAT, this.getSoundSource(), 0.8F, pitch);

                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new net.minecraft.core.particles.ItemParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.ITEM,
                                    new ItemStack(Items.COOKED_BEEF)
                            ),
                            this.getX(),
                            this.getEyeY(),
                            this.getZ(),
                            6,
                            random.nextGaussian() * 0.1,
                            0.1,
                            random.nextGaussian() * 0.1,
                            0.05
                    );
                }
            }

            // Done eating (~2.5 seconds)
            if (eatingTimer >= 50) {
                this.heal(5.0F);
                finishEating();
                setCharacterType("aggressive"); // resume fighting
                updateBehavior();
            }

            return; // skip other logic while eating
        }

        // === TRANSFORMATION SEQUENCE ===
        // Now protected by persistent flag so it only happens once
        if (!transformed && this.getHealth() <= this.getMaxHealth() / 2) {
            transformed = true;
            transformationTimer = 0;
            this.getNavigation().stop();
        }

        if (transformed && transformationTimer >= 0 && transformationTimer < 90) { // capped for safety
            transformationTimer++;

            Player lookTarget = this.level().getNearestPlayer(this, 32);
            if (lookTarget != null)
                this.getLookControl().setLookAt(lookTarget);

            if (transformationTimer < 40) return;

            if (transformationTimer % 10 == 0) {
                int step = (transformationTimer - 40) / 10;

                switch (step) {
                    case 0 -> {
                        this.setItemSlot(EquipmentSlot.MAINHAND, Items.TURTLE_HELMET.getDefaultInstance());
                        equipArmorPiece(EquipmentSlot.HEAD, Items.TURTLE_HELMET.getDefaultInstance());
                    }
                    case 1 -> {
                        this.setItemSlot(EquipmentSlot.MAINHAND, Items.NETHERITE_CHESTPLATE.getDefaultInstance());
                        equipArmorPiece(EquipmentSlot.CHEST, Items.NETHERITE_CHESTPLATE.getDefaultInstance());
                    }
                    case 2 -> {
                        this.setItemSlot(EquipmentSlot.MAINHAND, Items.NETHERITE_LEGGINGS.getDefaultInstance());
                        equipArmorPiece(EquipmentSlot.LEGS, Items.NETHERITE_LEGGINGS.getDefaultInstance());
                    }
                    case 3 -> {
                        this.setItemSlot(EquipmentSlot.MAINHAND, Items.DIAMOND_BOOTS.getDefaultInstance());
                        equipArmorPiece(EquipmentSlot.FEET, Items.DIAMOND_BOOTS.getDefaultInstance());
                    }
                    case 4 -> {
                        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                        this.level().playSound(null, this.blockPosition(),
                                SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.2F, 1.0F);
                        setCharacterType("aggressive");
                        updateBehavior();
                        transformationTimer = -1; // done
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean result = super.doHurtTarget(target);

        if (result && target instanceof Player player) {
            // 30% chance to do a critical hit
            if (random.nextFloat() < 0.3F) {
                player.hurt(this.damageSources().mobAttack(this), 5.0F); // +3 HP bonus damage

                // Play crit sound & spawn particles
                this.level().playSound(null, this.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.2F, 1.0F);

                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.CRIT,
                            player.getX(),
                            player.getY(0.5),
                            player.getZ(),
                            10,
                            0.3, 0.3, 0.3, 0.1
                    );
                }
            }
        }

        return result;
    }

    private void startEating() {
        this.isEating = true;
        this.eatingTimer = 0;
        this.getNavigation().stop();
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COOKED_BEEF));

        this.level().playSound(null, this.blockPosition(),
                SoundEvents.PLAYER_BURP, this.getSoundSource(), 0.6F, 1.2F);
    }

    private void finishEating() {
        this.isEating = false;
        this.eatingTimer = 0;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
    }

    private void equipArmorPiece(EquipmentSlot slot, ItemStack item) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE, this.getSoundSource(), 1.0F, 1.0F);
        this.setItemSlot(slot, item);
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
        tag.putString("OwnerName", this.entityData.get(OWNER_NAME));
        tag.putString("OwnerUUID", this.entityData.get(OWNER_UUID));
        tag.putString("OwnerModel", this.entityData.get(OWNER_MODEL));
        tag.putBoolean("Transformed", this.transformed);
        tag.putInt("TransformationTimer", this.transformationTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("CharacterType"))
            this.entityData.set(CHARACTER_TYPE, tag.getString("CharacterType"));

        if (tag.contains("OwnerName"))
            this.entityData.set(OWNER_NAME, tag.getString("OwnerName"));

        if (tag.contains("OwnerUUID"))
            this.entityData.set(OWNER_UUID, tag.getString("OwnerUUID"));

        if (tag.contains("OwnerModel"))
            this.entityData.set(OWNER_MODEL, tag.getString("OwnerModel"));

        if (tag.contains("Transformed"))
            this.transformed = tag.getBoolean("Transformed");

        if (tag.contains("TransformationTimer"))
            this.transformationTimer = tag.getInt("TransformationTimer");

        // rebuild cached profile so skin reloads correctly
        String name = this.entityData.get(OWNER_NAME);
        String uuidStr = this.entityData.get(OWNER_UUID);
        if (!name.isEmpty() && !uuidStr.isEmpty()) {
            try {
                this.cachedProfile = new GameProfile(UUID.fromString(uuidStr), name);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}