package com.firerideur.arcanequest.boss;

/** Marks Arcane Quest bosses: fixed level, big XP reward, excluded from random mob levels. */
public interface ArcaneBoss {
    int getBossLevel();

    long getXpReward();
}
