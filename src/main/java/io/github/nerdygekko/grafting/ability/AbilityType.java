package io.github.nerdygekko.grafting.ability;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every grafting ability and the item that carries it.
 */
public enum AbilityType {
    LOCATION(
            "Location Graft",
            NamedTextColor.AQUA,
            Material.RECOVERY_COMPASS,
            "Ties a target to a place chosen in advance.",
            List.of(GraftMode.SINGLE, GraftMode.AREA),
            List.of("Right-click: graft", "Sneak + right-click: change mode", "Sneak + left-click: set anchor")
    ),
    TIME(
            "Time Graft",
            NamedTextColor.GOLD,
            Material.CLOCK,
            "Ties the end of a process to its start.",
            List.of(GraftMode.FORWARD, GraftMode.REVERSE),
            List.of("Right-click: graft", "Sneak + right-click: change mode")
    ),
    ENCHANT(
            "Enchantment Graft",
            NamedTextColor.LIGHT_PURPLE,
            Material.AMETHYST_SHARD,
            "Grafts enchantments from one item onto another.",
            List.of(),
            List.of("Right-click: open grafting menu")
    );

    private final String displayName;
    private final TextColor color;
    private final Material material;
    private final String description;
    private final List<GraftMode> modes;
    private final List<String> controls;

    AbilityType(String displayName, TextColor color, Material material, String description,
                List<GraftMode> modes, List<String> controls) {
        this.displayName = displayName;
        this.color = color;
        this.material = material;
        this.description = description;
        this.modes = modes;
        this.controls = controls;
    }

    /** Lowercase id used in commands, config keys, permissions and item tags. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public TextColor color() {
        return color;
    }

    public Material material() {
        return material;
    }

    public String description() {
        return description;
    }

    /** Modes the item can cycle through. Empty for abilities with a single behaviour. */
    public List<GraftMode> modes() {
        return modes;
    }

    public List<String> controls() {
        return controls;
    }

    public String permission() {
        return "grafting.use." + id();
    }

    public static Optional<AbilityType> fromId(String id) {
        for (AbilityType type : values()) {
            if (type.id().equalsIgnoreCase(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
