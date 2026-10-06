package com.heartbound.menu;

import com.heartbound.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

/**
 * Menu types of the mod. The MenuType itself is created by each loader module (vanilla hides the
 * constructor), handed to {@link #init} before registration, and registered by the loader.
 */
public final class ModMenus {

    public static final String RELATIONSHIP_NAME = "relationship";

    public static MenuType<RelationshipMenu> RELATIONSHIP;

    private ModMenus() {
    }

    public static void init(MenuType<RelationshipMenu> relationship) {
        RELATIONSHIP = relationship;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
