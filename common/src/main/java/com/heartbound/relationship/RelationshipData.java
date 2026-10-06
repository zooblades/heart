package com.heartbound.relationship;

import com.heartbound.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;

/**
 * World-level save data holding all affinity values. It is stored once per world (in the overworld's
 * data storage), so it works the same on every mod loader and for every dimension.
 */
public class RelationshipData extends SavedData {

    public static final String NAME = Constants.MOD_ID + "_relationships";

    private final AffinityTable table = new AffinityTable();

    public RelationshipData() {
    }

    public static SavedData.Factory<RelationshipData> factory() {
        return new SavedData.Factory<>(RelationshipData::new, RelationshipData::load, DataFixTypes.LEVEL);
    }

    public static RelationshipData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    public static RelationshipData load(CompoundTag tag, HolderLookup.Provider registries) {
        RelationshipData data = new RelationshipData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("mob") && entry.hasUUID("player")) {
                data.table.set(entry.getUUID("mob"), entry.getUUID("player"), entry.getInt("value"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<AffinityTable.Key, Integer> e : table.entries().entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("mob", e.getKey().mob());
            entry.putUUID("player", e.getKey().player());
            entry.putInt("value", e.getValue());
            list.add(entry);
        }
        tag.put("entries", list);
        return tag;
    }

    public int get(UUID mob, UUID player) {
        return table.get(mob, player);
    }

    public int set(UUID mob, UUID player, int value) {
        int result = table.set(mob, player, value);
        setDirty();
        return result;
    }

    public int add(UUID mob, UUID player, int delta) {
        int result = table.add(mob, player, delta);
        setDirty();
        return result;
    }

    public void removeMob(UUID mob) {
        table.removeMob(mob);
        setDirty();
    }
}
