package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

@Singleton
public class SelectCommand extends AbstractCommand {

    public void select(@NotNull Player sender, @NotNull CommandArguments args) {
        var target = this.getTargetNullable(sender, args);
        manager.setSelection(sender, target);
        if (target == null) {
            sender.sendMessage(translatable("fakeplayer.command.select.success.clear", GRAY));
        } else {
            sender.sendMessage(translatable(
                    "fakeplayer.command.select.success.selected",
                    text(target.getName(), WHITE)
            ).color(GRAY));
        }
    }

    public void selection(@NotNull Player sender, @NotNull CommandArguments args) {
        var selection = manager.getSelection(sender);
        if (selection == null) {
            sender.sendMessage(translatable("fakeplayer.command.selection.error.none", GRAY));
        } else {
            sender.sendMessage(translatable(
                    "fakeplayer.command.selection.success",
                    text(selection.getName(), WHITE)
            ).color(GRAY));
        }
    }

}
