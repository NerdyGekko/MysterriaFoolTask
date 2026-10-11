package io.github.nerdygekko.grafting.ability.location;

import io.github.nerdygekko.grafting.ability.AbilityType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One active area. Its edge is grafted to the opposite edge, so anything that crosses it comes back in on
 * the far side, still moving the same way. The area is a column from the bottom of the world to the top,
 * so there's no way over or under it.
 * <p>
 * Entities outside are sealed out the same way: walking in at one edge puts you back outside the opposite edge.
 */
final class GraftArea {

    // How far inside (or outside) the edge a wrapped entity is placed, so it isn't wrapped straight back
    private static final double INSET = 1.0;
    // A side change further than this from the edge was a long teleport (like /tp), not a crossing
    private static final double CROSS_MARGIN = 6.0;
    private static final Particle.DustOptions RING_DUST = new Particle.DustOptions(Color.fromRGB(0x55FFFF), 1.2f);

    private final UUID casterId;
    private final World world;
    private final double centreX;
    private final double centreY;
    private final double centreZ;
    private final double radius;
    private final int totalTicks;
    private int ticksLeft;
    // Which side of the edge each nearby entity is on: true = inside
    private Map<UUID, Boolean> sides = new HashMap<>();

    GraftArea(Player caster, double radius, int durationTicks) {
        Location centre = caster.getLocation();
        this.casterId = caster.getUniqueId();
        this.world = centre.getWorld();
        this.centreX = centre.getX();
        this.centreY = centre.getY();
        this.centreZ = centre.getZ();
        this.radius = radius;
        this.totalTicks = durationTicks;
        this.ticksLeft = durationTicks;
    }

    UUID casterId() {
        return casterId;
    }

    double radius() {
        return radius;
    }

    Location centre() {
        return new Location(world, centreX, centreY, centreZ);
    }

    /**
     * Runs one tick of the area.
     *
     * @param wrapping entities currently being moved by an area, so the teleport listener leaves them alone
     * @return false once the area has run out of time
     */
    boolean tick(Set<UUID> wrapping) {
        if (--ticksLeft < 0) {
            return false;
        }
        double reach = radius + CROSS_MARGIN + 2;
        BoundingBox box = new BoundingBox(centreX - reach, world.getMinHeight(), centreZ - reach,
                centreX + reach, world.getMaxHeight(), centreZ + reach);

        Map<UUID, Boolean> seen = new HashMap<>();
        Set<Double> ringHeights = new HashSet<>();
        ringHeights.add(Math.floor(centreY));
        for (Entity entity : world.getNearbyEntities(box)) {
            if (!canWrap(entity)) {
                continue;
            }
            UUID id = entity.getUniqueId();
            double distance = horizontalDistance(entity.getLocation());
            boolean inside = distance <= radius;
            Boolean wasInside = sides.get(id);
            if (wasInside != null && wasInside != inside && Math.abs(distance - radius) <= CROSS_MARGIN) {
                wrap(entity, wasInside, wrapping);
                inside = wasInside;
            }
            seen.put(id, inside);
            if (entity instanceof Player) {
                // Draw the ring at each player's height too, so the edge stays visible when climbing
                ringHeights.add(Math.floor(entity.getLocation().getY()));
            }
        }
        // Entities that weren't found this tick have died, unloaded or teleported far away, so stop tracking them
        sides = seen;

        if (ticksLeft % 2 == 0) {
            ringHeights.forEach(this::drawRing);
        }
        if (ticksLeft % 20 == 0 && Bukkit.getPlayer(casterId) instanceof Player caster) {
            caster.sendActionBar(Component.text("Area: " + (ticksLeft / 20) + "s left", AbilityType.LOCATION.color()));
        }
        return true;
    }

