package io.github.nerdygekko.grafting;

import io.github.nerdygekko.grafting.ability.AbilityManager;
import io.github.nerdygekko.grafting.ability.CooldownManager;
import io.github.nerdygekko.grafting.ability.enchant.EnchantGraft;
import io.github.nerdygekko.grafting.ability.enchant.EnchantGraftMenu;
import io.github.nerdygekko.grafting.ability.location.LocationGraft;
import io.github.nerdygekko.grafting.ability.time.TimeGraft;
import io.github.nerdygekko.grafting.command.GraftCommand;
import io.github.nerdygekko.grafting.item.GraftItems;
import io.github.nerdygekko.grafting.listener.EnchantGraftMenuListener;
import io.github.nerdygekko.grafting.listener.GraftItemListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class GraftingPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        GraftItems items = new GraftItems(this);
        AbilityManager abilities = new AbilityManager(new CooldownManager(this), List.of(
                new LocationGraft(this),
                new TimeGraft(this),
                new EnchantGraft(this)
        ));

        getServer().getPluginManager().registerEvents(new GraftItemListener(items, abilities), this);
        getServer().getPluginManager().registerEvents(new EnchantGraftMenuListener(this), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(GraftCommand.create(this, items), "Grafting abilities"));
    }

    @Override
    public void onDisable() {
        // Hand back items left in open menus so a shutdown or reload can't delete them
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof EnchantGraftMenu menu) {
                menu.returnItems();
                player.closeInventory();
            }
        }
    }
}
