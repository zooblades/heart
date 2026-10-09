package com.heartbound.menu;

import com.heartbound.interaction.InteractionHandler;
import com.heartbound.config.HeartboundConfig;
import com.heartbound.relationship.EventType;
import com.heartbound.relationship.Gender;
import com.heartbound.relationship.GiftKind;
import com.heartbound.relationship.GiftPreferences;
import com.heartbound.relationship.PairMemory;
import com.heartbound.relationship.RelationshipData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The relationship window. It has no item slots; it only syncs a few numbers (affinity, gender,
 * target entity id) to the client through vanilla container data and receives button clicks through
 * vanilla {@link #clickMenuButton}, so no custom networking is needed.
 */
public class RelationshipMenu extends AbstractContainerMenu {

    public static final int BUTTON_GIFT = 0;
    public static final int BUTTON_FOLLOW = 2;
    public static final int BUTTON_PROPOSE = 3;
    public static final int BUTTON_HOME = 4;

    private static final int DATA_AFFINITY = 0;
    private static final int DATA_GENDER = 1;
    private static final int DATA_ENTITY_LOW = 2;
    private static final int DATA_ENTITY_HIGH = 3;
    private static final int DATA_FOLLOWING = 4;
    private static final int DATA_PARTNER = 5;
    private static final int DATA_KNOWLEDGE = 6;
    private static final int DATA_FAVORITE = 7;
    private static final int DATA_COUNTERS = 8;
    private static final int DATA_EVENTS = 12;
    public static final int EVENT_SLOTS = 12;
    private static final int DATA_COUNT = DATA_EVENTS + EVENT_SLOTS * 3;

    private final ContainerData data;
    /** Only set on the server. */
    private final Entity target;

    /** Client side constructor. */
    public RelationshipMenu(int containerId, Inventory inventory) {
        this(containerId, new SimpleContainerData(DATA_COUNT), null);
    }

    /** Server side constructor. */
    public RelationshipMenu(int containerId, Inventory inventory, Entity target) {
        this(containerId, serverData(inventory.player, target), target);
    }

    private RelationshipMenu(int containerId, ContainerData data, Entity target) {
        super(ModMenus.RELATIONSHIP, containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.target = target;
        addDataSlots(data);
    }

    private static ContainerData serverData(Player player, Entity target) {
        MinecraftServer server = player.getServer();
        UUID playerId = player.getUUID();
        UUID mobId = target.getUUID();
        int gender = Gender.of(mobId).ordinal();
        int entityId = target.getId();
        String mobPath = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        int favorite = GiftPreferences.favorite(mobPath, Gender.of(mobId), HeartboundConfig.get().giftGains).ordinal() + 1;
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index >= DATA_COUNTERS) {
                    return server == null ? 0 : memoryValue(server, mobId, playerId, index);
                }
                return switch (index) {
                    case DATA_AFFINITY -> server == null ? 0 : RelationshipData.get(server).get(mobId, playerId);
                    case DATA_GENDER -> gender;
                    case DATA_ENTITY_LOW -> entityId & 0xFFFF;
                    case DATA_ENTITY_HIGH -> entityId >>> 16;
                    case DATA_KNOWLEDGE -> server == null ? 0 : RelationshipData.get(server).getKnowledge(mobId, playerId);
                    case DATA_FAVORITE -> favorite;
                    case DATA_PARTNER -> server != null && RelationshipData.get(server).isPartner(mobId, playerId) ? 1 : 0;
                    case DATA_FOLLOWING -> server != null && playerId.equals(RelationshipData.get(server).getFollowTarget(mobId)) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static int memoryValue(MinecraftServer server, UUID mobId, UUID playerId, int index) {
        PairMemory memory = RelationshipData.get(server).getMemory(mobId, playerId);
        if (memory == null) {
            return 0;
        }
        if (index < DATA_EVENTS) {
            PairMemory.Counter[] counters = PairMemory.Counter.values();
            int i = index - DATA_COUNTERS;
            return i < counters.length ? memory.count(counters[i]) : 0;
        }
        int relative = index - DATA_EVENTS;
        int slot = relative / 3;
        int field = relative % 3;
        java.util.List<PairMemory.Entry> entries = memory.entries();
        if (slot >= entries.size()) {
            return 0;
        }
        PairMemory.Entry entry = entries.get(slot);
        return switch (field) {
            case 0 -> entry.type().ordinal() + 1;
            case 1 -> (int) Math.min(30000L, entry.day());
            default -> entry.extra();
        };
    }

    /** One line of the diary as the client sees it. */
    public record DiaryLine(EventType type, int day, int extra) {
    }

    public int getCounter(PairMemory.Counter counter) {
        return data.get(DATA_COUNTERS + counter.ordinal());
    }

    /** The remembered events in order (at most EVENT_SLOTS). */
    public java.util.List<DiaryLine> getDiary() {
        java.util.List<DiaryLine> lines = new java.util.ArrayList<>();
        for (int slot = 0; slot < EVENT_SLOTS; slot++) {
            int base = DATA_EVENTS + slot * 3;
            EventType type = EventType.byOrdinal(data.get(base) - 1);
            if (type == null) {
                break;
            }
            lines.add(new DiaryLine(type, data.get(base + 1), data.get(base + 2)));
        }
        return lines;
    }

    public int getAffinity() {
        return data.get(DATA_AFFINITY);
    }

    public Gender getGender() {
        Gender[] values = Gender.values();
        int index = data.get(DATA_GENDER);
        return values[Math.max(0, Math.min(values.length - 1, index))];
    }

    /** 0 nothing known, 1 character known, 2 favourite gift known. */
    public int getKnowledge() {
        return data.get(DATA_KNOWLEDGE);
    }

    /** The favourite gift, or null if unknown. */
    public GiftKind getFavoriteGift() {
        int index = data.get(DATA_FAVORITE) - 1;
        GiftKind[] kinds = GiftKind.values();
        return index >= 0 && index < kinds.length ? kinds[index] : null;
    }

    /** The mob this window is about; only available on the server. */
    public Entity getTargetEntity() {
        return target;
    }

    public boolean isPartner() {
        return data.get(DATA_PARTNER) == 1;
    }

    public boolean isFollowing() {
        return data.get(DATA_FOLLOWING) == 1;
    }

    public int getTargetId() {
        return ((data.get(DATA_ENTITY_HIGH) & 0xFFFF) << 16) | (data.get(DATA_ENTITY_LOW) & 0xFFFF);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || target == null || !target.isAlive()) {
            return false;
        }
        switch (id) {
            case BUTTON_GIFT:
                InteractionHandler.giveHeldGift(serverPlayer, target);
                return true;
            case BUTTON_FOLLOW:
                InteractionHandler.toggleFollow(serverPlayer, target);
                return true;
            case BUTTON_PROPOSE:
                InteractionHandler.proposeWithHeldRing(serverPlayer, target);
                return true;
            case BUTTON_HOME:
                InteractionHandler.setHome(serverPlayer, target);
                return true;
            default:
                return false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return target == null || (target.isAlive() && player.distanceToSqr(target) < 64.0D);
    }
}
