package com.siuzu.paradox.init;

import com.siuzu.paradox.ParadoxMod;
import com.siuzu.paradox.entity.ParadoxPlayerEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ParadoxMod.MODID);

    public static final RegistryObject<EntityType<ParadoxPlayerEntity>> PARADOX_PLAYER =
            ENTITY_TYPES.register("paradox_player",
                    () -> EntityType.Builder.of(ParadoxPlayerEntity::new, MobCategory.MONSTER  )
                            .sized(0.6F, 1.7F)
                            .build("paradox_player"));
}