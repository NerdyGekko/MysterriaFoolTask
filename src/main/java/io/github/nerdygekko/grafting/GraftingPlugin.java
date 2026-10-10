package io.github.nerdygekko.grafting;

import org.bukkit.plugin.java.JavaPlugin;

public final class GraftingPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Grafting enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Grafting disabled");
    }
}
