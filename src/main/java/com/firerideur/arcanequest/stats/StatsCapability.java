package com.firerideur.arcanequest.stats;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Attaches {@link PlayerStats} to every player and saves it with the player data.
 */
public final class StatsCapability {
    public static final Capability<PlayerStats> STATS = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = ArcaneQuest.id("player_stats");

    private StatsCapability() {}

    public static Optional<PlayerStats> get(Player player) {
        return player.getCapability(STATS).resolve();
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerStats stats = new PlayerStats();
        private LazyOptional<PlayerStats> optional = LazyOptional.of(() -> stats);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (!optional.isPresent()) {
                // Invalidated when the player died, then revived for the Clone event: hand out a fresh one,
                // otherwise the stats of the dead player can't be copied and are lost.
                optional = LazyOptional.of(() -> stats);
            }
            return STATS.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
            return stats.save();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
            stats.load(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
