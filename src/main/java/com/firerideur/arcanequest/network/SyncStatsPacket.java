package com.firerideur.arcanequest.network;

import com.firerideur.arcanequest.client.ClientStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Server -> client: full copy of the player's RPG data. */
public record SyncStatsPacket(CompoundTag data) {
    public static void encode(SyncStatsPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncStatsPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncStatsPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncStatsPacket packet, CustomPayloadEvent.Context context) {
        ClientStats.get().load(packet.data);
        context.setPacketHandled(true);
    }
}
