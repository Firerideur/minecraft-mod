package com.firerideur.arcanequest.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Every dungeon created behind a gate: its rank, whether it was built and cleared, and where it is. */
public class DungeonData extends SavedData {
    private static final String NAME = "arcanequest_dungeons";
    private static final Factory<DungeonData> FACTORY = new Factory<>(DungeonData::new, DungeonData::load, null);

    /** Distance between two dungeons in the dungeon dimension. */
    public static final int SPACING = 1024;

    public static final class Dungeon {
        public final int id;
        public final int stars;
        /** Name of the {@link DungeonTheme}. */
        public String theme;
        public boolean generated;
        public boolean cleared;
        public BlockPos entry = BlockPos.ZERO;
        public BlockPos bossRoom = BlockPos.ZERO;

        Dungeon(int id, int stars, String theme) {
            this.id = id;
            this.stars = stars;
            this.theme = theme;
        }

        public DungeonTheme theme() {
            return DungeonTheme.byName(theme, stars);
        }

        /** Where this dungeon is built in the dungeon dimension. */
        public BlockPos origin() {
            return new BlockPos(id * SPACING, 100, 0);
        }
    }

    private int nextId;
    private final Map<Integer, Dungeon> dungeons = new HashMap<>();

    public static DungeonData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public Dungeon create(int stars, DungeonTheme theme) {
        Dungeon dungeon = new Dungeon(nextId++, stars, theme.name());
        dungeons.put(dungeon.id, dungeon);
        setDirty();
        return dungeon;
    }

    public Dungeon get(int id) {
        return dungeons.get(id);
    }

    private static DungeonData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonData data = new DungeonData();
        data.nextId = tag.getInt("NextId");
        for (Tag element : tag.getList("Dungeons", Tag.TAG_COMPOUND)) {
            CompoundTag d = (CompoundTag) element;
            int stars = d.getInt("Stars");
            Dungeon dungeon = new Dungeon(d.getInt("Id"), stars, d.contains("Theme") ? d.getString("Theme") : DungeonTheme.of(stars).name());
            dungeon.generated = d.getBoolean("Generated");
            dungeon.cleared = d.getBoolean("Cleared");
            dungeon.entry = NbtUtils.readBlockPos(d, "Entry").orElse(BlockPos.ZERO);
            dungeon.bossRoom = NbtUtils.readBlockPos(d, "BossRoom").orElse(BlockPos.ZERO);
            data.dungeons.put(dungeon.id, dungeon);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextId", nextId);
        ListTag list = new ListTag();
        for (Dungeon dungeon : dungeons.values()) {
            CompoundTag d = new CompoundTag();
            d.putInt("Id", dungeon.id);
            d.putInt("Stars", dungeon.stars);
            d.putString("Theme", dungeon.theme);
            d.putBoolean("Generated", dungeon.generated);
            d.putBoolean("Cleared", dungeon.cleared);
            d.put("Entry", NbtUtils.writeBlockPos(dungeon.entry));
            d.put("BossRoom", NbtUtils.writeBlockPos(dungeon.bossRoom));
            list.add(d);
        }
        tag.put("Dungeons", list);
        return tag;
    }
}
