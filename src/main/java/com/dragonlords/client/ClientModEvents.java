package com.dragonlords.client;

import com.dragonlords.DragonLords;
import com.dragonlords.DragonType;
import com.dragonlords.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = DragonLords.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    public static final KeyMapping BREATH = new KeyMapping("key.dragonlords.breath", InputConstants.KEY_R, "key.categories.dragonlords");
    public static final KeyMapping CHARGE = new KeyMapping("key.dragonlords.charge", InputConstants.KEY_V, "key.categories.dragonlords");

    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.register(BREATH); e.register(CHARGE); }

    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(DragonModel.LAYER, DragonModel::createBodyLayer);
    }

    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (DragonType t : DragonType.values())
            e.registerEntityRenderer(ModEntities.get(t), ctx -> new DragonRenderer(ctx, t));
    }
}
