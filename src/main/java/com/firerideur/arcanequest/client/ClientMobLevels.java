package com.firerideur.arcanequest.client;

import java.util.HashMap;
import java.util.Map;

/** Levels of the mobs around the local player, keyed by entity id. */
public final class ClientMobLevels {
    public record Info(int level, boolean elite) {}

    private static final Map<Integer, Info> LEVELS = new HashMap<>();

    private ClientMobLevels() {}

    public static void put(int entityId, int level, boolean elite) {
        LEVELS.put(entityId, new Info(level, elite));
    }

    public static Info get(int entityId) {
        return LEVELS.get(entityId);
    }

    public static void clear() {
        LEVELS.clear();
    }
}
