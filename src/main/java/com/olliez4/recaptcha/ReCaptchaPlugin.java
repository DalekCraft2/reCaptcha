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
     * An instance of the {@link ReCaptchaGui} class.
     */
    ReCaptchaGui reCaptchaGui;

    @Override
    public void onEnable() {
        getLogger().info("Thanks for using reCaptcha by OLLIEZ4");
        saveDefaultConfig();
        // Initialise the ReCaptchaGui
        reCaptchaGui = new ReCaptchaGui(this);
        // Register the join event, chat event, inventory close event, etc...
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(reCaptchaGui, this);

    }

    /**
     * Listens for {@link PlayerQuitEvent PlayerQuitEvents}.
     *
     * @param playerQuitEvent a {@link PlayerQuitEvent}
     */
    @EventHandler
    public void leave(PlayerQuitEvent playerQuitEvent) {
        if (!isPlayerVerified(playerQuitEvent.getPlayer())) {
            if (!reCaptchaGui.verified.contains(playerQuitEvent.getPlayer())) {
                playerQuitEvent.setQuitMessage("");
            }
        }
    }

    /**
     * Listens for {@link PlayerJoinEvent PlayerJoinEvents}.
     *
     * @param playerJoinEvent a {@link PlayerJoinEvent}
     */
    @EventHandler
    public void join(PlayerJoinEvent playerJoinEvent) {
        // If the player is verified, ignore them
        if (!isPlayerVerified(playerJoinEvent.getPlayer())) {
            format = playerJoinEvent.getJoinMessage().replace(playerJoinEvent.getPlayer().getName(), "NAME");
            playerJoinEvent.setJoinMessage("");
            // Wait "wait-time" ticks before sending the GUI, so they do not instantly close
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
     * Checks whether a {@link Player} is verified.
     *
     * @param player the {@link Player} of whom to check verification
     * @return {@code true} if the {@link Player} is verified
     */
    public boolean isPlayerVerified(Player player) {
        return getConfig().getStringList("Verified-Players").contains(player.getUniqueId().toString());
    }
}
