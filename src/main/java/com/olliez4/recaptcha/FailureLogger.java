package com.olliez4.recaptcha;

import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FailureLogger {

    /**
     * Logs a {@link Player}'s Captcha failure. Will ban or kick the {@link Player} if they have failed too many times.
     *
     * @param plugin the instance of the {@link ReCaptchaPlugin}
     * @param player the {@link Player} of whom to log the failure
     * @param reason the reason for the ban or kick
     */
    public FailureLogger(ReCaptchaPlugin plugin, Player player, String reason) {
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
     * Gets the amount of times a {@link Player} has failed the Captcha.
     *
     * @param plugin the instance of the {@link ReCaptchaPlugin}
     * @param player the {@link Player} of whom to check the failures
     * @return the amount of times the {@link Player} has failed the Captcha
     */
    public static int getFailures(ReCaptchaPlugin plugin, Player player) {
        // The placeholder
        int times = 0;
        // Iterate through all the times they have failed and add to config after every pass
        for (String s : plugin.getConfig().getStringList("Failed-Captcha-Attempts." + player.getName())) {
            if (s.contains(player.getUniqueId().toString())) {
                times++;
            }
        }
        return times;
    }

    /**
     * Gets the time as a formatted date.
     *
     * @return the time as a formatted date
     */
    private String getTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return ("[" + simpleDateFormat.format(new Date()) + "]");
    }
}
