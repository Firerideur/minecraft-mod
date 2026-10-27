package com.firerideur.arcanequest.network;

import com.firerideur.arcanequest.skill.SkillRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: the player pressed the key of a skill slot. */
public record UseSkillPacket(int slot) {
    public static void encode(UseSkillPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slot);
    }

    public static UseSkillPacket decode(FriendlyByteBuf buf) {
        return new UseSkillPacket(buf.readVarInt());
    }

    public static void handle(UseSkillPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            SkillRuntime.use(player, packet.slot);
        }
        context.setPacketHandled(true);
    }
}
