package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.character.CharacterMenu;
import com.firerideur.arcanequest.forge.WeaponForgeMenu;
import com.firerideur.arcanequest.fusion.FusionForgeMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ArcaneQuest.MODID);

    public static final RegistryObject<MenuType<FusionForgeMenu>> FUSION_FORGE = MENUS.register("fusion_forge",
            () -> new MenuType<>(FusionForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<WeaponForgeMenu>> WEAPON_FORGE = MENUS.register("weapon_forge",
            () -> new MenuType<>(WeaponForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<CharacterMenu>> CHARACTER = MENUS.register("character",
            () -> new MenuType<>(CharacterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
