package com.firerideur.arcanequest.network;

import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.StatsCapability;
import com.firerideur.arcanequest.stats.StatsEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: the player picks a class. Only one of their offers, at level 20, while they have none. */
public record ChooseClassPacket(int classId) {
    public static void encode(ChooseClassPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.classId);
    }

    public static ChooseClassPacket decode(FriendlyByteBuf buf) {
        return new ChooseClassPacket(buf.readVarInt());
    }

    public static void handle(ChooseClassPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        PlayerClass chosen = PlayerClass.byId(packet.classId);
        if (player != null && chosen != PlayerClass.NONE) {
            StatsCapability.get(player).ifPresent(stats -> PlayerClass.choose(stats, chosen));
            StatsEvents.refresh(player);
        }
        context.setPacketHandled(true);
    }
}
