package com.gmail.nossr50.util.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.AdvancedConfig;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.config.HiddenConfig;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.AbilityType;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.datatypes.skills.ToolType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import com.gmail.nossr50.events.fake.FakeBlockBreakEvent;
import com.gmail.nossr50.events.fake.FakeBlockDamageEvent;
import com.gmail.nossr50.events.fake.FakePlayerAnimationEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.runnables.skills.AbilityDisableTask;
import com.gmail.nossr50.runnables.skills.ToolLowerTask;
import com.gmail.nossr50.util.ItemUtils;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.ModUtils;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.StringUtils;
import com.gmail.nossr50.util.player.UserManager;

public class SkillUtils {
    public static int handleFoodSkills(Player player, SkillType skill, int eventFoodLevel, int baseLevel, int maxLevel, int rankChange) {
        int skillLevel = UserManager.getPlayer(player).getProfile().getSkillLevel(skill);

        int currentFoodLevel = player.getFoodLevel();
        int foodChange = eventFoodLevel - currentFoodLevel;

        for (int i = baseLevel; i <= maxLevel; i += rankChange) {
            if (skillLevel >= i) {
                foodChange++;
            }
        }

        return currentFoodLevel + foodChange;
    }

    /**
     * Calculate the time remaining until the cooldown expires.
     *
     * @param deactivatedTimeStamp Time of deactivation
     * @param cooldown The length of the cooldown
     * @param player The Player to check for cooldown perks
     *
     * @return the number of seconds remaining before the cooldown expires
     */
    public static int calculateTimeLeft(long deactivatedTimeStamp, int cooldown, Player player) {
        return (int) (((deactivatedTimeStamp + (PerksUtils.handleCooldownPerks(player, cooldown) * Misc.TIME_CONVERSION_FACTOR)) - System.currentTimeMillis()) / Misc.TIME_CONVERSION_FACTOR);
    }

    /**
     * Check if the cooldown has expired.
     * This does NOT account for cooldown perks!
     *
     * @param deactivatedTimeStamp Time of deactivation in seconds
     * @param cooldown The length of the cooldown in seconds
     *
     * @return true if the cooldown is expired
     */
    public static boolean cooldownExpired(long deactivatedTimeStamp, int cooldown) {
        return (System.currentTimeMillis() >= (deactivatedTimeStamp + cooldown) * Misc.TIME_CONVERSION_FACTOR);
    }

    /**
     * Process activating abilities & readying the tool.
     *
     * @param player The player using the ability
     * @param skill The skill the ability is tied to
     */
    public static void activationCheck(Player player, SkillType skill) {
        if (Config.getInstance().getAbilitiesOnlyActivateWhenSneaking() && !player.isSneaking()) {
            return;
        }

        ItemStack inHand = player.getItemInHand();

        if (ModUtils.isCustomTool(inHand) && !ModUtils.getToolFromItemStack(inHand).isAbilityEnabled()) {
            return;
        }

        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);

        if (!mcMMOPlayer.getAbilityUse()) {
            return;
        }

        for (AbilityType abilityType : AbilityType.values()) {
            if (mcMMOPlayer.getAbilityMode(abilityType)) {
                return;
            }
        }

        PlayerProfile playerProfile = mcMMOPlayer.getProfile();
        AbilityType ability = skill.getAbility();
        ToolType tool = skill.getTool();

