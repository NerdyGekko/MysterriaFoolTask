package io.github.nerdygekko.grafting.listener;

import io.github.nerdygekko.grafting.ability.AbilityManager;
import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.ability.GraftMode;
import io.github.nerdygekko.grafting.item.GraftItems;
import io.github.nerdygekko.grafting.util.Feedback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Turns clicks with a grafting item into ability uses and mode changes.
 */
public final class GraftItemListener implements Listener {

    // Clicking an entity can fire both the entity and the air click events, so ignore repeat clicks this close together
    private static final int CLICK_DEBOUNCE_TICKS = 4;

    private final GraftItems items;
    private final AbilityManager abilities;
    private final Map<UUID, Integer> lastClickTick = new HashMap<>();

    public GraftItemListener(GraftItems items, AbilityManager abilities) {
        this.items = items;
        this.abilities = abilities;
    }

    // Not ignoreCancelled: right-clicking air arrives already cancelled
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        AbilityType type = items.getType(item).orElse(null);
        if (type == null) {
            return;
        }
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action.isRightClick()) {
            // Also stops the clicked block's own behaviour, like opening a furnace
            event.setCancelled(true);
            rightClick(player, item, type);
        } else if (action.isLeftClick() && player.isSneaking()) {
            event.setCancelled(true);
            if (!isRepeatClick(player)) {
                abilities.useAlternate(player, type, items.getMode(item, type));
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        AbilityType type = items.getType(item).orElse(null);
        if (type == null) {
            return;
        }
        // Stops vanilla interactions such as handing the shard to an allay
        event.setCancelled(true);
        rightClick(player, item, type);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (items.isGraftItem(ingredient)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    private void rightClick(Player player, ItemStack item, AbilityType type) {
        if (isRepeatClick(player)) {
            return;
        }
        if (player.isSneaking() && type.modes().size() > 1) {
            GraftMode mode = items.cycleMode(item, type);
            Feedback.info(player, type.displayName() + " mode: " + mode.displayName(), type.color());
        } else {
            abilities.use(player, type, items.getMode(item, type));
        }
    }

    private boolean isRepeatClick(Player player) {
        int now = Bukkit.getCurrentTick();
        Integer last = lastClickTick.put(player.getUniqueId(), now);
        return last != null && now - last < CLICK_DEBOUNCE_TICKS;
    }
}
