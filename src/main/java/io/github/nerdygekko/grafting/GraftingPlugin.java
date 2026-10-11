package io.github.nerdygekko.grafting;

import io.github.nerdygekko.grafting.ability.AbilityManager;
import io.github.nerdygekko.grafting.ability.CooldownManager;
import io.github.nerdygekko.grafting.ability.enchant.EnchantGraft;
import io.github.nerdygekko.grafting.ability.location.LocationGraft;
import io.github.nerdygekko.grafting.ability.time.TimeGraft;
import io.github.nerdygekko.grafting.command.GraftCommand;
import io.github.nerdygekko.grafting.item.GraftItems;
import io.github.nerdygekko.grafting.listener.GraftItemListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class GraftingPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        GraftItems items = new GraftItems(this);
        AbilityManager abilities = new AbilityManager(new CooldownManager(this), List.of(
                new LocationGraft(),
                new TimeGraft(this),
                new EnchantGraft()
        ));

        getServer().getPluginManager().registerEvents(new GraftItemListener(items, abilities), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(GraftCommand.create(this, items), "Grafting abilities"));
    }
}
