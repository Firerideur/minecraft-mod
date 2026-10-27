package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.client.ui.UiKit;
import com.firerideur.arcanequest.skill.ClassSkills;
import com.firerideur.arcanequest.skill.Skill;
import com.firerideur.arcanequest.stats.PlayerStats;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Skill slots at the bottom right of the screen: icon, key, cooldown and lock level. */
public final class SkillBar {
    private static final int SLOT = UiKit.SLOT_SIZE;
    private static final int GAP = 3;

    private SkillBar() {}

    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        PlayerStats stats = ClientStats.get();
        List<Skill> skills = ClassSkills.skillsOf(stats.getPlayerClass());
        if (skills.isEmpty()) {
            return;
        }
        int count = skills.size();
        int width = count * SLOT + (count - 1) * GAP;
        int x = g.guiWidth() - width - 6;
        int y = g.guiHeight() - SLOT - 8;
        if (x < g.guiWidth() / 2 + 96) {
            // Narrow screen: go above the hunger bar instead of over the hotbar
            y -= 34;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        for (int slot = 0; slot < count; slot++) {
            Skill skill = skills.get(slot);
            int sx = x + slot * (SLOT + GAP);
            boolean unlocked = stats.getLevel() >= ClassSkills.UNLOCK_LEVELS[slot];
            g.fill(sx, y, sx + SLOT, y + SLOT, 0xC0100E0C);
            UiKit.slot(g, sx, y, false);
            g.renderItem(new ItemStack(skill.icon()), sx + 1, y + 1);
            if (!unlocked) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(sx + 1, y + 1, sx + SLOT - 1, y + SLOT - 1, 0xC0000000);
                UiKit.barText(g, mc.font, String.valueOf(ClassSkills.UNLOCK_LEVELS[slot]), sx + SLOT / 2, y + 6, 0.75F);
                g.pose().popPose();
                continue;
            }
            float cooldown = ClientSkills.remaining(skill, partialTick);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            if (cooldown > 0) {
                int h = Math.round((SLOT - 2) * cooldown);
                g.fill(sx + 1, y + SLOT - 1 - h, sx + SLOT - 1, y + SLOT - 1, 0xB0000000);
                UiKit.barText(g, mc.font, String.valueOf(ClientSkills.secondsLeft(skill)), sx + SLOT / 2, y + 6, 0.75F);
            } else if (!mc.player.getAbilities().instabuild && stats.getMana() < skill.manaCost() * stats.getPlayerClass().bonuses().manaCost()) {
                // Not enough mana: blue tint
                g.fill(sx + 1, y + 1, sx + SLOT - 1, y + SLOT - 1, 0x803060C0);
            }
            String key = ClientSetup.SKILL_KEYS[slot].getTranslatedKeyMessage().getString();
            g.pose().scale(0.5F, 0.5F, 1.0F);
            g.drawString(mc.font, key, (sx + 2) * 2, (y + SLOT - 5) * 2, UiKit.GOLD_LIGHT, true);
            g.pose().popPose();
        }
    }
}
