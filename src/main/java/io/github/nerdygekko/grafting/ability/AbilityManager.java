package io.github.nerdygekko.grafting.ability;

import io.github.nerdygekko.grafting.util.Feedback;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Owns every ability and is the single entry point for using one, so permission and cooldown
 * checks apply the same way whether the trigger is an item or a command.
 */
public final class AbilityManager {

    private final Map<AbilityType, Ability> abilities = new EnumMap<>(AbilityType.class);
    private final CooldownManager cooldowns;

    public AbilityManager(CooldownManager cooldowns, List<Ability> abilities) {
        this.cooldowns = cooldowns;
        for (Ability ability : abilities) {
            this.abilities.put(ability.type(), ability);
        }
    }

    public void use(Player player, AbilityType type, GraftMode mode) {
        Ability ability = abilities.get(type);
        if (ability == null || !checkPermission(player, type)) {
            return;
        }
        long remaining = cooldowns.remainingMillis(player, type);
        if (remaining > 0) {
            Feedback.fail(player, String.format("%s is on cooldown for %.1fs", type.displayName(), remaining / 1000.0));
            return;
        }
        if (ability.use(player, mode)) {
            cooldowns.start(player, type);
        }
    }

    public void useAlternate(Player player, AbilityType type, GraftMode mode) {
        Ability ability = abilities.get(type);
        if (ability != null && checkPermission(player, type)) {
            ability.useAlternate(player, mode);
        }
    }

    private boolean checkPermission(Player player, AbilityType type) {
        if (player.hasPermission(type.permission())) {
            return true;
        }
        Feedback.fail(player, "You can't use the " + type.displayName());
        return false;
    }
}
