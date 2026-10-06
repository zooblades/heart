package com.heartbound.relationship;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One-to-one pairing between a mob and a player. Pure Java. */
public final class PartnerTable {

    private final Map<UUID, UUID> mobToPlayer = new HashMap<>();
    private final Map<UUID, UUID> playerToMob = new HashMap<>();

    public UUID partnerOfMob(UUID mob) {
        return mobToPlayer.get(mob);
    }

    public UUID partnerOfPlayer(UUID player) {
        return playerToMob.get(player);
    }

    public boolean isPair(UUID mob, UUID player) {
        return player.equals(mobToPlayer.get(mob));
    }

    /** Pairs them unless either already has a partner. */
    public boolean pair(UUID mob, UUID player) {
        if (mobToPlayer.containsKey(mob) || playerToMob.containsKey(player)) {
            return false;
        }
        mobToPlayer.put(mob, player);
        playerToMob.put(player, mob);
        return true;
    }

    /** Returns the former partner (player) or null. */
    public UUID unpairMob(UUID mob) {
        UUID player = mobToPlayer.remove(mob);
        if (player != null) {
            playerToMob.remove(player);
        }
        return player;
    }

    /** Returns the former partner (mob) or null. */
    public UUID unpairPlayer(UUID player) {
        UUID mob = playerToMob.remove(player);
        if (mob != null) {
            mobToPlayer.remove(mob);
        }
        return mob;
    }

    /** Mob to player, as a copy. */
    public Map<UUID, UUID> snapshot() {
        return new HashMap<>(mobToPlayer);
    }
}
