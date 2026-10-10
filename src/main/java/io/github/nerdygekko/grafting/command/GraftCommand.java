package io.github.nerdygekko.grafting.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.nerdygekko.grafting.ability.AbilityType;
import io.github.nerdygekko.grafting.item.GraftItems;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * /graft give &lt;players&gt; &lt;ability&gt; and /graft reload.
 */
public final class GraftCommand {

    private GraftCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> create(Plugin plugin, GraftItems items) {
        return Commands.literal("graft")
                .then(Commands.literal("give")
                        .requires(source -> source.getSender().hasPermission("grafting.give"))
                        .then(Commands.argument("players", ArgumentTypes.players())
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (AbilityType type : AbilityType.values()) {
                                                if (type.id().startsWith(builder.getRemainingLowerCase())) {
                                                    builder.suggest(type.id());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> give(context, items)))))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission("grafting.reload"))
                        .executes(context -> {
                            plugin.reloadConfig();
                            context.getSource().getSender().sendMessage(
                                    Component.text("Grafting config reloaded", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private static int give(CommandContext<CommandSourceStack> context, GraftItems items) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "ability");
        AbilityType type = AbilityType.fromId(id).orElse(null);
        if (type == null) {
            context.getSource().getSender().sendMessage(Component.text("Unknown ability: " + id, NamedTextColor.RED));
            return 0;
        }
        List<Player> players = context.getArgument("players", PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource());
        if (players.isEmpty()) {
            context.getSource().getSender().sendMessage(Component.text("No players found", NamedTextColor.RED));
            return 0;
        }
        for (Player player : players) {
            // Drop the item at their feet if their inventory is full
            player.getInventory().addItem(items.create(type)).values()
                    .forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
        }
        context.getSource().getSender().sendMessage(Component.text(
                "Gave the " + type.displayName() + " to " + players.size() + " player" + (players.size() == 1 ? "" : "s"),
                NamedTextColor.GREEN));
        return players.size();
    }
}
