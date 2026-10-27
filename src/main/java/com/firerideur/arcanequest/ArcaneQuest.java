package com.firerideur.arcanequest;

import com.firerideur.arcanequest.client.ClientSetup;
import com.firerideur.arcanequest.item.HammerItem;
import com.firerideur.arcanequest.mobs.MobEvents;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.registry.ModArmorMaterials;
import com.firerideur.arcanequest.registry.ModBlockEntities;
import com.firerideur.arcanequest.registry.ModBlocks;
import com.firerideur.arcanequest.registry.ModCreativeTabs;
import com.firerideur.arcanequest.registry.ModEntities;
import com.firerideur.arcanequest.registry.ModItems;
import com.firerideur.arcanequest.registry.ModMenus;
import com.firerideur.arcanequest.registry.ModRecipes;
import com.firerideur.arcanequest.dungeon.Gates;
import com.firerideur.arcanequest.skill.SkillRuntime;
import com.firerideur.arcanequest.stats.ClassEvents;
import com.firerideur.arcanequest.stats.StatsEvents;
import com.firerideur.arcanequest.weapon.WeaponEvents;
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

        // Blocks first: they also register their block items
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModEntities::registerAttributes);

        // Game events (kills, login, respawn, commands, mobs...)
        MinecraftForge.EVENT_BUS.register(StatsEvents.class);
        MinecraftForge.EVENT_BUS.register(ClassEvents.class);
        MinecraftForge.EVENT_BUS.register(SkillRuntime.class);
        MinecraftForge.EVENT_BUS.register(Gates.class);
        MinecraftForge.EVENT_BUS.register(MobEvents.class);
        MinecraftForge.EVENT_BUS.register(WeaponEvents.class);
        MinecraftForge.EVENT_BUS.register(HammerItem.class);

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
