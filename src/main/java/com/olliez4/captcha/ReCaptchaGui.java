package com.olliez4.captcha;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class ReCaptchaGui implements Listener {

    /**
     * An instance of the main class
     */
    JavaPlugin plugin;
    /**
     * An instance of the random class
     */
    Random random;

    /**
     * A store of the verified players. This will contain players who have verified recently
     */
    ArrayList<Player> verified = new ArrayList<>();

    /**
     * The title of the GUI
     */
    String title = ChatColor.BLUE.toString() + ChatColor.BOLD + "Please select ";

    /**
     * The amount of times each user has passed a captcha
     */
    HashMap<Player, Integer> amountPassed = new HashMap<>();

    /**
     * The colours to display within the GUI
     */
    ArrayList<Material> colours = new ArrayList<>();

    // TODO Figure out how to describe the plugin parameter

    /**
     * Instantiate the class
     *
     * @param plugin
     */
    public ReCaptchaGui(JavaPlugin plugin) {
        // Initialise the classes
        this.plugin = plugin;
        random = new Random();
        // If there is no colours in the list, add them
        if (colours.isEmpty()) {
            colours.add(Material.WHITE_STAINED_GLASS_PANE);
            colours.add(Material.ORANGE_STAINED_GLASS_PANE);
            colours.add(Material.MAGENTA_STAINED_GLASS_PANE);
            colours.add(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
            colours.add(Material.YELLOW_STAINED_GLASS_PANE);
            colours.add(Material.LIME_STAINED_GLASS_PANE);
            colours.add(Material.PINK_STAINED_GLASS_PANE);
            colours.add(Material.CYAN_STAINED_GLASS_PANE);
            colours.add(Material.PURPLE_STAINED_GLASS_PANE);
            colours.add(Material.BLUE_STAINED_GLASS_PANE);
            colours.add(Material.BROWN_STAINED_GLASS_PANE);
            colours.add(Material.GREEN_STAINED_GLASS_PANE);
            colours.add(Material.RED_STAINED_GLASS_PANE);
        }
    }

    /**
     * Method to verify a player
     *
     * @param player The player to verify
     */
    public void verifyPlayer(Player player) {
        // Add one to the amount of times a player has passed the captcha
        amountPassed.put(player, amountPassed.get(player) + 1);
        // If the user hasn't passed the right amount of times, present the GUI again
        if (amountPassed.get(player) < plugin.getConfig().getInt("captcha-times")) {
            send(player);
        } else {
            // Remove them from the amount of times they have passed so we clear up memory
            amountPassed.remove(player);
            // Close the GUI
            player.closeInventory();
            // Send the message to them to say they have passed the captcha
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("pass-message"))));
            // Get the list of verified players from the config.yml
            List<String> stringList = plugin.getConfig().getStringList("Verified-Players");
            // Add the player to the list
            stringList.add(player.getUniqueId().toString());
            // Save the list back to the config.yml
            plugin.getConfig().set("Verified-Players", stringList);
            // Save the config.yml
            plugin.saveConfig();
            // Add the player to the list of verified users
            verified.add(player);
            // Alert staff they pass
            alertOp(player, player.getName() + " passed the Captcha", true);
            // Send the join message
            Bukkit.broadcastMessage(ReCaptchaPlugin.format.replaceAll("NAME", player.getName()));
            // Reset their walk speed
            for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                player.removePotionEffect(potionEffect.getType());
            }
            // Remove their god mode
            player.setInvulnerable(false);
        }
    }

    /**
     * Send the inventory to the player
     *
     * @param player The player to whom to send the inventory
     */
    public void send(Player player) {
        // If the player has not already attempted the captcha, add them to the list
        // with
        // 0 attempts
        if (!amountPassed.containsKey(player)) {
            amountPassed.put(player, 0);
        }
        // Get a random colour for the captcha
        int index = random.nextInt(colours.size());
        Material material = colours.get(index);
        // Strip the material down to a string (EG: Material.RED_STAINED_GLASS_PANE to
        // "red")
        String name = material.toString().replaceAll("_STAINED_GLASS_PANE", "").replaceAll("_", " ").toLowerCase();
        // Create the inventory with 4 rows (36 slots) and format the title
        Inventory inventory = addBorder(Bukkit.getServer().createInventory(null, 36, title + name));
        // Add random colours to the GUI from the colours list
        for (int item = 0; item <= 13; item++) {
            int toAdd = random.nextInt(colours.size());
            ItemStack itemStack = new ItemStack(colours.get(toAdd));
            ItemMeta itemMeta = itemStack.getItemMeta();
            // This is to prevent items stacking (EG two types of the same glass)
            assert itemMeta != null;
            itemMeta.setCustomModelData(item);
            itemMeta.setDisplayName(formatItemName(itemStack.getType()));
            itemStack.setItemMeta(itemMeta);
            inventory.addItem(itemStack);
        }
        // Pick a random integer (0-6)
        int slot = random.nextInt(6);
        // The slot relative to the inventory which we will add the new item
        int realSlot;
        if (random.nextBoolean()) {
            // Top row selected
            realSlot = 10 + slot;
        } else {
            // Bottom row selected
            realSlot = 19 + slot;
        }
        // Format the correct item
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(formatItemName(material));
        itemStack.setItemMeta(itemMeta);

        // Put the item in the inventory to ensure it is always solvable
        inventory.setItem(realSlot, itemStack);
        // Open the inventory
        player.openInventory(inventory);
    }

    /**
     * Add a border to the inventory (Around the top row, bottom row and sides)
     *
     * @param inventory The inventory to which to add a border
     * @return The inventory with an added border
     */
    private Inventory addBorder(Inventory inventory) {
        for (int i = 0; i <= 9; i++) {
            inventory.setItem(i, emptyGlass());
        }
        inventory.setItem(17, emptyGlass());
        inventory.setItem(18, emptyGlass());
        for (int i = 26; i <= 35; i++) {
            inventory.setItem(i, emptyGlass());
        }
        return inventory;
    }

    /**
     * Ensure it runs as LOWEST so that it is not overridden by another plugin so they can not talk
     *
     * @param asyncPlayerChatEvent An {@link AsyncPlayerChatEvent}
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void chat(AsyncPlayerChatEvent asyncPlayerChatEvent) {
        // If the player is not verified either in memory or the config.yml
        if (!isPlayerVerified(asyncPlayerChatEvent.getPlayer())) {
            if (!verified.contains(asyncPlayerChatEvent.getPlayer())) {
                // Warn the player
                asyncPlayerChatEvent.getPlayer().sendMessage(
                        ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("can-not-talk"))));
                // Cancel the message
                asyncPlayerChatEvent.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent inventoryCloseEvent) {
        Player player = (Player) inventoryCloseEvent.getPlayer();
        // Add a scheduler so that the GUI does not glitch
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // If the player is not verified at all
            if (!verified.contains(player)) {
                if (!isPlayerVerified(player)) {
                    try {
                        // If the dismissed inventory is the captcha inventory, the player
                        if (player.getOpenInventory() != null) {
                            if (!player.getOpenInventory().getTitle().contains(title)) {
                                // Prevent double logging
                                send(player);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }, 2);
    }

    /**
     * Check whether a player is verified or not
     *
     * @param player The player of whom to check verification
     * @return Whether the player is verified
     */
    public boolean isPlayerVerified(Player player) {
        return plugin.getConfig().getStringList("Verified-Players").contains(player.getUniqueId().toString());
    }

    /**
     * Listen for click events
     *
     * @param inventoryClickEvent An {@link InventoryClickEvent}
     */
    @EventHandler
    public void click(InventoryClickEvent inventoryClickEvent) {
        try {
            // Get the item they click
            ItemStack currentItem = inventoryClickEvent.getCurrentItem();
            // Get their view of the inventory
            InventoryView inventoryView = inventoryClickEvent.getView();
            // Proceed if the inventory title is correct
            if (inventoryView.getTitle().contains(title)) {
                // Strip down the title to a material and compare to the clicked item's material
                assert currentItem != null;
                if (currentItem.getType().equals(
                        Material.getMaterial(inventoryView.getTitle().replaceAll(title, "").replaceAll(" ", "_").toUpperCase()
                                + "_STAINED_GLASS_PANE"))) {
                    // Verify the player as they have clicked the correct item
                    verifyPlayer((Player) inventoryClickEvent.getWhoClicked());
                } else {
                    // Kick the player, they have failed the captcha
                    Player whoClicked = (Player) inventoryClickEvent.getWhoClicked();
                    whoClicked.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                            Objects.requireNonNull(plugin.getConfig().getString("captcha-failed-message")).replaceAll("%amount%",
                                    "" + plugin.getConfig().getInt("Failure-Ban-Times"))));
                    // Alert staff
                    alertOp(whoClicked, whoClicked.getName() + " failed the Captcha", false);
                    // Log that they have failed with a reason
                    new FailureLogger(plugin, whoClicked, "Failed Captcha");
                }
                // Cancel the click so they can't take or drop items
                inventoryClickEvent.setCancelled(true);
            }
        }
        // If they click outside the GUI, suppress the error
        catch (Exception ignored) {
        }
    }

    /**
     * The border glass for the GUI, this has no name
     *
     * @return A border glass item
     */
    private ItemStack emptyGlass() {
        ItemStack itemStack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName("");
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void alertOp(Player player, String reason, boolean passed) {
        if (!passed) {
            if (!reason.equals("")) {
                for (Player op : Bukkit.getOnlinePlayers()) {
                    if (op.isOp() || op.hasPermission("captcha.viewalert"))
                        op.sendMessage(ChatColor.RED + player.getName() + " has failed the captcha for " + reason);
                }
            } else {
                for (Player op : Bukkit.getOnlinePlayers()) {
                    if (op.isOp() || op.hasPermission("captcha.viewalert"))
                        op.sendMessage(ChatColor.RED + player.getName() + " has failed the captcha");
                }
            }
        } else {
            for (Player op : Bukkit.getOnlinePlayers()) {
                if (op.isOp() || op.hasPermission("captcha.viewalert"))
                    op.sendMessage(ChatColor.GREEN + player.getName() + " has passed the captcha");
            }
        }
    }

    /**
     * Format a material down into a coloured, easily readable string
     *
     * @param material The material to format
     * @return The formatted string from the material
     */
    private String formatItemName(Material material) {
        String name = material.toString().replaceAll("_STAINED_GLASS_PANE", "");
        StringBuilder formattedName = new StringBuilder();
        for (String string : name.split("_")) {
            formattedName.append(string.substring(0, 1).toUpperCase()).append(string.substring(1).toLowerCase()).append(" ");
        }
        return applyColour(formattedName.toString(), material);
    }

    /**
     * Add a colour to a string based on its material
     *
     * @param string   The string to colour
     * @param material The material of the string
     * @return The coloured string
     */
    private String applyColour(String string, Material material) {
        if (material.equals(Material.WHITE_STAINED_GLASS_PANE)) {
            string = ChatColor.WHITE + string;
        } else if (material.equals(Material.ORANGE_STAINED_GLASS_PANE)) {
            string = ChatColor.GOLD + string;
        } else if (material.equals(Material.MAGENTA_STAINED_GLASS_PANE)) {
            string = ChatColor.DARK_PURPLE + string;
        } else if (material.equals(Material.LIGHT_BLUE_STAINED_GLASS_PANE)) {
            string = ChatColor.AQUA + string;
        } else if (material.equals(Material.YELLOW_STAINED_GLASS_PANE)) {
            string = ChatColor.YELLOW + string;
        } else if (material.equals(Material.LIME_STAINED_GLASS_PANE)) {
            string = ChatColor.GREEN + string;
        } else if (material.equals(Material.PINK_STAINED_GLASS_PANE)) {
            string = ChatColor.LIGHT_PURPLE + string;
        } else if (material.equals(Material.CYAN_STAINED_GLASS_PANE)) {
            string = ChatColor.DARK_AQUA + string;
        } else if (material.equals(Material.PURPLE_STAINED_GLASS_PANE)) {
            string = ChatColor.DARK_PURPLE + string;
        } else if (material.equals(Material.BLUE_STAINED_GLASS_PANE)) {
            string = ChatColor.DARK_BLUE + string;
        } else if (material.equals(Material.BROWN_STAINED_GLASS_PANE)) {
            string = ChatColor.GRAY + string;
        } else if (material.equals(Material.GREEN_STAINED_GLASS_PANE)) {
            string = ChatColor.DARK_GREEN + string;
        } else if (material.equals(Material.RED_STAINED_GLASS_PANE)) {
            string = ChatColor.DARK_RED + string;
        }
        return string;
    }
}
