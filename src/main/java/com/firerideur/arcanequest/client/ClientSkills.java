package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.skill.Skill;
import net.minecraft.client.Minecraft;

import java.util.EnumMap;
import java.util.Map;

/** Client copy of the skill cooldowns, for the skill bar. */
public final class ClientSkills {
    private record Cooldown(long end, int length) {}

    private static final Map<Skill, Cooldown> COOLDOWNS = new EnumMap<>(Skill.class);

    private ClientSkills() {}

    public static void onCooldown(int skillId, int ticks) {
        if (skillId < 0) {
            COOLDOWNS.clear();
            return;
        }
        Skill skill = Skill.byId(skillId);
        if (skill != null) {
            COOLDOWNS.put(skill, new Cooldown(now() + ticks, ticks));
        }
    }

    /** Part of the cooldown left, from 1 (just used) to 0 (ready). */
    public static float remaining(Skill skill, float partialTick) {
        Cooldown cooldown = COOLDOWNS.get(skill);
        if (cooldown == null) {
            return 0.0F;
        }
        float left = cooldown.end - now() - partialTick;
        return left <= 0 ? 0.0F : left / cooldown.length;
    }

    /** Seconds left before {@code skill} is ready, 0 if ready. */
    public static int secondsLeft(Skill skill) {
        Cooldown cooldown = COOLDOWNS.get(skill);
        return cooldown == null ? 0 : (int) Math.max(0, (cooldown.end - now() + 19) / 20);
    }

    public static void clear() {
        COOLDOWNS.clear();
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }
}
