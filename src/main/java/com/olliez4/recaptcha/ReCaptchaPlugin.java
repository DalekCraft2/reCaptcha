package com.olliez4.recaptcha;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ReCaptchaPlugin extends JavaPlugin implements Listener {

    public static String format;

    /**
     * An instance of the CaptchaGUI class
     */
    ReCaptchaGui reCaptchaGui;

    public void onEnable() {
        getLogger().info("Thanks for using reCaptcha by OLLIEZ4");
        saveDefaultConfig();
        // Initialise the CaptchaGui
        reCaptchaGui = new ReCaptchaGui(this);
        // Register the join event, chat event, inventory close event, etc...
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(reCaptchaGui, this);

    }

    @EventHandler
    public void leave(PlayerQuitEvent playerQuitEvent) {
        if (!isPlayerVerified(playerQuitEvent.getPlayer())) {
            if (!reCaptchaGui.verified.contains(playerQuitEvent.getPlayer())) {
                playerQuitEvent.setQuitMessage("");
            }
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent playerJoinEvent) {
        // If the player is verified, ignore them
        if (!isPlayerVerified(playerJoinEvent.getPlayer())) {
            format = playerJoinEvent.getJoinMessage().replace(playerJoinEvent.getPlayer().getName(), "NAME");
            playerJoinEvent.setJoinMessage("");
            // Wait "wait-time" ticks before sending the GUI so they do not instantly close
            // it
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                // The player is not verified, alert console to make logs easier and send the
                // Captcha to the player
                Player player = playerJoinEvent.getPlayer();
                getLogger().info(player.getName() + " has not verified their Captcha code before. Sending now.");
                reCaptchaGui.send(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 10000, 255, true, false, false));
                player.setInvulnerable(true);
            }, getConfig().getInt("wait-time"));
        }
    }

    /**
     * Check if the player is verified or not
     *
     * @param player The player of whom to check verification
     * @return Whether the player is verified
     */
    public boolean isPlayerVerified(Player player) {
        return getConfig().getStringList("Verified-Players").contains(player.getUniqueId().toString());
    }
}
