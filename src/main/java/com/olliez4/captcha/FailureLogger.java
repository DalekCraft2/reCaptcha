package com.olliez4.captcha;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FailureLogger {

    public FailureLogger(JavaPlugin plugin, Player player, String reason) {
        // Only log if the logger is enabled
        if (plugin.getConfig().getBoolean("Use-Logging")) {
            // Format the text
            String text = reason + " - " + player.getUniqueId() + " - " + getTime();
            // Get their previous failures
            List<String> failures = plugin.getConfig().getStringList("Failed-Captcha-Attempts." + player.getName());
            // Only ban players if the module is enabled
            if (plugin.getConfig().getBoolean("Ban-after-too-many-tries")) {
                if (failures.size() > 0) {
                    // Manage the commands ran to ban people if their failure count is too high
                    if ((failures.size() % plugin.getConfig().getInt("Failure-Ban-Times")) == 0) {
                        // Ban the user, not just kick them
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), plugin.getConfig().getString("Ban-Command").replaceAll("%player%", player.getName()));
                        // Add the ban to the log
                        text = text + " - Ban issued (exceeded maximum failure count)";
                    }
                }
            }
            // Add the new failure to the previous failures
            failures.add(text);
            // Save the Captcha failure
            plugin.getConfig().set("Failed-Captcha-Attempts." + player.getName(), failures);
            // Save the config.yml
            plugin.saveConfig();
        }
    }

    /**
     * Get the amount of times a player has failed the Captcha
     *
     * @param plugin
     * @param player The player of whom to check the failures
     * @return The amount of times the player has failed the Captcha
     */
    public static int getFailures(JavaPlugin plugin, Player player) {
        // The placeholder
        int times = 0;
        // Iterate through all the times they have failed and add to times every pass
        for (String string : plugin.getConfig().getStringList("Failed-Captcha-Attempts." + player.getName())) {
            if (string.contains(player.getUniqueId().toString())) {
                times++;
            }
        }
        return times;
    }

    /**
     * Get the time as a formatted date
     *
     * @return The time as a formatted date
     */
    private String getTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return ("[" + simpleDateFormat.format(new Date()) + "]");
    }
}