    /**
     * Works out where a teleport that would cross the edge should go instead.
     *
     * @return the redirected destination, the original {@code to} if the teleport doesn't cross the edge,
     * or {@code null} if it should be cancelled
     */
    Location redirect(Entity entity, Location to) {
        Boolean inside = sides.get(entity.getUniqueId());
        if (inside == null || to == null || !world.equals(to.getWorld())
                || (horizontalDistance(to) <= radius) == inside) {
            return to;
        }
        Location wrapped = oppositeSide(to, inside);
        return wrapped == null ? null : findSpace(wrapped, entity);
    }

    void end() {
        Location centre = centre();
        world.playSound(centre, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.2f);
        world.spawnParticle(Particle.REVERSE_PORTAL, centre.add(0, 1, 0), 80, radius / 3, 1, radius / 3, 0.05);
    }

    void start() {
        world.playSound(centre(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
    }

    int secondsTotal() {
        return totalTicks / 20;
    }

    // Players, mobs, projectiles (ender pearls included) and vehicles wrap; dropped items and XP orbs don't
    private static boolean canWrap(Entity entity) {
        if (entity.isInsideVehicle()) {
            // Riders move with their vehicle, which is wrapped instead
            return false;
        }
        if (entity instanceof Player player) {
            return player.getGameMode() != GameMode.SPECTATOR;
        }
        return entity instanceof LivingEntity || entity instanceof Projectile || entity instanceof Vehicle;
    }

    private void wrap(Entity entity, boolean keepInside, Set<UUID> wrapping) {
        Location from = entity.getLocation();
        Location destination = oppositeSide(from, keepInside);
        if (destination == null) {
            return;
        }
        destination = findSpace(destination, entity);
        if (destination == null) {
            // The far side is solid, so bounce the entity back onto the side it came from
            destination = sameSide(from, keepInside);
        }

        Vector velocity = entity.getVelocity();
        wrapping.add(entity.getUniqueId());
        try {
            // Anything riding the entity comes along, which is Paper's default since 1.21.10
            entity.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } finally {
            wrapping.remove(entity.getUniqueId());
        }
        // Keep its momentum, so an arrow or a running player carries on in the same direction
        entity.setVelocity(velocity);

        world.spawnParticle(Particle.REVERSE_PORTAL, from.clone().add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.02);
        world.spawnParticle(Particle.PORTAL, destination.clone().add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.2);
        world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
    }

    /** The point straight across the centre from {@code location}, placed just inside or just outside the edge. */
    private Location oppositeSide(Location location, boolean inside) {
        return placeOnEdge(location, inside, -1);
    }

    private Location sameSide(Location location, boolean inside) {
        return placeOnEdge(location, inside, 1);
    }

    private Location placeOnEdge(Location location, boolean inside, int direction) {
        double dx = location.getX() - centreX;
        double dz = location.getZ() - centreZ;
        double length = Math.hypot(dx, dz);
        if (length < 1.0E-6) {
            // Exactly at the centre, so there's no direction to work with
            return null;
        }
        double distance = inside ? radius - INSET : radius + INSET;
        return new Location(world,
                centreX + direction * dx / length * distance,
                location.getY(),
                centreZ + direction * dz / length * distance,
                location.getYaw(), location.getPitch());
    }

    // Looks a few blocks up and down for room to stand, so a wrap never puts anything inside a wall
    private static Location findSpace(Location location, Entity entity) {
        if (entity instanceof Projectile) {
            return location;
        }
        for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
            Location candidate = location.clone().add(0, dy, 0);
            Block feet = candidate.getBlock();
            if (feet.isPassable() && feet.getRelative(0, 1, 0).isPassable()) {
                return candidate;
            }
        }
        return null;
    }

    private double horizontalDistance(Location location) {
        return Math.hypot(location.getX() - centreX, location.getZ() - centreZ);
    }

    private void drawRing(double y) {
        int points = (int) Math.ceil(2 * Math.PI * radius * 1.5);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            world.spawnParticle(Particle.DUST,
                    centreX + Math.cos(angle) * radius, y + 0.2, centreZ + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0, RING_DUST);
        }
    }
}
