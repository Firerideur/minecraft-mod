package com.firerideur.arcanequest.network;

import com.firerideur.arcanequest.client.ClientSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Server -> client: a skill went on cooldown ({@code skill} = -1: every cooldown was reset). */
public record SkillCooldownPacket(int skill, int ticks) {
    public static void encode(SkillCooldownPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.skill);
        buf.writeVarInt(packet.ticks);
    }

    public static SkillCooldownPacket decode(FriendlyByteBuf buf) {
        return new SkillCooldownPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SkillCooldownPacket packet, CustomPayloadEvent.Context context) {
        ClientSkills.onCooldown(packet.skill, packet.ticks);
        context.setPacketHandled(true);
    }
}
