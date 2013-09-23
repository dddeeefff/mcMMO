package com.gmail.nossr50.util;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.config.mods.CustomArmorConfig;
import com.gmail.nossr50.config.mods.CustomBlockConfig;
import com.gmail.nossr50.config.mods.CustomEntityConfig;
import com.gmail.nossr50.config.mods.CustomToolConfig;
import com.gmail.nossr50.datatypes.mods.CustomBlock;
import com.gmail.nossr50.datatypes.mods.CustomEntity;
import com.gmail.nossr50.datatypes.mods.CustomTool;

public final class ModUtils {
    private static Config configInstance = Config.getInstance();

    private static boolean customToolsEnabled    = configInstance.getToolModsEnabled();
    private static boolean customArmorEnabled    = configInstance.getArmorModsEnabled();
    private static boolean customBlocksEnabled   = configInstance.getBlockModsEnabled();
    private static boolean customEntitiesEnabled = configInstance.getEntityModsEnabled();

    private ModUtils() {}

    /**
     * Get the custom tool associated with an item.
     *
     * @param item The item to check
     * @return the tool if it exists, null otherwise
     */
    public static CustomTool getToolFromItemStack(ItemStack item) {
        return CustomToolConfig.getInstance().customToolMap.get(item.getType());
    }

    /**
     * Get the custom block associated with an block.
     *
     * @param blockState The block to check
     * @return the block if it exists, null otherwise
     */
    public static CustomBlock getCustomBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customItems.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return block;
                    }
                }
            }
        }

        return null;
    }

    public static CustomEntity getCustomEntity(Entity entity) {
        if (!CustomEntityConfig.getInstance().customEntityIds.contains(entity.getEntityId()) && !CustomEntityConfig.getInstance().customEntityTypes.contains(entity.getType())) {
            return null;
        }

        for (CustomEntity customEntity : CustomEntityConfig.getInstance().customEntities) {
            if ((customEntity.getEntityID() == entity.getEntityId()) && (customEntity.getEntityType() == entity.getType())) {
                return customEntity;
            }
        }

        return null;
    }

    /**
     * Check if a custom block is a woodcutting block.
     *
     * @param blockState The BlockState of the block to check
     * @return true if the block represents a log, false otherwise
     */
    public static boolean isCustomWoodcuttingBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customWoodcuttingBlocks.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a custom block should not activate abilites.
     *
     * @param blockState The BlockState of the block to check
     * @return true if the block represents an ability block, false otherwise
     */
    public static boolean isCustomAbilityBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customAbilityBlocks.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a custom block is a mining block.
     *
     * @param blockState The BlockState of the block to check
     * @return true if the block is custom, false otherwise
     */
    public static boolean isCustomMiningBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customMiningBlocks.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a custom block is an excavation block.
     *
     * @param blockState The BlockState of the block to check
     * @return true if the block is custom, false otherwise
     */
    public static boolean isCustomExcavationBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customExcavationBlocks.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a custom block is an herbalism block.
     *
     * @param blockState The block to check
     * @return true if the block is custom, false otherwise
     */
    public static boolean isCustomHerbalismBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customHerbalismBlocks.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a custom block is a leaf block.
     *
     * @param blockState The BlockState of the block to check
     * @return true if the block represents leaves, false otherwise
     */
    public static boolean isCustomLeafBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customLeaves.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a custom block is a log block.
     *
     * @param blockState The BlockState of the block to check
     * @return true if the block represents a log, false otherwise
     */
    public static boolean isCustomLogBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customLogs.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a custom block is an ore block.
     *
     * @param blockState The block to check
     * @return true if the block represents an ore, false otherwise
     */
    public static boolean isCustomOreBlock(BlockState blockState) {
        if (customBlocksEnabled) {
            ItemStack item = blockState.getData().toItemStack(1);

            if (CustomBlockConfig.getInstance().customOres.contains(item)) {
                for (CustomBlock block : CustomBlockConfig.getInstance().customBlocks) {
                    if (new MaterialData(block.getItemID(), block.getDataValue()).equals(blockState.getData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks to see if an item is a custom tool.
     *
     * @param item Item to check
     * @return true if the item is a custom tool, false otherwise
     */
    public static boolean isCustomTool(ItemStack item) {
        if (customToolsEnabled && CustomToolConfig.getInstance().customTool.contains(item.getType())) {
            return true;
        }

        return false;
    }

    /**
     * Checks to see if an item is custom armor.
     *
     * @param item Item to check
     * @return true if the item is custom armor, false otherwise
     */
    public static boolean isCustomArmor(ItemStack item) {
        if (customArmorEnabled && CustomArmorConfig.getInstance().customArmor.contains(item.getType())) {
            return true;
        }

        return false;
    }

    public static boolean isCustomEntity(Entity entity) {
        if (customEntitiesEnabled && CustomEntityConfig.getInstance().customEntityIds.contains(entity.getEntityId())) {
            return true;
        }

        return false;
    }

    /**
     * Check if a custom entity is a boss.
     *
     * @param entity The entity to check
     * @return true if the entity represents a boss, false otherwise
     */
    public static boolean isCustomBossEntity(Entity entity) {
        //TODO: Finish this method
        return false;
    }
}
