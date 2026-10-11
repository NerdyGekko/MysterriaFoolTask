package io.github.nerdygekko.grafting.listener;

import io.github.nerdygekko.grafting.ability.enchant.EnchantGraftMenu;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Keeps the Enchantment Graft menu safe: only the two input slots accept items, clicking the
 * result performs the graft, and nothing can be taken from the decoration or the preview.
 */
public final class EnchantGraftMenuListener implements Listener {

    private final Plugin plugin;

    public EnchantGraftMenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnchantGraftMenu menu)) {
            return;
        }
        // Double-clicking gathers matching items from every slot, which could pull out the preview
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        // Raw slots number the menu first, then the player's inventory
        boolean clickedMenu = event.getRawSlot() < top.getSize();
        if (clickedMenu) {
            int slot = event.getRawSlot();
            if (slot == EnchantGraftMenu.RESULT_SLOT) {
                event.setCancelled(true);
                menu.graft();
            } else if (EnchantGraftMenu.isInputSlot(slot)) {
                // Let vanilla move the item, then update the preview once it has
                refreshNextTick(menu);
            } else {
                event.setCancelled(true);
            }
        } else if (event.isShiftClick()) {
            // Vanilla would shift the item into the first free menu slot, so place it into an input ourselves
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.isEmpty()) {
                return;
            }
            for (int slot : new int[]{EnchantGraftMenu.TARGET_SLOT, EnchantGraftMenu.SOURCE_SLOT}) {
                ItemStack existing = top.getItem(slot);
                if (existing == null || existing.isEmpty()) {
                    top.setItem(slot, item);
                    event.setCurrentItem(null);
                    menu.refresh();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnchantGraftMenu menu)) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize() && !EnchantGraftMenu.isInputSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        refreshNextTick(menu);
    }

    // Also fires when the player logs out with the menu open
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EnchantGraftMenu menu) {
            menu.returnItems();
        }
    }

    private void refreshNextTick(EnchantGraftMenu menu) {
        Bukkit.getScheduler().runTask(plugin, menu::refresh);
    }
}
