package de.themoep.itemuselimiter;

/*
 * ItemUseLimiter
 * Copyright (c) 2020 Max Lee aka Phoenix616 (max@themoep.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class ItemUseLimiter extends JavaPlugin implements Listener {

    private Map<Material, Integer> itemIdLimits = new EnumMap<>(Material.class);
    private Set<String> playerList = new HashSet<>();

    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    public void loadConfig() {

        ConfigurationSection s = getConfig().getConfigurationSection("items");
        for (String item : s.getKeys(false)) {
            int time = getConfig().getInt("items." + item, -1);
            if (time >= 0) {
                try {
                    Material id = Material.valueOf(item.toUpperCase());
                    itemIdLimits.put(id, time);
                    getLogger().log(Level.INFO, "Added cooldown for item " + item + ": " + time + "s");
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE, "Wrong item config " + item + " (not a valid material)");
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ItemUseLimiter.admin")) return true;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + " -------- " + ChatColor.YELLOW + "ItemUseLimiter" + ChatColor.GREEN + " ---------- ");
            sender.sendMessage(ChatColor.GREEN + "Cooldown of items:");
            for (Material id : itemIdLimits.keySet()) {
                sender.sendMessage(ChatColor.GREEN + " " + id + ": " + itemIdLimits.get(id) + "s");
            }
            sender.sendMessage(ChatColor.GREEN + "Use " + ChatColor.YELLOW + "/setitemuselimit <id|name> <time in s>" + ChatColor.GREEN + " to set item cooldowns!");
            return true;
        } else if (args.length == 1) {
            sender.sendMessage(ChatColor.GREEN + "Use " + ChatColor.YELLOW + "/setitemuselimit <id|name> <time in s>" + ChatColor.GREEN + " to set item cooldowns!");
            return true;
        } else if (args.length == 2) {
            try {
                int time = Integer.parseInt(args[1]);
                try {
                    Material id = Material.valueOf(args[0].toUpperCase());
                    if (time > 0) {
                        itemIdLimits.put(id, time);
                        getConfig().set("items." + id, time);
                        sender.sendMessage(ChatColor.GREEN + "Set cooldown for item " + args[0].toLowerCase() + " to " + time + "s!");
                    } else {
                        itemIdLimits.remove(id);
                        getConfig().set("items." + id, null);
                        sender.sendMessage(ChatColor.GREEN + "Removed cooldown for item " + args[0].toLowerCase() + "!");
                    }
                    saveConfig();
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Wrong item material " + args[0]);
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.DARK_RED + "Error:" + ChatColor.RED + " The time argument has to be a valid integer!");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onItemRightClick(final PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) || !event.hasItem() || event.getPlayer().hasPermission("itemuselimiter.bypass"))
            return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            Material cbt = event.getClickedBlock().getType();
            if (cbt == Material.CHEST
                    || Tag.BEDS.isTagged(cbt)
                    || Tag.DOORS.isTagged(cbt)
                    || Tag.TRAPDOORS.isTagged(cbt)
                    || Tag.BUTTONS.isTagged(cbt)
                    || Tag.FENCE_GATES.isTagged(cbt)
                    || cbt == Material.FURNACE
                    || cbt == Material.TRAPPED_CHEST
                    || cbt == Material.ANVIL
                    || cbt == Material.DROPPER
                    || cbt == Material.DISPENSER
                    || cbt == Material.HOPPER
                    || cbt == Material.LEVER
                    || cbt == Material.BEACON
                    || cbt == Material.ITEM_FRAME
                    || cbt == Material.BREWING_STAND
                    || cbt == Material.ENDER_CHEST) return;
        }
        Material itemMat = event.getMaterial();
        Player p = event.getPlayer();
        final UUID pid = p.getUniqueId();
        final Material itemid = event.getItem().getType();
        if (playerList.contains(p.getUniqueId().toString() + itemid)) {
            event.setCancelled(true);
            p.updateInventory();
            p.playSound(p.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                Player p1 = Bukkit.getPlayer(pid);
                if (p1 != null && p1.isOnline())
                    p1.playSound(p1.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
            }, 10L);
        } else {
            if (itemMat == Material.CARROTS ||
                    itemMat == Material.POTATOES ||
                    itemMat == Material.POISONOUS_POTATO ||
                    itemMat == Material.BAKED_POTATO ||
                    itemMat == Material.PORKCHOP ||
                    itemMat == Material.COOKED_PORKCHOP ||
                    itemMat == Material.BEACON ||
                    itemMat == Material.BEEF ||
                    itemMat == Material.CHICKEN ||
                    itemMat == Material.COD ||
                    itemMat == Material.COOKED_BEEF ||
                    itemMat == Material.COOKED_CHICKEN ||
                    itemMat == Material.COOKED_COD ||
                    itemMat == Material.BREAD ||
                    itemMat == Material.MUSHROOM_STEW ||
                    itemMat == Material.COOKIE ||
                    itemMat == Material.PUMPKIN_PIE ||
                    itemMat == Material.ROTTEN_FLESH ||
                    itemMat == Material.GOLDEN_APPLE ||
                    itemMat == Material.GOLDEN_CARROT ||
                    itemMat == Material.APPLE ||
                    itemMat == Material.MELON ||
                    itemMat == Material.SPIDER_EYE ||
                    itemMat == Material.MILK_BUCKET ||
                    itemMat == Material.POTION)
                return;
            if (itemIdLimits.containsKey(itemid)) {
                int time = itemIdLimits.get(itemid);
                playerList.add(p.getUniqueId().toString() + itemid);
                getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                    if (playerList.remove(pid.toString() + itemid)) {
                        Player p13 = getServer().getPlayer(pid);
                        if (p13 != null && p13.isOnline())
                            p13.playSound(p13.getEyeLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                    }
                }, 20L * time);
            }
        }
    }

    @EventHandler
    public void onItemConsume(final PlayerItemConsumeEvent event) {
        if (event.getPlayer().hasPermission("itemuselimiter.bypass")) return;
        Player p = event.getPlayer();
        final UUID pid = p.getUniqueId();
        final String itemname = event.getItem().getType().toString().toLowerCase();
        final Material material = event.getItem().getType();

        if (playerList.contains(p.getUniqueId().toString() + itemname) || playerList.contains(p.getUniqueId().toString() + material)) {
            event.setCancelled(true);
            p.updateInventory();
            p.playSound(p.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                Player p13 = getServer().getPlayer(pid);
                if (p13 != null && p13.isOnline())
                    p13.playSound(p13.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
            }, 10L);
        } else if (itemIdLimits.containsKey(material)) {
            int time = itemIdLimits.get(material);
            playerList.add(p.getUniqueId().toString() + material);
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
                if (playerList.remove(pid.toString() + material)) {
                    Player p1 = Bukkit.getPlayer(pid);
                    if (p1 != null && p1.isOnline())
                        p1.playSound(p1.getEyeLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                }
            }, 20L * time);
        }
    }
}

