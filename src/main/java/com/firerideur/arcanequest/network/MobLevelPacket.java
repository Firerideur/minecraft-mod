package com.firerideur.arcanequest.network;

import com.firerideur.arcanequest.client.ClientMobLevels;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Server -> client: level of a mob the player just started seeing. */
public record MobLevelPacket(int entityId, int level, boolean elite) {
    public static void encode(MobLevelPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.level);
        buf.writeBoolean(packet.elite);
    }

    public static MobLevelPacket decode(FriendlyByteBuf buf) {
        return new MobLevelPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(MobLevelPacket packet, CustomPayloadEvent.Context context) {
        ClientMobLevels.put(packet.entityId, packet.level, packet.elite);
        context.setPacketHandled(true);
    }
}
