package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.fusion.FusionForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FusionForgeScreen extends AbstractContainerScreen<FusionForgeMenu> {
    private static final ResourceLocation TEXTURE = ArcaneQuest.id("textures/gui/container/fusion_forge.png");
    // Sprites stored to the right of the panel in the texture
    private static final int ARROW_U = 176, ARROW_V = 0, ARROW_W = 30, ARROW_H = 17;
    private static final int ARROW_X = 74, ARROW_Y = 34;
    private static final int CHARGE_U = 176, CHARGE_V = 17, CHARGE_W = 4, CHARGE_H = 18;
    private static final int CHARGE_X = 28, CHARGE_Y = 35;

    public FusionForgeScreen(FusionForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(CHARGE_X, CHARGE_Y, CHARGE_W, CHARGE_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("container.arcanequest.fusion_forge.charge",
                    menu.charge(), menu.maxCharge()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int arrow = Math.round(menu.progress() * ARROW_W);
        if (arrow > 0) {
            graphics.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, arrow, ARROW_H);
        }
        int charge = Math.round((float) menu.charge() / menu.maxCharge() * CHARGE_H);
        if (charge > 0) {
            graphics.blit(TEXTURE, leftPos + CHARGE_X, topPos + CHARGE_Y + CHARGE_H - charge,
                    CHARGE_U, CHARGE_V + CHARGE_H - charge, CHARGE_W, charge);
        }
    }
}
