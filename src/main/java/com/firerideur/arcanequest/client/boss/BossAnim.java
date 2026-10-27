package com.firerideur.arcanequest.client.boss;

import net.minecraft.util.Mth;

/** Small helpers for the boss animations. */
final class BossAnim {
    private BossAnim() {}

    /**
     * Weight of an attack pose: eases in over {@code in} ticks after the start and out over the last {@code out} ticks,
     * so poses blend with walking instead of snapping.
     */
    static float weight(float time, float length, float in, float out) {
        return Mth.clamp(time / in, 0.0F, 1.0F) * Mth.clamp((length - time) / out, 0.0F, 1.0F);
    }

    /** 0 before {@code start}, then rises to 1 over {@code duration} ticks. */
    static float after(float time, float start, float duration) {
        return Mth.clamp((time - start) / duration, 0.0F, 1.0F);
    }

    static float lerp(float k, float from, float to) {
        return from + (to - from) * k;
    }
}
