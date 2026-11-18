package com.dragonlords;

import com.dragonlords.entity.DragonEntity;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: 0 = breath (held), 1 = charge. */
public record DragonActionPayload(int action) implements CustomPacketPayload {
    public static final Type<DragonActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DragonLords.MOD_ID, "action"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, DragonActionPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(DragonActionPayload::new, DragonActionPayload::action);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DragonActionPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player pl = ctx.player();
            if (pl.getVehicle() instanceof DragonEntity d && d.getControllingPassenger() == pl) d.performAction(p.action());
        });
    }
}
