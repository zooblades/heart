package com.heartbound.relationship;

import com.heartbound.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-level save data: affinity values, the "follows" table, partners and homes.
 * Stored once per world (in the overworld's data storage), so it works the same on every mod loader
 * and for every dimension.
 */
public class RelationshipData extends SavedData {

    public static final String NAME = Constants.MOD_ID + "_relationships";

    private final AffinityTable table = new AffinityTable();
    private final FollowTable following = new FollowTable();
    private final PartnerTable partners = new PartnerTable();
    private final Map<UUID, Home> homes = new HashMap<>();

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
        ListTag follows = tag.getList("following", Tag.TAG_COMPOUND);
        for (int i = 0; i < follows.size(); i++) {
            CompoundTag entry = follows.getCompound(i);
            if (entry.hasUUID("mob") && entry.hasUUID("player")) {
                data.following.set(entry.getUUID("mob"), entry.getUUID("player"));
            }
        }
        ListTag pairs = tag.getList("partners", Tag.TAG_COMPOUND);
        for (int i = 0; i < pairs.size(); i++) {
            CompoundTag entry = pairs.getCompound(i);
            if (entry.hasUUID("mob") && entry.hasUUID("player")) {
                data.partners.pair(entry.getUUID("mob"), entry.getUUID("player"));
            }
        }
        ListTag homeList = tag.getList("homes", Tag.TAG_COMPOUND);
        for (int i = 0; i < homeList.size(); i++) {
            CompoundTag entry = homeList.getCompound(i);
            if (entry.hasUUID("mob")) {
                data.homes.put(entry.getUUID("mob"), new Home(entry.getString("dimension"), entry.getLong("pos")));
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

        ListTag follows = new ListTag();
        for (Map.Entry<UUID, UUID> e : following.snapshot().entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("mob", e.getKey());
            entry.putUUID("player", e.getValue());
            follows.add(entry);
        }
        tag.put("following", follows);

        ListTag pairs = new ListTag();
        for (Map.Entry<UUID, UUID> e : partners.snapshot().entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("mob", e.getKey());
            entry.putUUID("player", e.getValue());
            pairs.add(entry);
        }
        tag.put("partners", pairs);

        ListTag homeList = new ListTag();
        for (Map.Entry<UUID, Home> e : homes.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("mob", e.getKey());
            entry.putString("dimension", e.getValue().dimension());
            entry.putLong("pos", e.getValue().pos());
            homeList.add(entry);
        }
        tag.put("homes", homeList);
        return tag;
    }

    // ---- affinity

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
        following.clear(mob);
        partners.unpairMob(mob);
        homes.remove(mob);
        setDirty();
    }

    // ---- following

    /** The player the mob follows, or null. */
    public UUID getFollowTarget(UUID mob) {
        return following.get(mob);
    }

    /** Starts following; a following mob no longer has a home. */
    public void setFollowing(UUID mob, UUID player) {
        following.set(mob, player);
        homes.remove(mob);
        setDirty();
    }

    public void stopFollowing(UUID mob) {
        if (following.clear(mob)) {
            setDirty();
        }
    }

    public int countFollowing(UUID player) {
        return following.count(player);
    }

    public Map<UUID, UUID> followingSnapshot() {
        return following.snapshot();
    }

    // ---- partners

    public UUID getPartnerOfMob(UUID mob) {
        return partners.partnerOfMob(mob);
    }

    public UUID getPartnerOfPlayer(UUID player) {
        return partners.partnerOfPlayer(player);
    }

    public boolean isPartner(UUID mob, UUID player) {
        return partners.isPair(mob, player);
    }

    public boolean pair(UUID mob, UUID player) {
        boolean ok = partners.pair(mob, player);
        if (ok) {
            setDirty();
        }
        return ok;
    }

    /** Ends the pairing of this mob; returns the former partner player or null. */
    public UUID unpairMob(UUID mob) {
        UUID player = partners.unpairMob(mob);
        if (player != null) {
            setDirty();
        }
        return player;
    }

    /** Ends the pairing of this player; returns the former partner mob or null. */
    public UUID unpairPlayer(UUID player) {
        UUID mob = partners.unpairPlayer(player);
        if (mob != null) {
            setDirty();
        }
        return mob;
    }

    /** Mob to player, as a copy. */
    public Map<UUID, UUID> partnersSnapshot() {
        return partners.snapshot();
    }

    // ---- homes

    public Home getHome(UUID mob) {
        return homes.get(mob);
    }

    /** Sets the home; a mob with a home does not follow anyone. */
    public void setHome(UUID mob, Home home) {
        homes.put(mob, home);
        following.clear(mob);
        setDirty();
    }

    public void clearHome(UUID mob) {
        if (homes.remove(mob) != null) {
            setDirty();
        }
    }

    public Map<UUID, Home> homesSnapshot() {
        return new HashMap<>(homes);
    }
}
