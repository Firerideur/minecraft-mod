package com.firerideur.arcanequest;

import com.firerideur.arcanequest.client.ClientSetup;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.stats.StatsEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(ArcaneQuest.MODID)
public class ArcaneQuest {
    public static final String MODID = "arcanequest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneQuest(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // Game events (kills, login, respawn, commands...)
        MinecraftForge.EVENT_BUS.register(StatsEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
