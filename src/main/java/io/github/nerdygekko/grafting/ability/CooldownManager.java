package io.github.nerdygekko.grafting.ability;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownManager {

    public static final String BYPASS_PERMISSION = "grafting.cooldown.bypass";

    private final Plugin plugin;
    private final Map<UUID, Map<AbilityType, Long>> readyAt = new HashMap<>();

    public CooldownManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Milliseconds until the player can use the ability again, or 0 if it's ready. */
    public long remainingMillis(Player player, AbilityType type) {
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return 0;
        }
        Long time = readyAt.getOrDefault(player.getUniqueId(), Map.of()).get(type);
        return time == null ? 0 : Math.max(0, time - System.currentTimeMillis());
    }

    public void start(Player player, AbilityType type) {
        // Read the config each time so /graft reload applies immediately
        long millis = (long) (plugin.getConfig().getDouble("cooldowns." + type.id()) * 1000);
        if (millis <= 0) {
            return;
        }
        readyAt.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(AbilityType.class))
                .put(type, System.currentTimeMillis() + millis);
    }
}
