package com.firerideur.arcanequest.skill;

import com.firerideur.arcanequest.stats.PlayerStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A skill being used.
 *
 * @param power multiplier of the skill's damage, healing and durations: grows with level, and with spell power
 *              (magic skills) or strength (the others)
 */
public record SkillContext(ServerPlayer player, PlayerStats stats, ServerLevel level, Skill skill, float power) {
}
