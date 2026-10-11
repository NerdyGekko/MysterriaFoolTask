package io.github.nerdygekko.grafting.util;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Optional;

/**
 * Finds what a player is looking at. Blocks in the way stop the search, so abilities can't target through walls.
 */
public final class Targeting {

    private static final double ENTITY_HITBOX_PADDING = 0.3;

    private Targeting() {
    }

    public static Optional<LivingEntity> entity(Player player, double range) {
        Location eye = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTrace(eye, eye.getDirection(), range,
                FluidCollisionMode.NEVER, true, ENTITY_HITBOX_PADDING,
                entity -> entity != player
                        && entity instanceof LivingEntity
                        && !(entity instanceof Player target && target.getGameMode() == GameMode.SPECTATOR));
        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            return Optional.empty();
        }
        return Optional.of(target);
    }

    public static Optional<Block> block(Player player, double range) {
        return Optional.ofNullable(player.getTargetBlockExact((int) Math.ceil(range), FluidCollisionMode.NEVER));
    }
}
