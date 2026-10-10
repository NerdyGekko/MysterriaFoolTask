package io.github.nerdygekko.grafting.ability.location;

import io.github.nerdygekko.grafting.ability.Ability;
import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.ability.GraftMode;
import io.github.nerdygekko.grafting.util.Feedback;
import org.bukkit.entity.Player;

public final class LocationGraft implements Ability {

    @Override
    public AbilityType type() {
        return AbilityType.LOCATION;
    }

    @Override
    public boolean use(Player player, GraftMode mode) {
        // TODO: Phase 3 - single target teleport and area wrap
        Feedback.fail(player, "The Location Graft isn't built yet");
        return false;
    }
}
