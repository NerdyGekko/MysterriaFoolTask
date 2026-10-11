package io.github.nerdygekko.grafting.ability.enchant;

import io.github.nerdygekko.grafting.ability.Ability;
import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.ability.GraftMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Opens a menu that grafts the enchantments from one item onto another.
 */
public final class EnchantGraft implements Ability {

    private final Plugin plugin;

    public EnchantGraft(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public AbilityType type() {
        return AbilityType.ENCHANT;
    }

    @Override
    public boolean use(Player player, GraftMode mode) {
        new EnchantGraftMenu(plugin, player).open();
        return true;
    }
}
