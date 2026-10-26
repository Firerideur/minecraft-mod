package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.stats.PlayerStats;

/** Client-side copy of the local player's RPG data, kept up to date by {@link com.firerideur.arcanequest.network.SyncStatsPacket}. */
public final class ClientStats {
    private static final PlayerStats STATS = new PlayerStats();

    private ClientStats() {}

    public static PlayerStats get() {
        return STATS;
    }
}
