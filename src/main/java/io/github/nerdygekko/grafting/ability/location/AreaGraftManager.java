package io.github.nerdygekko.grafting.ability.location;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Keeps track of every active area, ticks them all from one repeating task, and stops teleports
 * from skipping over an area's edge.
 */
public final class AreaGraftManager implements Listener {

    // Player teleports that could otherwise jump out of (or into) an area. Commands, portals and
    // spectating are left alone, so admins can still move players and portals still work
    private static final Set<PlayerTeleportEvent.TeleportCause> REDIRECTED_CAUSES = EnumSet.of(
            PlayerTeleportEvent.TeleportCause.ENDER_PEARL,
            PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT,
            PlayerTeleportEvent.TeleportCause.PLUGIN);

    private final Map<UUID, GraftArea> areas = new HashMap<>();
    // Entities an area is moving right now. Their teleports come from us, so the listeners skip them
    private final Set<UUID> wrapping = new HashSet<>();

    public AreaGraftManager(Plugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public boolean hasArea(Player caster) {
        return areas.containsKey(caster.getUniqueId());
    }

    /** Creates an area centred on the caster. Returns how long it lasts, in seconds. */
    public int start(Player caster, double radius, int durationTicks) {
        GraftArea area = new GraftArea(caster, radius, durationTicks);
        areas.put(caster.getUniqueId(), area);
        area.start();
        return area.secondsTotal();
    }

    /** Ends the caster's area early. Returns false if they didn't have one. */
    public boolean end(UUID casterId) {
        GraftArea area = areas.remove(casterId);
        if (area == null) {
            return false;
        }
        area.end();
        return true;
    }

    private void tick() {
        Iterator<GraftArea> iterator = areas.values().iterator();
        while (iterator.hasNext()) {
            GraftArea area = iterator.next();
            if (!area.tick(wrapping)) {
                iterator.remove();
                area.end();
            }
        }
    }

    // An area with nobody left to end it shouldn't keep trapping people
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer().getUniqueId());
    }

    // HIGH so other plugins have had their say about the destination first
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (REDIRECTED_CAUSES.contains(event.getCause())) {
            redirect(event.getPlayer(), event.getTo(), event, event::setTo);
        }
    }

    // Endermen, shulkers and mobs eating chorus fruit
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        redirect(event.getEntity(), event.getTo(), event, event::setTo);
    }

    private void redirect(Entity entity, Location to, Cancellable event, Consumer<Location> setTo) {
        if (wrapping.contains(entity.getUniqueId())) {
            return;
        }
        for (GraftArea area : areas.values()) {
            Location redirected = area.redirect(entity, to);
            if (redirected == null) {
                // Nowhere safe on the far side, so the teleport just doesn't happen
                event.setCancelled(true);
                return;
            }
            if (redirected != to) {
                setTo.accept(redirected);
                return;
            }
        }
    }
}
