package com.heartbound.menu;

import com.heartbound.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Menu types of the mod. Created here (common) and registered by each loader module. */
public final class ModMenus {

    public static final String RELATIONSHIP_NAME = "relationship";

    public static final MenuType<RelationshipMenu> RELATIONSHIP =
            new MenuType<>(RelationshipMenu::new, FeatureFlags.DEFAULT_FLAGS);

    private ModMenus() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
