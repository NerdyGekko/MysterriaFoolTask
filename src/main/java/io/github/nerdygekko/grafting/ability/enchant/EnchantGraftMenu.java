package io.github.nerdygekko.grafting.ability.enchant;

import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.util.Feedback;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * One player's Enchantment Graft menu. Being the inventory's holder is how the listener recognises it.
 *
 * <pre>
 * row 1:  .  [label]  .  [label]   .   .    .     .  .
 * row 2:  .  TARGET   .  SOURCE    .   .  RESULT  .  .
 * row 3:  .    .      .    .       .   .    .     .  .
 * </pre>
 */
public final class EnchantGraftMenu implements InventoryHolder {

    public static final int TARGET_SLOT = 10;
    public static final int SOURCE_SLOT = 12;
    public static final int RESULT_SLOT = 15;
    private static final int TARGET_LABEL_SLOT = 1;
    private static final int SOURCE_LABEL_SLOT = 3;

    private final Plugin plugin;
    private final Player viewer;
    private final Inventory inventory;
    private boolean grafted;

    public EnchantGraftMenu(Plugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 27,
                Component.text(AbilityType.ENCHANT.displayName(), AbilityType.ENCHANT.color()));

        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, Component.empty(), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!isInputSlot(slot)) {
                inventory.setItem(slot, filler);
            }
        }
        inventory.setItem(TARGET_LABEL_SLOT, button(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                text("Item to enchant", NamedTextColor.AQUA), List.of(text("Place it below", NamedTextColor.GRAY))));
        inventory.setItem(SOURCE_LABEL_SLOT, button(Material.PURPLE_STAINED_GLASS_PANE,
                text("Enchantment source", NamedTextColor.LIGHT_PURPLE),
                List.of(text("Place it below", NamedTextColor.GRAY), text("It is used up by the graft", NamedTextColor.GRAY))));
        refresh();
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static boolean isInputSlot(int slot) {
        return slot == TARGET_SLOT || slot == SOURCE_SLOT;
    }

    /** Rebuilds the result preview from the current inputs. */
    public void refresh() {
        EnchantMerger.Result merge = EnchantMerger.merge(inventory.getItem(TARGET_SLOT), inventory.getItem(SOURCE_SLOT));
        if (!merge.success()) {
            inventory.setItem(RESULT_SLOT, button(Material.BARRIER, text(merge.error(), NamedTextColor.RED), List.of()));
            return;
        }
        ItemStack preview = merge.result().clone();
        List<Component> lore = new ArrayList<>(preview.lore() == null ? List.of() : preview.lore());
        lore.add(Component.empty());
        lore.add(text("Click to graft", NamedTextColor.YELLOW));
        preview.lore(lore);
        inventory.setItem(RESULT_SLOT, preview);
    }

    /** Performs the graft if the inputs allow it, then closes the menu. */
    public void graft() {
        // The menu stays open for one more tick after a graft, so ignore any extra clicks in that time
        if (grafted) {
            return;
        }
        // Recalculate rather than trusting the preview, in case the inputs changed since it was drawn
        EnchantMerger.Result merge = EnchantMerger.merge(inventory.getItem(TARGET_SLOT), inventory.getItem(SOURCE_SLOT));
        if (!merge.success()) {
            Feedback.fail(viewer, merge.error());
            return;
        }

        ItemStack source = inventory.getItem(SOURCE_SLOT);
        source.setAmount(source.getAmount() - 1);
        inventory.setItem(SOURCE_SLOT, source.isEmpty() ? null : source);
        inventory.setItem(TARGET_SLOT, merge.result());
        grafted = true;

        viewer.playSound(viewer, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
        Feedback.success(viewer, "Enchantments grafted");
        // Closing returns the grafted item to the player. Bukkit doesn't allow closing an inventory
        // during its own click event, so wait a tick
        Bukkit.getScheduler().runTask(plugin, () -> viewer.closeInventory());
    }

    /** Gives the input items back to the player, dropping anything that doesn't fit. Safe to call more than once. */
    public void returnItems() {
        for (int slot : new int[]{TARGET_SLOT, SOURCE_SLOT}) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.isEmpty()) {
                continue;
            }
            inventory.setItem(slot, null);
            viewer.getInventory().addItem(item).values()
                    .forEach(leftover -> viewer.getWorld().dropItem(viewer.getLocation(), leftover));
        }
    }

    private static ItemStack button(Material material, Component name, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        item.editMeta(meta -> {
            meta.itemName(name);
            meta.lore(lore);
            // Plain filler panes show no tooltip at all
            meta.setHideTooltip(name.equals(Component.empty()));
        });
        return item;
    }

    private static Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
