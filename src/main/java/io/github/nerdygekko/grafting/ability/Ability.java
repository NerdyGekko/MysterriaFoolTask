package io.github.nerdygekko.grafting.ability;

import org.bukkit.entity.Player;

public interface Ability {

    AbilityType type();

    /**
     * Called when the player right-clicks with the ability's item.
     *
     * @param mode the item's current mode, or {@code null} if the ability has no modes
     * @return {@code true} if the graft happened, which starts the cooldown
     */
    boolean use(Player player, GraftMode mode);

    /**
     * Called when the player sneak + left-clicks with the ability's item. Never triggers a cooldown.
     */
    default void useAlternate(Player player, GraftMode mode) {
    }
}
