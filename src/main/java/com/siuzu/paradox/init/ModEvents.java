package com.siuzu.paradox.init;

import com.siuzu.paradox.Config;
import com.siuzu.paradox.ParadoxMod;
import com.siuzu.paradox.entity.ParadoxPlayerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.checkerframework.checker.units.qual.C;

import java.util.Random;

@Mod.EventBusSubscriber(modid = ParadoxMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {
    private static void adjustBehavior(Player player, int delta) {
        if (player == null || player.level().isClientSide) return;
        int behavior = player.getPersistentData().getInt("behavior");
        player.getPersistentData().putInt("behavior", behavior + delta);
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event) {
        RandomSource random = RandomSource.create();
        Player player = event.player;

        if (event.player.getUUID().toString().equals("bd594a1e-8708-4c78-bfcd-869638df63bf")) {
            if (random.nextInt(1, 3) == 1 && player.level().isClientSide) {
                player.level().addParticle(ParticleTypes.WITCH, player.getX(), player.getY()+0.15f, player.getZ(), 1f, 1f, 1f);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CompoundTag oldData = event.getOriginal().getPersistentData();
            CompoundTag newData = event.getEntity().getPersistentData();

            if (oldData.contains("behavior")) {
                newData.putInt("behavior", oldData.getInt("behavior"));
            }
        }
    }


    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        if (!player.getPersistentData().contains("behavior")) {
            adjustBehavior(player, Config.COMMON.defaultBehavior.get());
        }
    }

    @SubscribeEvent
    public void tradeEvent(TradeWithVillagerEvent event) {
        Player player = event.getEntity();

        if (player != null && !player.level().isClientSide) {
            adjustBehavior(player, Config.COMMON.tradeBehavior.get());
        }
    }

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        if (event.getSource().getEntity() instanceof Player player) {
            if (!player.level().isClientSide) {
                int behavior = player.getPersistentData().getInt("behavior");
                adjustBehavior(player, Config.COMMON.mobKillBehavior.get());
                if (event.getEntity() instanceof Villager) {
                    adjustBehavior(player, Config.COMMON.villagerKillBehavior.get() + Config.COMMON.mobKillBehavior.get());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player victim)  && !(event.getEntity() instanceof ParadoxPlayerEntity entity)) return;
        if (event.getSource().getEntity() instanceof Player killer) {
            adjustBehavior(killer, Config.COMMON.playerKillBehavior.get());
        }
    }

    @SubscribeEvent
    public static void onAnimalTame(AnimalTameEvent event) {
        Player player = event.getTamer();
        if (event.getTamer() instanceof Player) {
            adjustBehavior(player, Config.COMMON.tameAnimalBehavior.get());
        }
    }

    @SubscribeEvent
    public static void onAnimalBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() != null) {
            Player player = event.getCausedByPlayer();
            adjustBehavior(player, Config.COMMON.breedAnimalBehavior.get());
        }
    }

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        if (level.isClientSide) return;


        // 99% spawn chance
        if (RANDOM.nextFloat() > Config.COMMON.echoSpawnChance.get() / 100f) return;

        int behavior = player.getPersistentData().getInt("behavior");

        // Determine echo character type
        String character;
        if (behavior >= Config.COMMON.neutralMinSpawnBehavior.get() && behavior < Config.COMMON.passiveMinSpawnBehavior.get()) {
            character = "neutral";
        } else if (behavior >= Config.COMMON.passiveMinSpawnBehavior.get()) {
            character = "passive";
        } else {
            character = "aggressive";
        }

        BlockPos spawnPos = findHiddenSpawnPos(level, player.blockPosition(), 30, (ServerPlayer) player);

        if (spawnPos == null) return;

        ParadoxPlayerEntity echo = ModEntities.PARADOX_PLAYER.get().create(level);
        if (echo != null) {
            echo.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            echo.setSummoner(player.getGameProfile());
            echo.setCustomName(player.getName());
            echo.setCustomNameVisible(true);
            echo.setCharacterType(character);

            level.addFreshEntity(echo);
        }
    }

    private static BlockPos findHiddenSpawnPos(ServerLevel level, BlockPos center, int radius, ServerPlayer player) {
        RandomSource random = level.getRandom();

        for (int i = 0; i < 50; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 8 + random.nextDouble() * (radius - 8);
            int x = center.getX() + (int) (Math.cos(angle) * distance);
            int z = center.getZ() + (int) (Math.sin(angle) * distance);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos pos = new BlockPos(x, y, z);

            if (!level.getBlockState(pos.below()).isSolid()) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;

            double dist = Math.sqrt(center.distSqr(pos));
            if (dist < 5 || dist > radius) continue;

            if (!player.hasLineOfSight(
                    new net.minecraft.world.entity.decoration.ArmorStand(level, x + 0.5, y, z + 0.5)
            )) {
                return pos;
            }
        }

        for (int i = 0; i < 50; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 8 + random.nextDouble() * (radius - 8);
            int x = center.getX() + (int) (Math.cos(angle) * distance);
            int z = center.getZ() + (int) (Math.sin(angle) * distance);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);

            if (level.getBlockState(pos.below()).isSolid() &&
                    level.getBlockState(pos).isAir() &&
                    level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }

        return null;
    }
}
