package com.dragonlords.client;

import com.dragonlords.DragonActionPayload;
import com.dragonlords.DragonLords;
import com.dragonlords.entity.DragonEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DragonLords.MOD_ID, value = Dist.CLIENT)
public class ClientGameEvents {
    private static int ticks;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof DragonEntity)) return;
        ticks++;
        if (ClientModEvents.BREATH.isDown() && ticks % 4 == 0) PacketDistributor.sendToServer(new DragonActionPayload(0));
        while (ClientModEvents.CHARGE.consumeClick()) PacketDistributor.sendToServer(new DragonActionPayload(1));
    }
}
