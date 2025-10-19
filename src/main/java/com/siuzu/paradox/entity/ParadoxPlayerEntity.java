package com.siuzu.paradox.entity;

import com.mojang.authlib.GameProfile;
import com.siuzu.paradox.ai.goal.AggressiveAttackGoal;
import com.siuzu.paradox.ai.goal.FollowFromDistanceGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;

public class ParadoxPlayerEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(ParadoxPlayerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OWNER_UUID =
            SynchedEntityData.defineId(ParadoxPlayerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CHARACTER_TYPE =
            SynchedEntityData.defineId(ParadoxPlayerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OWNER_MODEL =
            SynchedEntityData.defineId(ParadoxPlayerEntity.class, EntityDataSerializers.STRING);

    private MeleeAttackGoal aggressiveAttackGoal;
    private NearestAttackableTargetGoal<Player> aggressiveTargetGoal;

    private AvoidEntityGoal<Player> passiveAvoidGoal;

    private FollowFromDistanceGoal neutralFollowGoal;

    private final Random random = new Random();
    private boolean transformed = false;
    private int transformationTimer = -1;

    private boolean isEating = false;
    private int eatingTimer = 0;
    private boolean isRetreating = false;
    private int retreatTimer = 0;
    private ItemStack previousMainhand = ItemStack.EMPTY;

    private int lifetime = 20 * 60; // 1 minute
    @Nullable private GameProfile cachedProfile;

    public ParadoxPlayerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.255D)
                .add(Attributes.ATTACK_DAMAGE, 2.5D)
                .add(Attributes.ATTACK_SPEED, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        updateBehavior();
    }

    private void updateBehavior() {
        // Remove all previously registered custom goals
        if (aggressiveAttackGoal != null) this.goalSelector.removeGoal(aggressiveAttackGoal);
        if (aggressiveTargetGoal != null) this.targetSelector.removeGoal(aggressiveTargetGoal);
        if (passiveAvoidGoal != null) this.goalSelector.removeGoal(passiveAvoidGoal);
        if (neutralFollowGoal != null) this.goalSelector.removeGoal(neutralFollowGoal);

        // Reset aggression and target
        this.setTarget(null);
        this.setAggressive(false);

        String type = getCharacterType();

        switch (type) {
            case "aggressive" -> {
                aggressiveAttackGoal = new MeleeAttackGoal(this, 1.25D, true);
                aggressiveTargetGoal = new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                        p -> !p.isSpectator() && !p.isInvulnerable());

                this.goalSelector.addGoal(1, aggressiveAttackGoal);
                this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
                this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
                this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

                this.targetSelector.addGoal(1, aggressiveTargetGoal);
                this.targetSelector.addGoal(2, new HurtByTargetGoal(this));

                System.out.println("[Echo AI] Aggressive goals loaded.");
            }
            case "passive" -> {
                passiveAvoidGoal = new AvoidEntityGoal<>(this, Player.class, 15.0F, 1.0D, 1.2D);

                this.goalSelector.addGoal(1, passiveAvoidGoal);
                this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
                this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

                System.out.println("[Echo AI] Passive goals loaded.");
            }
            default -> {
                neutralFollowGoal = new FollowFromDistanceGoal(this, 1.0D, 33.0D, 50.0D);

                this.goalSelector.addGoal(1, neutralFollowGoal);
                this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
                this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

                System.out.println("[Echo AI] Neutral goals loaded.");
            }
        }
    }

    // === Character Type ===
    public String getCharacterType() {
        return this.entityData.get(CHARACTER_TYPE);
    }

    public void setCharacterType(String type) {
        type = type.toLowerCase();
        String old = this.entityData.get(CHARACTER_TYPE);
        if (old.equals(type)) return; // no spam reset

        this.entityData.set(CHARACTER_TYPE, type);
        updateBehavior();
        System.out.println("[Echo Type] Changed from " + old + " → " + type);
    }

    // === Main Tick ===
    @Override
    public void tick() {
        super.tick();

        if (getCharacterType().equals("aggressive") && this.tickCount % 100 == 0 && this.getTarget() != null) {
            this.forceAIWake();
        }

        if (this.level().isClientSide) return;

        // lifetime
        if (--lifetime <= 0 && !isVisibleToAnyPlayer()) {
            this.discard();
            return;
        }

        Player nearest = this.level().getNearestPlayer(this, 32);
        if (nearest == null) return;

        double dist = this.distanceTo(nearest);

        // === Retreat & Eat Logic ===
        if (getCharacterType().equals("aggressive")) {
            if (!isEating && !isRetreating && this.getHealth() <= this.getMaxHealth() * 0.3F) {
                startRetreat(nearest);
            }
            if (isRetreating) handleRetreat(nearest, dist);
        }

        if (isEating) {
            handleEating();
            return;
        }

        // === Transformation Logic ===
        if (!transformed && this.getHealth() <= this.getMaxHealth() - 15) {
            startTransformation();
        }
        if (transformationTimer >= 0) {
            handleTransformation();
        }

        // Debug every 40 ticks
        if (this.tickCount % 40 == 0) {
            System.out.println("[Echo Debug] Type=" + getCharacterType() +
                    " | Goals=" + this.goalSelector.getAvailableGoals().size() +
                    " | Target=" + (this.getTarget() != null ? this.getTarget().getName().getString() : "none"));
        }

        if (getCharacterType().equals("aggressive") && this.getTarget() != null && this.tickCount % 20 == 0) {
            this.goalSelector.tick();
        }
    }

    // === Retreat ===
    private void startRetreat(Player nearest) {
        isRetreating = true;
        retreatTimer = 0;
        this.setTarget(null);
        this.setAggressive(false);
        this.getNavigation().stop();
        System.out.println("[Echo] Starting retreat");
    }

    private void handleRetreat(Player nearest, double dist) {
        retreatTimer++;
        double dx = this.getX() - nearest.getX();
        double dz = this.getZ() - nearest.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 0.1) {
            double fleeX = this.getX() + (dx / len) * 6;
            double fleeZ = this.getZ() + (dz / len) * 6;
            this.getNavigation().moveTo(fleeX, this.getY(), fleeZ, 1.4D);
        }

        if (dist > 10.0F || retreatTimer > 60) {
            isRetreating = false;
            startEating();
        }
    }

    // === Eating ===
    private void startEating() {
        isEating = true;
        eatingTimer = 0;
        this.setTarget(null);
        this.setAggressive(false);
        this.getNavigation().stop();

        previousMainhand = this.getMainHandItem().copy();
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_APPLE));
        System.out.println("[Echo] Started eating");
    }

    private void handleEating() {
        eatingTimer++;
        if (eatingTimer % 4 == 0) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT,
                    this.getSoundSource(), 0.8F, 0.9F + random.nextFloat() * 0.1F);
            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(
                        new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLDEN_APPLE)),
                        this.getX(), this.getEyeY(), this.getZ(),
                        6, random.nextGaussian() * 0.1, 0.1, random.nextGaussian() * 0.1, 0.05);
            }
        }
        if (eatingTimer >= 50) finishEating();
    }

    private void finishEating() {
        this.isEating = false;
        this.eatingTimer = 0;
        this.heal(20.0F);

        if (!previousMainhand.isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, previousMainhand);
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        }
        previousMainhand = ItemStack.EMPTY;

        setCharacterType("aggressive");
        reacquireTarget();
    }

    // === Transformation ===
    private void startTransformation() {
        transformed = true;
        transformationTimer = 0;
        this.getNavigation().stop();

        // Clear gear
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR || slot == EquipmentSlot.MAINHAND)
                this.setItemSlot(slot, ItemStack.EMPTY);
        }

        this.level().playSound(null, this.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, this.getSoundSource(), 1.0F, 0.8F + random.nextFloat() * 0.4F);
        System.out.println("[Echo] Starting transformation");
    }

    private void handleTransformation() {
        transformationTimer++;
        if (transformationTimer % 10 == 0 && transformationTimer <= 50)
            equipNextRandomPiece();

        if (transformationTimer > 50) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.2F, 1.0F);

            setCharacterType("aggressive"); // calls updateBehavior()

            // Immediately acquire a target
            LivingEntity target = this.level().getNearestPlayer(this, 32);
            if (target instanceof Player p && !p.isCreative() && !p.isSpectator()) {
                this.setTarget(p);
                this.setAggressive(true);
                this.getNavigation().moveTo(p, 1.25D);
                System.out.println("[Echo] Target reacquired and aggression active on: " + p.getName().getString());
            } else {
                System.out.println("[Echo] No valid player found for aggression.");
            }

            transformationTimer = -1;
        }
    }

    private void equipNextRandomPiece() {
        ItemStack[] pool = {
                new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_BOOTS),
                new ItemStack(Items.CHAINMAIL_HELMET), new ItemStack(Items.CHAINMAIL_CHESTPLATE),
                new ItemStack(Items.CHAINMAIL_LEGGINGS), new ItemStack(Items.CHAINMAIL_BOOTS),
                new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.GOLDEN_CHESTPLATE),
                new ItemStack(Items.GOLDEN_LEGGINGS), new ItemStack(Items.GOLDEN_BOOTS),
                new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS),
                new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE),
                new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS)
        };
        ItemStack piece = pool[random.nextInt(pool.length)];
        if (!(piece.getItem() instanceof ArmorItem armor)) return;

        EquipmentSlot slot = armor.getEquipmentSlot();
        if (this.getItemBySlot(slot).isEmpty()) {
            this.setItemSlot(slot, piece);
            if (this.level() instanceof ServerLevel server)
                server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getEyeY(), this.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private void reacquireTarget() {
        // Prefer the last attacker if available
        LivingEntity revengeTarget = this.getLastHurtByMob();
        Player targetPlayer = null;
        if (revengeTarget instanceof Player p && !p.isSpectator() && !p.isCreative() && p.isAlive()) {
            targetPlayer = p;
        } else {
            // Fallback to nearest player within 32 blocks
            targetPlayer = this.level().getNearestPlayer(this, 32);
            if (targetPlayer != null && (targetPlayer.isSpectator() || targetPlayer.isCreative())) {
                targetPlayer = null; // don't target spectators/creative
            }
        }
        // Set the target and aggression
        this.setTarget(targetPlayer);
        this.setAggressive(targetPlayer != null);
        if (targetPlayer != null) {
            // Force immediate pathfinding toward target
            this.getNavigation().moveTo(targetPlayer, 1.25D);
        }
    }

    private void forceAIWake() {
        this.goalSelector.tick();           // tick goal selector immediately
        this.targetSelector.tick();         // ensure target goal is also evaluated
        if (this.getTarget() != null) {
            this.getNavigation().moveTo(this.getTarget(), 1.25D);
            this.setAggressive(true);
            System.out.println("[Echo] Forced AI wake-up on target: " + this.getTarget().getName().getString());
        } else {
            Player nearest = this.level().getNearestPlayer(this, 32);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                this.setTarget(nearest);
                this.setAggressive(true);
                this.getNavigation().moveTo(nearest, 1.25D);
                System.out.println("[Echo] Found new target after wake-up: " + nearest.getName().getString());
            }
        }
    }

    private boolean isVisibleToAnyPlayer() {
        for (Player p : this.level().players()) {
            if (p.hasLineOfSight(this)) return true;
        }
        return false;
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

        this.updateBehavior();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(OWNER_UUID, "");
        this.entityData.define(OWNER_MODEL, "default");
        this.entityData.define(CHARACTER_TYPE, "neutral");
    }

    public String getModelType() {
        return this.entityData.get(OWNER_MODEL);
    }

    public void setSummoner(GameProfile profile) {
        this.entityData.set(OWNER_NAME, profile.getName());
        this.entityData.set(OWNER_UUID, profile.getId().toString());
        this.entityData.set(OWNER_MODEL, "default");
        this.cachedProfile = profile;
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
}