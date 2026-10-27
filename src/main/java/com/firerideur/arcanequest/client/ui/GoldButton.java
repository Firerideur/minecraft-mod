package com.firerideur.arcanequest.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Button drawn in the Arcane Quest style. */
public class GoldButton extends AbstractButton {
    private final Runnable onPress;

    public GoldButton(int x, int y, int width, int height, Component message, Runnable onPress, Component tooltip) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        if (tooltip != null) {
            setTooltip(Tooltip.create(tooltip));
        }
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiKit.button(graphics, Minecraft.getInstance().font, getMessage(), getX(), getY(), getWidth(), getHeight(),
                isHoveredOrFocused(), active);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
