package com.firerideur.arcanequest.boss;

/** Marks Arcane Quest bosses: fixed level, big XP reward, excluded from random mob levels. */
public interface ArcaneBoss {
    int getBossLevel();

    long getXpReward();

    /** Bit of this boss in the player's defeated bosses, see {@link com.firerideur.arcanequest.stats.PlayerClass.BossKills}. */
    int getBossId();

    /** Shared boss state; the client reads the running attack from it to animate the model. */
    BossCore getCore();
}
