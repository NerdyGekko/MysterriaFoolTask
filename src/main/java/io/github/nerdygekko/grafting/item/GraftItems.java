package io.github.nerdygekko.grafting.item;

import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.ability.GraftMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Creates grafting items and reads their hidden tags. An item is a grafting item only if it
 * carries the ability tag, so renaming it or using a plain vanilla copy has no effect.
 */
public final class GraftItems {

    private final NamespacedKey abilityKey;
    private final NamespacedKey modeKey;

    public GraftItems(Plugin plugin) {
        this.abilityKey = new NamespacedKey(plugin, "ability");
        this.modeKey = new NamespacedKey(plugin, "mode");
    }

    public ItemStack create(AbilityType type) {
        ItemStack item = ItemStack.of(type.material());
        item.editPersistentDataContainer(pdc -> {
            pdc.set(abilityKey, PersistentDataType.STRING, type.id());
            if (!type.modes().isEmpty()) {
                pdc.set(modeKey, PersistentDataType.STRING, type.modes().getFirst().name());
            }
        });
        item.editMeta(meta -> {
            meta.itemName(Component.text(type.displayName(), type.color()));
            meta.setEnchantmentGlintOverride(true);
            meta.setMaxStackSize(1);
        });
        updateLore(item, type);
        return item;
    }

    public Optional<AbilityType> getType(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }
        String id = item.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
        return id == null ? Optional.empty() : AbilityType.fromId(id);
    }

    public boolean isGraftItem(ItemStack item) {
        return getType(item).isPresent();
    }

    /** The item's current mode, or {@code null} if its ability has no modes. */
    public GraftMode getMode(ItemStack item, AbilityType type) {
        if (type.modes().isEmpty()) {
            return null;
        }
        String name = item.getPersistentDataContainer().get(modeKey, PersistentDataType.STRING);
        for (GraftMode mode : type.modes()) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return type.modes().getFirst();
    }

    /** Switches the item to its ability's next mode and returns the new mode. */
    public GraftMode cycleMode(ItemStack item, AbilityType type) {
        List<GraftMode> modes = type.modes();
        GraftMode next = modes.get((modes.indexOf(getMode(item, type)) + 1) % modes.size());
        item.editPersistentDataContainer(pdc -> pdc.set(modeKey, PersistentDataType.STRING, next.name()));
        updateLore(item, type);
        return next;
    }

    private void updateLore(ItemStack item, AbilityType type) {
        List<Component> lore = new ArrayList<>();
        lore.add(line(type.description(), NamedTextColor.GRAY));
        GraftMode mode = getMode(item, type);
        if (mode != null) {
            lore.add(Component.empty());
            lore.add(line("Mode: ", NamedTextColor.GRAY).append(Component.text(mode.displayName(), type.color())));
        }
        lore.add(Component.empty());
        for (String control : type.controls()) {
            lore.add(line(control, NamedTextColor.DARK_GRAY));
        }
        item.editMeta(meta -> meta.lore(lore));
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
