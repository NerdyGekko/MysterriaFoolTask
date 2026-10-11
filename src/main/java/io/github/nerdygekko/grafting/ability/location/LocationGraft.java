package io.github.nerdygekko.grafting.ability.location;

import io.github.nerdygekko.grafting.ability.Ability;
import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.ability.GraftMode;
import io.github.nerdygekko.grafting.util.Feedback;
import io.github.nerdygekko.grafting.util.Targeting;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Ties a target to a place chosen in advance. Sneak + left-click sets the anchor, and right-click in
 * Single Target mode moves the entity the player is looking at (or the player) to it.
 */
public final class LocationGraft implements Ability {

    private final Plugin plugin;
    private final NamespacedKey anchorKey;

    public LocationGraft(Plugin plugin) {
        this.plugin = plugin;
        this.anchorKey = new NamespacedKey(plugin, "anchor");
    }

    @Override
    public AbilityType type() {
        return AbilityType.LOCATION;
    }

    @Override
    public void useAlternate(Player player, GraftMode mode) {
        Location anchor = player.getLocation();
        saveAnchor(player, anchor);
        effect(anchor, Particle.END_ROD);
        Feedback.success(player, String.format("Anchor set at %d, %d, %d",
                anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ()));
    }

    @Override
    public boolean use(Player player, GraftMode mode) {
        if (mode == GraftMode.AREA) {
            // TODO: Phase 3b - area wrap
            Feedback.fail(player, "Area grafting isn't built yet");
            return false;
        }
        return graftSingle(player);
    }

    private boolean graftSingle(Player player) {
        Location anchor = loadAnchor(player).orElse(null);
        if (anchor == null) {
            Feedback.fail(player, "Set an anchor first (sneak + left-click)");
            return false;
        }
        if (!anchor.getWorld().equals(player.getWorld())) {
            Feedback.fail(player, "Your anchor is in another world");
            return false;
        }
        // Someone may have built over the anchor since it was set, and teleporting into blocks would suffocate the target
        if (!anchor.getBlock().isPassable() || !anchor.clone().add(0, 1, 0).getBlock().isPassable()) {
            Feedback.fail(player, "Your anchor is blocked");
            return false;
        }

        double range = plugin.getConfig().getDouble("target-range", 30);
        LivingEntity target = Targeting.entity(player, range).orElse(player);
        if (target instanceof Player && target != player && !plugin.getConfig().getBoolean("location.allow-player-targets", true)) {
            Feedback.fail(player, "The Location Graft can't move other players");
            return false;
        }
        double maxDistance = plugin.getConfig().getDouble("location.max-anchor-distance", 500);
        if (maxDistance > 0 && target.getLocation().distanceSquared(anchor) > maxDistance * maxDistance) {
            Feedback.fail(player, (target == player ? "You are" : target.getName() + " is") + " too far from your anchor");
            return false;
        }

        // Keep the target facing the way it already was; only its position is grafted
        Location destination = anchor.clone().setRotation(target.getYaw(), target.getPitch());
        Location origin = target.getLocation();
        // A rider can't be teleported on its own, so take it off its mount first.
        // Anything riding the target comes along, which is Paper's default since 1.21.10
        target.leaveVehicle();
        target.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.PLUGIN)
                .thenAccept(success -> {
                    if (!success) {
                        Feedback.fail(player, "Something stopped the graft");
                        return;
                    }
                    target.setFallDistance(0);
                    effect(origin, Particle.REVERSE_PORTAL);
                    effect(destination, Particle.PORTAL);
                    Feedback.success(player, target == player
                            ? "You were grafted to your anchor"
                            : target.getName() + " was grafted to your anchor");
                });
        return true;
    }

    // Stored on the player as "worldUUID,x,y,z" so the anchor survives logouts and restarts
    private void saveAnchor(Player player, Location location) {
        String value = location.getWorld().getUID() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
        player.getPersistentDataContainer().set(anchorKey, PersistentDataType.STRING, value);
    }

    private Optional<Location> loadAnchor(Player player) {
        String value = player.getPersistentDataContainer().get(anchorKey, PersistentDataType.STRING);
        if (value == null) {
            return Optional.empty();
        }
        try {
            String[] parts = value.split(",");
            World world = Bukkit.getWorld(UUID.fromString(parts[0]));
            if (world == null) {
                // The anchor's world was deleted or unloaded
                return Optional.empty();
            }
            return Optional.of(new Location(world,
                    Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    private static void effect(Location location, Particle particle) {
        location.getWorld().spawnParticle(particle, location.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.05);
        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }
}
