package com.dragonlords;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(DragonLords.MOD_ID)
public class DragonLords {
    public static final String MOD_ID = "dragonlords";

    public DragonLords(IEventBus modBus) {
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
    }
}
