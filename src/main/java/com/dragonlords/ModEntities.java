package com.dragonlords;

import com.dragonlords.entity.DragonEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DragonLords.MOD_ID);

    public static final Map<DragonType, DeferredHolder<EntityType<?>, EntityType<DragonEntity>>> TYPES =
            new EnumMap<>(DragonType.class);

    static {
        for (DragonType t : DragonType.values()) {
            TYPES.put(t, ENTITIES.register(t.entityName(), () -> {
                EntityType.Builder<DragonEntity> b = EntityType.Builder.<DragonEntity>of(DragonEntity::new, MobCategory.CREATURE)
                        .sized(t.scale * 1.2f, t.scale * 1.4f)
                        .clientTrackingRange(10)
                        .updateInterval(2);
                if (t == DragonType.FIRE) b.fireImmune();
                return b.build(t.entityName());
            }));
        }
    }

    public static EntityType<DragonEntity> get(DragonType t) { return TYPES.get(t).get(); }
}
