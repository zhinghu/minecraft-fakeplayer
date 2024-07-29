package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.devtools.core.utils.EntityUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@Singleton
public class TeleportCommand extends AbstractCommand {

    /**
     * 传送到假人
     */
    public void tp(@NotNull Player sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var target = getTarget(sender, args);
        this.teleport(sender, sender, target);
    }

    /**
     * 将假人传送过来
     */
    public void tphere(@NotNull Player sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var target = getTarget(sender, args);
        this.teleport(sender, target, sender);
    }

    /**
     * 与假人交换位置
     */
    public void tps(@NotNull Player sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var target = getTarget(sender, args);

        var l1 = sender.getLocation();
        var l2 = target.getLocation();

        EntityUtils.teleportAndSound(target, l1);
        EntityUtils.teleportAndSound(sender, l2);
    }

    private void teleport(@NotNull CommandSender sender, @NotNull Player from, @NotNull Player to) {
        if (!EntityUtils.teleportAndSound(from, to.getLocation())) {
            sender.sendMessage(translatable("fakeplayer.command.teleport.error.canceled", RED));
        }
    }

}
