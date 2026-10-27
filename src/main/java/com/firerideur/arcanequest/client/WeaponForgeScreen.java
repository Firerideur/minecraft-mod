package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.forge.WeaponForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WeaponForgeScreen extends ItemCombinerScreen<WeaponForgeMenu> {
    private static final ResourceLocation TEXTURE = ArcaneQuest.id("textures/gui/container/weapon_forge.png");
    private static final int ERROR_U = 176, ERROR_V = 0, ERROR_SIZE = 16;
    private static final int ERROR_X = 65, ERROR_Y = 46;

    public WeaponForgeScreen(WeaponForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
        titleLabelX = 44;
        titleLabelY = 15;
    }

    @Override
    protected void renderErrorIcon(GuiGraphics graphics, int left, int top) {
        if (menu.hasInvalidRecipe()) {
            graphics.blit(TEXTURE, left + ERROR_X, top + ERROR_Y, ERROR_U, ERROR_V, ERROR_SIZE, ERROR_SIZE);
        }
    }
}
