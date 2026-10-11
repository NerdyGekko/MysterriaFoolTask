package io.github.nerdygekko.grafting.ability.time;

import io.github.nerdygekko.grafting.ability.Ability;
import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.ability.GraftMode;
import io.github.nerdygekko.grafting.util.Feedback;
import io.github.nerdygekko.grafting.util.Targeting;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * Grafts the end of a process onto its start (Forward) or the start onto its end (Reverse).
 * Works on the mob or block the player is looking at: baby/adult mobs, crops, furnaces and brewing stands.
 */
public final class TimeGraft implements Ability {

    // Crops outside the vanilla #crops tag that still grow through age stages
    private static final Set<Material> EXTRA_CROPS = Set.of(
            Material.NETHER_WART, Material.COCOA, Material.SWEET_BERRY_BUSH);

    // Pitcher crops grow into a two-block-tall plant, which setting the age alone can't create
    private static final Set<Material> UNSUPPORTED_CROPS = Set.of(Material.PITCHER_CROP);

    private final Plugin plugin;

    public TimeGraft(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public AbilityType type() {
        return AbilityType.TIME;
    }

    @Override
    public boolean use(Player player, GraftMode mode) {
        boolean forward = mode != GraftMode.REVERSE;
        double range = plugin.getConfig().getDouble("target-range", 30);

        LivingEntity entity = Targeting.entity(player, range).orElse(null);
        if (entity instanceof Ageable mob) {
            return graftMob(player, mob, forward);
        }

        Block block = Targeting.block(player, range).orElse(null);
        if (block != null) {
            BlockState state = block.getState();
            if (state instanceof Furnace furnace) {
                return graftFurnace(player, furnace, forward);
            }
            if (state instanceof BrewingStand stand) {
                return graftBrewingStand(player, stand, forward);
            }
            if (block.getType() == Material.TORCHFLOWER_CROP || block.getType() == Material.TORCHFLOWER) {
                return graftTorchflower(player, block, forward);
            }
            if (isCrop(block.getType()) && block.getBlockData() instanceof org.bukkit.block.data.Ageable crop) {
                return graftCrop(player, block, crop, forward);
            }
        }

        Feedback.fail(player, "There's nothing there for time to graft");
        return false;
    }

    private boolean graftMob(Player player, Ageable mob, boolean forward) {
        if (mob.isAdult() == forward) {
            Feedback.fail(player, mob.getName() + (forward ? " is already grown" : " is already a baby"));
            return false;
        }
        if (forward) {
            mob.setAdult();
        } else {
            mob.setBaby();
        }
        effect(mob.getLocation().add(0, mob.getHeight() / 2, 0), forward);
        Feedback.success(player, mob.getName() + (forward ? " grew up" : " became a baby"));
        return true;
    }

    private boolean graftCrop(Player player, Block block, org.bukkit.block.data.Ageable crop, boolean forward) {
        int targetAge = forward ? crop.getMaximumAge() : 0;
        if (crop.getAge() == targetAge) {
            Feedback.fail(player, forward ? "That crop is already fully grown" : "That crop was only just planted");
            return false;
        }
        crop.setAge(targetAge);
        block.setBlockData(crop);
        effect(block.getLocation().toCenterLocation(), forward);
        Feedback.success(player, forward ? "The crop ripened instantly" : "The crop returned to a seedling");
        return true;
    }

    // Torchflowers finish growing by turning into a different block, so they can't use the age stages alone
    private boolean graftTorchflower(Player player, Block block, boolean forward) {
        boolean grown = block.getType() == Material.TORCHFLOWER;
        if (grown == forward) {
            Feedback.fail(player, forward ? "That torchflower is already fully grown" : "That torchflower was only just planted");
            return false;
        }
        // A crop off farmland would just pop off, so only flowers that were grown on farmland can go back
        if (!forward && block.getRelative(0, -1, 0).getType() != Material.FARMLAND) {
            Feedback.fail(player, "That torchflower wasn't grown from a seed");
            return false;
        }
        block.setType(forward ? Material.TORCHFLOWER : Material.TORCHFLOWER_CROP);
        effect(block.getLocation().toCenterLocation(), forward);
        Feedback.success(player, forward ? "The torchflower bloomed instantly" : "The torchflower returned to a seed");
        return true;
    }

    private boolean graftFurnace(Player player, Furnace furnace, boolean forward) {
        // cookTime only rises while the furnace is lit and has something it can smelt
        if (furnace.getCookTime() <= 0 || (forward && furnace.getBurnTime() <= 0)) {
            Feedback.fail(player, "That furnace isn't smelting anything");
            return false;
        }
        // Vanilla finishes the item on the tick cookTime reaches the total, so this completes it next tick
        // and leaves vanilla to handle the output, XP and recipe unlocks
        furnace.setCookTime((short) (forward ? furnace.getCookTimeTotal() - 1 : 0));
        furnace.update();
        effect(furnace.getLocation().toCenterLocation(), forward);
        Feedback.success(player, forward ? "The furnace finished smelting" : "The furnace's progress was undone");
        return true;
    }

    private boolean graftBrewingStand(Player player, BrewingStand stand, boolean forward) {
        // brewingTime counts down from the full brew time to 0, and is 0 when nothing is brewing
        if (stand.getBrewingTime() <= 0) {
            Feedback.fail(player, "That brewing stand isn't brewing anything");
            return false;
        }
        stand.setBrewingTime(forward ? 1 : stand.getRecipeBrewTime());
        stand.update();
        effect(stand.getLocation().toCenterLocation(), forward);
        Feedback.success(player, forward ? "The brew finished" : "The brew started over");
        return true;
    }

    private static boolean isCrop(Material material) {
        return (Tag.CROPS.isTagged(material) || EXTRA_CROPS.contains(material)) && !UNSUPPORTED_CROPS.contains(material);
    }

    private static void effect(Location location, boolean forward) {
        location.getWorld().spawnParticle(forward ? Particle.HAPPY_VILLAGER : Particle.REVERSE_PORTAL,
                location, 20, 0.4, 0.4, 0.4, forward ? 0 : 0.02);
    }
}
