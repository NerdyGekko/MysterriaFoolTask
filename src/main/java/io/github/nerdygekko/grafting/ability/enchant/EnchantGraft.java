package io.github.nerdygekko.grafting.ability.enchant;

import io.github.nerdygekko.grafting.ability.Ability;
import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.ability.GraftMode;
import io.github.nerdygekko.grafting.util.Feedback;
import org.bukkit.entity.Player;

public final class EnchantGraft implements Ability {

    @Override
    public AbilityType type() {
        return AbilityType.ENCHANT;
    }

    @Override
    public boolean use(Player player, GraftMode mode) {
        // TODO: Phase 2 - enchantment merging GUI
        Feedback.fail(player, "The Enchantment Graft isn't built yet");
        return false;
    }
}
