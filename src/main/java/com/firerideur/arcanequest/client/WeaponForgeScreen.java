package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.client.ui.UiKit;
import com.firerideur.arcanequest.forge.WeaponForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class WeaponForgeScreen extends ItemCombinerScreen<WeaponForgeMenu> {
    private static final int ARROW_X = 66, ARROW_Y = 49;

    public WeaponForgeScreen(WeaponForgeMenu menu, Inventory inventory, Component title) {
        // The texture argument is unused: the background is drawn by UiKit
        super(menu, inventory, title, ArcaneQuest.id("textures/gui/arcane_ui.png"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        UiKit.panel(g, leftPos, topPos, imageWidth, imageHeight);
        UiKit.title(g, font, title, leftPos + 8, topPos + 8, imageWidth - 16);
        g.drawString(font, Component.translatable("container.arcanequest.weapon_forge.slots"), leftPos + 8, topPos + 30,
                UiKit.TEXT_DIM, false);
        UiKit.title(g, font, playerInventoryTitle, leftPos + 8, topPos + 70, imageWidth - 16);
        for (Slot slot : menu.slots) {
            UiKit.slot(g, leftPos + slot.x - 1, topPos + slot.y - 1, slot == hoveredSlot);
        }
        UiKit.frame(g, leftPos + WeaponForgeMenu.RESULT_X - 4, topPos + WeaponForgeMenu.SLOT_Y - 4, 24, 24, UiKit.GOLD);
        UiKit.arrow(g, leftPos + ARROW_X, topPos + ARROW_Y);
        renderErrorIcon(g, leftPos, topPos);
    }

    @Override
    protected void renderErrorIcon(GuiGraphics g, int left, int top) {
        if (menu.hasInvalidRecipe()) {
            g.drawCenteredString(font, "✖", left + ARROW_X + 12, top + ARROW_Y + 4, 0xFFE04040);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Titles are drawn in renderBg
    }
}
