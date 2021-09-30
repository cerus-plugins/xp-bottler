package dev.cerus.xpbottler;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class XpBottlerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Save default config
        this.saveDefaultConfig();
        final FileConfiguration config = this.getConfig();

        // Get variables from config
        final int cost = config.getInt("cost", 9);
        final String permission = config.getString("permission", "xpbottler.use");
        final Material blockType = Material.getMaterial(config.getString("block-type", Material.EMERALD_BLOCK.name()));
        final Sound sound = Sound.valueOf(config.getString("sound", Sound.ITEM_BOTTLE_FILL.name()));

        // Exit if block type is unknown / invalid
        if (blockType == null) {
            this.getLogger().severe("Error: Unknown block type!");
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        // Register interact listener
        this.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onInteract(final PlayerInteractEvent event) {
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                    return;
                }
                if (event.getClickedBlock().getType() != blockType) {
                    return;
                }
                if (event.getHand() != EquipmentSlot.HAND) {
                    return;
                }

                // Return if player lacks permission
                final Player player = event.getPlayer();
                if (!player.hasPermission(permission)) {
                    return;
                }

                // Return if item in hand is not a glass bottle
                final ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() != Material.GLASS_BOTTLE) {
                    return;
                }

                // Return if player does not have enough xp points
                if (XpBottlerPlugin.this.getPlayerExp(player) < (player.isSneaking() ? cost * item.getAmount() : cost)) {
                    player.sendMessage("§cYou need at least " + (player.isSneaking() ? cost * item.getAmount() : cost)
                            + " experience points to do this!");
                    return;
                }

                // Return if players inventory is full
                final ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
                bottle.setAmount(player.isSneaking() ? item.getAmount() : 1);
                if (!player.getInventory().addItem(bottle).isEmpty()) {
                    player.sendMessage("§cYour inventory is full!");
                    return;
                }

                // Update players xp
                XpBottlerPlugin.this.changePlayerExp(player, -(player.isSneaking() ? cost * item.getAmount() : cost));

                // Decrement glass bottle item amount
                if (player.isSneaking()) {
                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                } else {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        player.getInventory().clear(player.getInventory().getHeldItemSlot());
                    }
                }

                player.playSound(player.getLocation(), sound, 1, 1);

                event.setCancelled(true);
            }
        }, this);
    }

    // Calculate amount of EXP needed to level up
    private int getExpToLevelUp(final int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    // Calculate total experience up to a level
    private int getExpAtLevel(final int level) {
        if (level <= 16) {
            return (int) (Math.pow(level, 2) + 6 * level);
        } else if (level <= 31) {
            return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360.0);
        } else {
            return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220.0);
        }
    }

    // Calculate player's current EXP amount
    private int getPlayerExp(final Player player) {
        int exp = 0;
        final int level = player.getLevel();

        // Get the amount of XP in past levels
        exp += this.getExpAtLevel(level);

        // Get amount of XP towards next level
        exp += Math.round(this.getExpToLevelUp(level) * player.getExp());

        return exp;
    }

    // Give or take EXP
    private int changePlayerExp(final Player player, final int exp) {
        // Get player's current exp
        final int currentExp = this.getPlayerExp(player);

        // Reset player's current exp to 0
        player.setExp(0);
        player.setLevel(0);

        // Give the player their exp back, with the difference
        final int newExp = currentExp + exp;
        player.giveExp(newExp);

        // Return the player's new exp amount
        return newExp;
    }

}