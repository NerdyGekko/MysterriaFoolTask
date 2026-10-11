package io.github.nerdygekko.grafting.ability.enchant;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Works out what grafting one item's enchantments onto another produces. Follows anvil rules for
 * levels, but ignores enchantment conflicts so pairs like Silk Touch and Fortune can sit together.
 */
public final class EnchantMerger {

    private EnchantMerger() {
    }

    /**
     * @param result the grafted item, or {@code null} if the graft isn't possible
     * @param error  why the graft isn't possible, or {@code null} if it is
     */
    public record Result(ItemStack result, String error) {

        static Result fail(String error) {
            return new Result(null, error);
        }

        public boolean success() {
            return result != null;
        }
    }

    public static Result merge(ItemStack target, ItemStack source) {
        if (target == null || target.isEmpty()) {
            return Result.fail("Place an item to enchant");
        }
        if (source == null || source.isEmpty()) {
            return Result.fail("Place an enchantment source");
        }
        if (target.getAmount() > 1) {
            return Result.fail("Only one item can be enchanted at a time");
        }
        Map<Enchantment, Integer> sourceEnchants = enchantsOf(source);
        if (sourceEnchants.isEmpty()) {
            return Result.fail("The source has no enchantments");
        }

        // Grafting onto a plain book turns it into an enchanted book, like an enchanting table would
        ItemStack result = target.getType() == Material.BOOK ? target.withType(Material.ENCHANTED_BOOK) : target.clone();
        boolean isBook = result.getType() == Material.ENCHANTED_BOOK;
        Map<Enchantment, Integer> resultEnchants = new HashMap<>(enchantsOf(result));
        boolean changed = false;

        for (Map.Entry<Enchantment, Integer> entry : sourceEnchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int sourceLevel = entry.getValue();
            // Books hold anything; other items only take enchantments that suit them (no Sharpness on a pickaxe)
            if (!isBook && !enchant.canEnchantItem(target)) {
                continue;
            }
            int current = resultEnchants.getOrDefault(enchant, 0);
            int merged = current == sourceLevel
                    ? Math.max(sourceLevel, Math.min(sourceLevel + 1, enchant.getMaxLevel()))
                    : Math.max(current, sourceLevel);
            if (merged > current) {
                resultEnchants.put(enchant, merged);
                changed = true;
            }
        }

        if (!changed) {
            return Result.fail("None of those enchantments can be grafted onto that item");
        }

        // ItemMeta skips conflict checks, which is what lets incompatible enchantments be combined
        result.editMeta(meta -> {
            if (meta instanceof EnchantmentStorageMeta book) {
                resultEnchants.forEach((enchant, level) -> book.addStoredEnchant(enchant, level, true));
            } else {
                resultEnchants.forEach((enchant, level) -> meta.addEnchant(enchant, level, true));
            }
        });
        return new Result(result, null);
    }

    private static Map<Enchantment, Integer> enchantsOf(ItemStack item) {
        if (item.getItemMeta() instanceof EnchantmentStorageMeta book) {
            return book.getStoredEnchants();
        }
        return item.getEnchantments();
    }
}
