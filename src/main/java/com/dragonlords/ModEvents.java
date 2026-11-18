package com.dragonlords;

import com.dragonlords.entity.DragonEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = DragonLords.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        for (DragonType t : DragonType.values())
            e.put(ModEntities.get(t), DragonEntity.createAttributes(t).build());
    }

    @SubscribeEvent
    public static void spawns(RegisterSpawnPlacementsEvent e) {
        for (DragonType t : DragonType.values())
            e.register(ModEntities.get(t), SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DragonEntity::checkDragonSpawn,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void payloads(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToServer(DragonActionPayload.TYPE, DragonActionPayload.CODEC, DragonActionPayload::handle);
    }

    @SubscribeEvent
    public static void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS)
            ModItems.EGGS.values().forEach(i -> e.accept(i.get()));
    }
}
