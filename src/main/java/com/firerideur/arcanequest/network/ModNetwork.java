package com.firerideur.arcanequest.network;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    private static final int PROTOCOL_VERSION = 4;

    public static final SimpleChannel CHANNEL = ChannelBuilder.named(ArcaneQuest.id("main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(SyncStatsPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStatsPacket::encode)
                .decoder(SyncStatsPacket::decode)
                .consumerMainThread(SyncStatsPacket::handle)
                .add();
        CHANNEL.messageBuilder(SpendPointPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpendPointPacket::encode)
                .decoder(SpendPointPacket::decode)
                .consumerMainThread(SpendPointPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChooseClassPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChooseClassPacket::encode)
                .decoder(ChooseClassPacket::decode)
                .consumerMainThread(ChooseClassPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenCharacterMenuPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenCharacterMenuPacket::encode)
                .decoder(OpenCharacterMenuPacket::decode)
                .consumerMainThread(OpenCharacterMenuPacket::handle)
                .add();
        CHANNEL.messageBuilder(UseSkillPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UseSkillPacket::encode)
                .decoder(UseSkillPacket::decode)
                .consumerMainThread(UseSkillPacket::handle)
                .add();
        CHANNEL.messageBuilder(SkillCooldownPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SkillCooldownPacket::encode)
                .decoder(SkillCooldownPacket::decode)
                .consumerMainThread(SkillCooldownPacket::handle)
                .add();
        CHANNEL.messageBuilder(MobLevelPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MobLevelPacket::encode)
                .decoder(MobLevelPacket::decode)
                .consumerMainThread(MobLevelPacket::handle)
                .add();
    }

    public static void sync(ServerPlayer player) {
        StatsCapability.get(player).ifPresent(stats ->
                CHANNEL.send(new SyncStatsPacket(stats.save()), PacketDistributor.PLAYER.with(player)));
    }

    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }
}