        /*
         * Woodcutting & Axes need to be treated differently.
         * Basically the tool always needs to ready and we check to see if the cooldown is over when the user takes action
         */
        if (ability.getPermissions(player) && tool.inHand(inHand) && !mcMMOPlayer.getToolPreparationMode(tool)) {
            if (skill != SkillType.WOODCUTTING && skill != SkillType.AXES) {
                int timeRemaining = calculateTimeLeft(playerProfile.getSkillDATS(ability) * Misc.TIME_CONVERSION_FACTOR, ability.getCooldown(), player);

                if (!mcMMOPlayer.getAbilityMode(ability) && timeRemaining > 0) {
                    player.sendMessage(LocaleLoader.getString("Skills.TooTired", timeRemaining));
                    return;
                }
            }

            if (Config.getInstance().getAbilityMessagesEnabled()) {
                player.sendMessage(tool.getRaiseTool());
            }

            mcMMOPlayer.setToolPreparationATS(tool, System.currentTimeMillis());
            mcMMOPlayer.setToolPreparationMode(tool, true);
            new ToolLowerTask(mcMMOPlayer, tool).runTaskLaterAsynchronously(mcMMO.p, 4 * Misc.TICK_CONVERSION_FACTOR);
        }
    }

    /**
     * Check the XP of a skill.
     *
     * @param skillType The skill to check
     * @param player The player whose skill to check
     * @param profile The profile of the player whose skill to check
     */
    public static void xpCheckSkill(SkillType skillType, Player player, PlayerProfile profile) {
        int levelsGained = 0;
        float xpRemoved = 0;

        if (profile.getSkillXpLevelRaw(skillType) >= profile.getXpToLevel(skillType)) {
            McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);

            while (profile.getSkillXpLevelRaw(skillType) >= profile.getXpToLevel(skillType)) {
                if ((skillType.getMaxLevel() >= profile.getSkillLevel(skillType) + 1) && (Config.getInstance().getPowerLevelCap() >= mcMMOPlayer.getPowerLevel() + 1)) {
                    int xp = profile.getXpToLevel(skillType);
                    xpRemoved += xp;

                    profile.removeXp(skillType, xp);
                    levelsGained++;
                    profile.skillUp(skillType, 1);
                }
                else {
                    profile.addLevels(skillType, 0);
                }
            }

            McMMOPlayerLevelUpEvent eventToFire = new McMMOPlayerLevelUpEvent(player, skillType, levelsGained);
            mcMMO.p.getServer().getPluginManager().callEvent(eventToFire);

            if (eventToFire.isCancelled()) {
                profile.modifySkill(skillType, profile.getSkillLevel(skillType) - levelsGained);
                profile.setSkillXpLevel(skillType, profile.getSkillXpLevelRaw(skillType) + xpRemoved);
                return;
            }

            String capitalized = StringUtils.getCapitalized(skillType.toString());

            player.playSound(player.getLocation(), Sound.LEVEL_UP, Misc.LEVELUP_VOLUME, Misc.LEVELUP_PITCH);
            player.sendMessage(LocaleLoader.getString(capitalized + ".Skillup", levelsGained, profile.getSkillLevel(skillType)));
        }
    }

    /**
     * Checks if the given string represents a valid skill
     *
     * @param skillName The name of the skill to check
     * @return true if this is a valid skill, false otherwise
     */
    public static boolean isSkill(String skillName) {
        if (!Config.getInstance().getLocale().equalsIgnoreCase("en_US")) {
            return isLocalizedSkill(skillName);
        }

        return SkillType.getSkill(skillName) != null;
    }

    private static boolean isLocalizedSkill(String skillName) {
        for (SkillType skill : SkillType.values()) {
            if (skillName.equalsIgnoreCase(LocaleLoader.getString(StringUtils.getCapitalized(skill.toString()) + ".SkillName"))) {
                return true;
            }
        }

        return false;
    }

    public static String getSkillName(SkillType skill) {
        if (!Config.getInstance().getLocale().equalsIgnoreCase("en_US")) {
            return StringUtils.getCapitalized(LocaleLoader.getString(StringUtils.getCapitalized(skill.toString()) + ".SkillName"));
        }

        return StringUtils.getCapitalized(skill.toString());
    }

    /**
     * Check if the player has any combat skill permissions.
     *
     * @param player The player to check permissions for
     * @return true if the player has combat skills, false otherwise
     */
    public static boolean hasCombatSkills(Player player) {
        return Permissions.skillEnabled(player, SkillType.AXES)
                || Permissions.skillEnabled(player, SkillType.ARCHERY)
                || Permissions.skillEnabled(player, SkillType.SWORDS)
                || Permissions.skillEnabled(player, SkillType.TAMING)
                || Permissions.skillEnabled(player, SkillType.UNARMED);
    }

    /**
     * Check if the player has any gathering skill permissions.
     *
     * @param player The player to check permissions for
     * @return true if the player has gathering skills, false otherwise
     */
    public static boolean hasGatheringSkills(Player player) {
        return Permissions.skillEnabled(player, SkillType.EXCAVATION)
                || Permissions.skillEnabled(player, SkillType.FISHING)
                || Permissions.skillEnabled(player, SkillType.HERBALISM)
                || Permissions.skillEnabled(player, SkillType.MINING)
                || Permissions.skillEnabled(player, SkillType.WOODCUTTING);
    }

    /**
     * Check if the player has any misc skill permissions.
     *
     * @param player The player to check permissions for
     * @return true if the player has misc skills, false otherwise
     */
    public static boolean hasMiscSkills(Player player) {
        return Permissions.skillEnabled(player, SkillType.ACROBATICS)
                || Permissions.skillEnabled(player, SkillType.SMELTING)
                || Permissions.skillEnabled(player, SkillType.REPAIR);
    }

    /**
     * Check to see if an ability can be activated.
     *
     * @param mcMMOPlayer The player activating the ability
     * @param type The skill the ability is based on
     */
    public static void abilityCheck(McMMOPlayer mcMMOPlayer, SkillType type) {
        ToolType tool = type.getTool();
        AbilityType ability = type.getAbility();

        mcMMOPlayer.setToolPreparationMode(tool, false);

        Player player = mcMMOPlayer.getPlayer();
        PlayerProfile playerProfile = mcMMOPlayer.getProfile();

        int timeRemaining = calculateTimeLeft(playerProfile.getSkillDATS(ability) * Misc.TIME_CONVERSION_FACTOR, ability.getCooldown(), player);

        /*
         * Axes and Woodcutting are odd because they share the same tool.
         * We show them the too tired message when they take action.
         */
        if (type == SkillType.WOODCUTTING || type == SkillType.AXES) {
            if (!mcMMOPlayer.getAbilityMode(ability) && timeRemaining > 0) {
                player.sendMessage(LocaleLoader.getString("Skills.TooTired", timeRemaining));
                return;
            }
        }

        if (!mcMMOPlayer.getAbilityMode(ability) && timeRemaining <= 0) {
            McMMOPlayerAbilityActivateEvent event = new McMMOPlayerAbilityActivateEvent(player, type);
            mcMMO.p.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            int ticks = PerksUtils.handleActivationPerks(player, 2 + (playerProfile.getSkillLevel(type) / AdvancedConfig.getInstance().getAbilityLength()), ability.getMaxLength());

            ParticleEffectUtils.playAbilityEnabledEffect(player);

            if (mcMMOPlayer.useChatNotifications()) {
                player.sendMessage(ability.getAbilityOn());
            }

            SkillUtils.sendSkillMessage(player, ability.getAbilityPlayer(player));

            playerProfile.setSkillDATS(ability, System.currentTimeMillis() + (ticks * Misc.TIME_CONVERSION_FACTOR));
            mcMMOPlayer.setAbilityMode(ability, true);

            if (ability == AbilityType.SUPER_BREAKER || ability == AbilityType.GIGA_DRILL_BREAKER) {
                handleAbilitySpeedIncrease(player);
            }

            new AbilityDisableTask(mcMMOPlayer, ability).runTaskLater(mcMMO.p, ticks * Misc.TICK_CONVERSION_FACTOR);
        }
    }

    /**
     * Check to see if ability should be triggered.
     *
     * @param player The player using the ability
     * @param block The block modified by the ability
     * @param ability The ability to check
     * @return true if the ability should activate, false otherwise
     */
    public static boolean triggerCheck(Player player, Block block, AbilityType ability) {
        boolean activate = true;

        switch (ability) {
            case BERSERK:
            case BLOCK_CRACKER:
            case LEAF_BLOWER:
                if (!ability.blockCheck(block.getState())) {
                    activate = false;
                    break;
                }

                if (!blockBreakSimulate(block, player, true)) {
                    activate = false;
                    break;
                }
                break;

            case GIGA_DRILL_BREAKER:
            case SUPER_BREAKER:
            case GREEN_TERRA:
                if (!ability.blockCheck(block.getState())) {
                    activate = false;
                    break;
                }
                break;

            default:
                activate = false;
                break;
        }

        return activate;
    }

    public static void sendSkillMessage(Player player, String message) {
        for (Player otherPlayer : player.getWorld().getPlayers()) {
            if (otherPlayer != player && Misc.isNear(player.getLocation(), otherPlayer.getLocation(), Misc.SKILL_MESSAGE_MAX_SENDING_DISTANCE)) {
                otherPlayer.sendMessage(message);
            }
        }
    }

    public static void handleAbilitySpeedIncrease(Player player) {
        if (HiddenConfig.getInstance().useEnchantmentBuffs()) {
            ItemStack heldItem = player.getItemInHand();

            if (heldItem == null || heldItem.getType() == Material.AIR) {
                return;
            }

            int efficiencyLevel = heldItem.getEnchantmentLevel(Enchantment.DIG_SPEED);
            ItemMeta itemMeta = heldItem.getItemMeta();
            List<String> itemLore = new ArrayList<String>();

            if (itemMeta.hasLore()) {
                itemLore = itemMeta.getLore();
            }

            itemLore.add("mcMMO Ability Tool");
            itemMeta.addEnchant(Enchantment.DIG_SPEED, efficiencyLevel + AdvancedConfig.getInstance().getEnchantBuff(), true);

            itemMeta.setLore(itemLore);
            heldItem.setItemMeta(itemMeta);
        }
        else {
            int duration = 0;
            int amplifier = 0;

            if (player.hasPotionEffect(PotionEffectType.FAST_DIGGING)) {
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    if (effect.getType() == PotionEffectType.FAST_DIGGING) {
                        duration = effect.getDuration();
                        amplifier = effect.getAmplifier();
                        break;
                    }
                }
            }

            McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);
            SkillType skill = mcMMOPlayer.getAbilityMode(AbilityType.SUPER_BREAKER) ? SkillType.MINING : SkillType.EXCAVATION;
            int ticks = PerksUtils.handleActivationPerks(player, 2 + (mcMMOPlayer.getProfile().getSkillLevel(skill) / AdvancedConfig.getInstance().getAbilityLength()), skill.getAbility().getMaxLength()) * Misc.TICK_CONVERSION_FACTOR;

            PotionEffect abilityBuff = new PotionEffect(PotionEffectType.FAST_DIGGING, duration + ticks, amplifier + 10);
            player.addPotionEffect(abilityBuff, true);
        }
    }

    public static void handleAbilitySpeedDecrease(Player player) {
        if (HiddenConfig.getInstance().useEnchantmentBuffs()) {
            PlayerInventory playerInventory = player.getInventory();

            for (int i = 0; i < playerInventory.getContents().length; i++) {
                ItemStack item = playerInventory.getItem(i);
                playerInventory.setItem(i, removeAbilityBuff(item));
            }
        }
    }

    public static ItemStack removeAbilityBuff(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        if (!ItemUtils.isPickaxe(item) && !ItemUtils.isShovel(item)) {
            return item;
        }

        if (item.containsEnchantment(Enchantment.DIG_SPEED)) {
            ItemMeta itemMeta = item.getItemMeta();

            if (itemMeta.hasLore()) {
                List<String> itemLore = itemMeta.getLore();

                if (itemLore.remove("mcMMO Ability Tool")) {
                    int efficiencyLevel = item.getEnchantmentLevel(Enchantment.DIG_SPEED);

                    if (efficiencyLevel <= AdvancedConfig.getInstance().getEnchantBuff()) {
                        itemMeta.removeEnchant(Enchantment.DIG_SPEED);
                    }
                    else {
                        itemMeta.addEnchant(Enchantment.DIG_SPEED, efficiencyLevel - AdvancedConfig.getInstance().getEnchantBuff(), true);
                    }

                    itemMeta.setLore(itemLore);
                    item.setItemMeta(itemMeta);
                }
            }
        }

        return item;
    }

    /**
     * Modify the durability of an ItemStack.
     *
     * @param itemStack The ItemStack which durability should be modified
     * @return the itemStack with modified durability
     */
    public static ItemStack handleDurabilityChange(ItemStack itemStack, int durabilityModifier) {
        short finalDurability = (short) (itemStack.getDurability() + durabilityModifier);
        short maxDurability = itemStack.getType().getMaxDurability();
        boolean overMax = (finalDurability >= maxDurability);

        itemStack.setDurability(overMax ? maxDurability : finalDurability);
        return itemStack;
    }

    /**
     * Simulate a block break event.
     *
     * @param block The block to break
     * @param player The player breaking the block
     * @param shouldArmSwing true if an armswing event should be fired, false otherwise
     * @return true if the event wasn't cancelled, false otherwise
     */
    public static boolean blockBreakSimulate(Block block, Player player, boolean shouldArmSwing) {
        PluginManager pluginManger = mcMMO.p.getServer().getPluginManager();

        // Support for NoCheat
        if (shouldArmSwing) {
            pluginManger.callEvent(new FakePlayerAnimationEvent(player));
        }

        FakeBlockDamageEvent damageEvent = new FakeBlockDamageEvent(player, block, player.getItemInHand(), true);
        pluginManger.callEvent(damageEvent);

        FakeBlockBreakEvent breakEvent = new FakeBlockBreakEvent(block, player);
        pluginManger.callEvent(breakEvent);

        return !damageEvent.isCancelled() && !breakEvent.isCancelled();
    }

    public static boolean activationSuccessful(Player player, SkillType skill, double maxChance, int maxLevel) {
        int skillLevel = UserManager.getPlayer(player).getProfile().getSkillLevel(skill);
        int activationChance = PerksUtils.handleLuckyPerks(player, skill);
        double chance = (maxChance / maxLevel) * Math.min(skillLevel, maxLevel);

        return chance > Misc.getRandom().nextInt(activationChance);
    }

    public static boolean activationSuccessful(int skillLevel, int activationChance, double maxChance, int maxLevel) {
        return ((maxChance / maxLevel) * Math.min(skillLevel, maxLevel)) > Misc.getRandom().nextInt(activationChance);
    }

    public static boolean treasureDropSuccessful(double dropChance, int activationChance) {
        return dropChance > Misc.getRandom().nextDouble() * activationChance;
    }
}
