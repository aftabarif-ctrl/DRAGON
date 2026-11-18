package com.dragonlords;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DragonLords.MOD_ID);
    public static final Map<DragonType, DeferredItem<Item>> EGGS = new EnumMap<>(DragonType.class);

    static {
        for (DragonType t : DragonType.values()) {
            EGGS.put(t, ITEMS.register(t.entityName() + "_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.TYPES.get(t), t.eggPrimary, t.eggSecondary, new Item.Properties())));
        }
    }
}
