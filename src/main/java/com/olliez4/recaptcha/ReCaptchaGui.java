package com.olliez4.recaptcha;

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
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class ReCaptchaGui implements Listener {

    /**
     * An instance of the {@link ReCaptchaPlugin}.
     */
    ReCaptchaPlugin plugin;
    /**
     * An instance of the {@link Random} class.
     */
    Random random;

    /**
     * A list of recently verified {@link Player}.
     */
    ArrayList<Player> verified = new ArrayList<>();

    /**
     * The title of the GUI.
     */
    String title = ChatColor.BLUE.toString() + ChatColor.BOLD + "Please select ";

    /**
     * The amount of times each {@link Player} has passed a Captcha.
     */
    HashMap<Player, Integer> amountPassed = new HashMap<>();

    /**
     * The colours to display within the GUI.
     */
    ArrayList<Material> colours = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param plugin the instance of the {@link ReCaptchaPlugin}
     */
    public ReCaptchaGui(ReCaptchaPlugin plugin) {
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
     * Verifies a {@link Player}.
     *
     * @param player the {@link Player} to verify
     */
    public void verifyPlayer(Player player) {
        // Add one to the amount of times a player has passed the Captcha
        amountPassed.put(player, amountPassed.get(player) + 1);
        // If the user hasn't passed the right amount of times, present the GUI again
        if (amountPassed.get(player) < plugin.getConfig().getInt("captcha-times")) {
            send(player);
        } else {
            // Remove them from the amount of times they have passed, so we clear up memory
            amountPassed.remove(player);
            // Close the GUI
            player.closeInventory();
            // Send the message to them to say they have passed the Captcha
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("pass-message")));
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
     * Sends the Captcha {@link Inventory} to a {@link Player}.
     *
     * @param player the {@link Player} to whom to send the {@link Inventory}
     */
    public void send(Player player) {
        // If the player has not already attempted the Captcha, add them to the list with 0 attempts
        if (!amountPassed.containsKey(player)) {
            amountPassed.put(player, 0);
        }
        // Get a random colour for the Captcha
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
        itemMeta.setDisplayName(formatItemName(material));
        itemStack.setItemMeta(itemMeta);

        // Put the item in the inventory to ensure it is always solvable
        inventory.setItem(realSlot, itemStack);
        // Open the inventory
        player.openInventory(inventory);
    }

    /**
     * Adds a border to an {@link Inventory} around the top row, bottom row, and sides.
     *
     * @param inventory the {@link Inventory} to which to add a border
     * @return the {@link Inventory} with an added border
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
     * Listens for {@link AsyncPlayerChatEvent AsyncPlayerChatEvents}.
     *
     * @param asyncPlayerChatEvent an {@link AsyncPlayerChatEvent}
     */
    // Ensure it runs as LOWEST so that it is not overridden by another plugin, so they can not talk
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent asyncPlayerChatEvent) {
        // If the player is not verified either in memory or the config.yml
        if (!isPlayerVerified(asyncPlayerChatEvent.getPlayer())) {
            if (!verified.contains(asyncPlayerChatEvent.getPlayer())) {
                // Warn the player
                asyncPlayerChatEvent.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("can-not-talk")));
                // Cancel the message
                asyncPlayerChatEvent.setCancelled(true);
            }
        }
    }

    /**
     * Listens for {@link InventoryCloseEvent InventoryCloseEvents}.
     *
     * @param inventoryCloseEvent an {@link InventoryCloseEvent}
     */
    @EventHandler
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        Player player = (Player) inventoryCloseEvent.getPlayer();
        // Add a scheduler so that the GUI does not glitch
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // If the player is not verified at all
            if (!verified.contains(player)) {
                if (!isPlayerVerified(player)) {
                    try {
                        // If the dismissed inventory is the Captcha inventory, the player
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
     * Checks whether a {@link Player} is verified.
     *
     * @param player the {@link Player} of whom to check verification
     * @return {@code true} if the {@link Player} is verified
     */
    public boolean isPlayerVerified(Player player) {
        return plugin.getConfig().getStringList("Verified-Players").contains(player.getUniqueId().toString());
    }

    /**
     * Listens for {@link InventoryClickEvent InventoryClickEvents}.
     *
     * @param inventoryClickEvent an {@link InventoryClickEvent}
     */
    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        try {
            // Get the item they click
            ItemStack currentItem = inventoryClickEvent.getCurrentItem();
            // Get their view of the inventory
            InventoryView inventoryView = inventoryClickEvent.getView();
            // Proceed if the inventory title is correct
            if (inventoryView.getTitle().contains(title)) {
                // Strip down the title to a material and compare to the clicked item's material
                if (currentItem.getType().equals(Material.getMaterial(inventoryView.getTitle().replaceAll(title, "").replaceAll(" ", "_").toUpperCase() + "_STAINED_GLASS_PANE"))) {
                    // Verify the player as they have clicked the correct item
                    verifyPlayer((Player) inventoryClickEvent.getWhoClicked());
                } else {
                    // Kick the player, they have failed the Captcha
                    Player whoClicked = (Player) inventoryClickEvent.getWhoClicked();
                    whoClicked.kickPlayer(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("captcha-failed-message").replaceAll("%amount%", "" + plugin.getConfig().getInt("Failure-Ban-Times"))));
                    // Alert staff
                    alertOp(whoClicked, whoClicked.getName() + " failed the Captcha", false);
                    // Log that they have failed with a reason
                    new FailureLogger(plugin, whoClicked, "Failed Captcha");
                }
                // Cancel the click, so they can't take or drop items
                inventoryClickEvent.setCancelled(true);
            }
        }
        // If they click outside the GUI, suppress the error
        catch (Exception ignored) {
        }
    }

    /**
     * The border glass for the GUI; this has no name.
     *
     * @return a border glass {@link ItemStack}
     */
    private ItemStack emptyGlass() {
        ItemStack itemStack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName("");
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void alertOp(Player player, String reason, boolean passed) {
        if (!passed) {
            if (!reason.equals("")) {
                for (Player op : Bukkit.getOnlinePlayers()) {
                    if (op.isOp() || op.hasPermission("captcha.viewalert")) {
                        op.sendMessage(ChatColor.RED + player.getName() + " has failed the Captcha for " + reason);
                    }
                }
            } else {
                for (Player op : Bukkit.getOnlinePlayers()) {
                    if (op.isOp() || op.hasPermission("captcha.viewalert")) {
                        op.sendMessage(ChatColor.RED + player.getName() + " has failed the Captcha");
                    }
                }
            }
        } else {
            for (Player op : Bukkit.getOnlinePlayers()) {
                if (op.isOp() || op.hasPermission("captcha.viewalert")) {
                    op.sendMessage(ChatColor.GREEN + player.getName() + " has passed the Captcha");
                }
            }
        }
    }

    /**
     * Formats a {@link Material} down into a coloured, easily readable {@link String}.
     *
     * @param material the {@link Material} to format
     * @return a formatted {@link String} from the {@link Material}
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
     * Adds a colour to a {@link String} based on its {@link Material}.
     *
     * @param string   the {@link String} to colour
     * @param material the {@link Material} of the {@link String}
     * @return the coloured {@link String}
     */
    private String applyColour(String string, Material material) {
        switch (material) {
            case WHITE_STAINED_GLASS_PANE -> string = ChatColor.WHITE + string;
            case ORANGE_STAINED_GLASS_PANE -> string = ChatColor.GOLD + string;
            case MAGENTA_STAINED_GLASS_PANE, PURPLE_STAINED_GLASS_PANE -> string = ChatColor.DARK_PURPLE + string;
            case LIGHT_BLUE_STAINED_GLASS_PANE -> string = ChatColor.AQUA + string;
            case YELLOW_STAINED_GLASS_PANE -> string = ChatColor.YELLOW + string;
            case LIME_STAINED_GLASS_PANE -> string = ChatColor.GREEN + string;
            case PINK_STAINED_GLASS_PANE -> string = ChatColor.LIGHT_PURPLE + string;
            case CYAN_STAINED_GLASS_PANE -> string = ChatColor.DARK_AQUA + string;
            case BLUE_STAINED_GLASS_PANE -> string = ChatColor.DARK_BLUE + string;
            case BROWN_STAINED_GLASS_PANE -> string = ChatColor.GRAY + string;
            case GREEN_STAINED_GLASS_PANE -> string = ChatColor.DARK_GREEN + string;
            case RED_STAINED_GLASS_PANE -> string = ChatColor.DARK_RED + string;
        }
        return string;
    }
}
