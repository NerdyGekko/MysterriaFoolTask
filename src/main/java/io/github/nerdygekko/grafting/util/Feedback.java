package io.github.nerdygekko.grafting.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Short action bar messages with a matching sound, used for all ability feedback.
 */
public final class Feedback {

    private Feedback() {
    }

    public static void success(Player player, String message) {
        player.sendActionBar(Component.text(message, NamedTextColor.LIGHT_PURPLE));
        player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
    }

    public static void info(Player player, String message, TextColor color) {
        player.sendActionBar(Component.text(message, color));
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
    }

    public static void fail(Player player, String message) {
        player.sendActionBar(Component.text(message, NamedTextColor.RED));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
    }
}
