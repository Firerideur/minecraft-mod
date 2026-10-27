package com.firerideur.arcanequest.network;

import com.firerideur.arcanequest.character.CharacterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> server: the player pressed the character menu key. */
public record OpenCharacterMenuPacket() {
    public static void encode(OpenCharacterMenuPacket packet, FriendlyByteBuf buf) {
    }

    public static OpenCharacterMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenCharacterMenuPacket();
    }

    public static void handle(OpenCharacterMenuPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CharacterMenu(id, inventory),
                    Component.translatable("screen.arcanequest.menu")));
        }
        context.setPacketHandled(true);
    }
}
