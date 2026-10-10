package io.github.nerdygekko.grafting.ability.time;

import io.github.nerdygekko.grafting.ability.Ability;
import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.ability.GraftMode;
import io.github.nerdygekko.grafting.util.Feedback;
import org.bukkit.entity.Player;

public final class TimeGraft implements Ability {

    @Override
    public AbilityType type() {
        return AbilityType.TIME;
    }

    @Override
    public boolean use(Player player, GraftMode mode) {
        // TODO: Phase 1 - crops, baby mobs, furnaces and brewing stands
        Feedback.fail(player, "The Time Graft isn't built yet");
        return false;
    }
}
