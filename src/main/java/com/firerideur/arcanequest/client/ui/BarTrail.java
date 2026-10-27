package com.firerideur.arcanequest.client.ui;

import net.minecraft.Util;

/**
 * "Damage trail" for health bars: when the value drops, a pale segment shows the lost part
 * for a moment, then slides down to the new value.
 */
public class BarTrail {
    private static final long HOLD_MS = 400;
    private static final float DRAIN_PER_SECOND = 0.6F;

    private float trail = 1.0F;
    private float lastValue = 1.0F;
    private long lastDrop;
    private long lastUpdate = Util.getMillis();

    /** Starts over at {@code value}, without showing a trail (e.g. a new target). */
    public void reset(float value) {
        trail = value;
        lastValue = value;
    }

    /** @return where the trail should end for the current value (0..1) */
    public float update(float value) {
        long now = Util.getMillis();
        float seconds = (now - lastUpdate) / 1000.0F;
        lastUpdate = now;
        if (value < lastValue - 0.001F) {
            lastDrop = now;
        }
        lastValue = value;
        if (value >= trail) {
            trail = value;
        } else if (now - lastDrop > HOLD_MS) {
            trail = Math.max(value, trail - DRAIN_PER_SECOND * seconds);
        }
        return trail;
    }
}
