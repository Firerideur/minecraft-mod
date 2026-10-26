package com.firerideur.arcanequest.network;

import com.firerideur.arcanequest.stats.Stat;
import com.firerideur.arcanequest.stats.StatEffects;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: the player wants to spend one point in a stat. */
public record SpendPointPacket(int statId) {
    public static void encode(SpendPointPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.statId);
    }

    public static SpendPointPacket decode(FriendlyByteBuf buf) {
        return new SpendPointPacket(buf.readVarInt());
    }

    public static void handle(SpendPointPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        Stat stat = Stat.byId(packet.statId);
        if (player != null && stat != null) {
            StatsCapability.get(player).ifPresent(stats -> {
                if (stats.spendPoint(stat)) {
                    StatEffects.apply(player, stats);
                }
                ModNetwork.sync(player);
            });
        }
        context.setPacketHandled(true);
    }
}
