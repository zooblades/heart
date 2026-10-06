package com.heartbound.menu;

import com.heartbound.interaction.InteractionHandler;
import com.heartbound.relationship.Gender;
import com.heartbound.relationship.RelationshipData;
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
    public static final int BUTTON_PET = 1;

    private static final int DATA_AFFINITY = 0;
    private static final int DATA_GENDER = 1;
    private static final int DATA_ENTITY_LOW = 2;
    private static final int DATA_ENTITY_HIGH = 3;
    private static final int DATA_COUNT = 4;

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
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_AFFINITY -> server == null ? 0 : RelationshipData.get(server).get(mobId, playerId);
                    case DATA_GENDER -> gender;
                    case DATA_ENTITY_LOW -> entityId & 0xFFFF;
                    case DATA_ENTITY_HIGH -> entityId >>> 16;
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

    public int getAffinity() {
        return data.get(DATA_AFFINITY);
    }

    public Gender getGender() {
        Gender[] values = Gender.values();
        int index = data.get(DATA_GENDER);
        return values[Math.max(0, Math.min(values.length - 1, index))];
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
            case BUTTON_PET:
                InteractionHandler.pet(serverPlayer, target);
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
