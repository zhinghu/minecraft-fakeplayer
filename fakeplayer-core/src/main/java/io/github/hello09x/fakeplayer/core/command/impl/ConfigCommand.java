package io.github.hello09x.fakeplayer.core.command.impl;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.bedrock.util.Components;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.UserConfigManager;
import io.github.hello09x.fakeplayer.core.repository.model.Config;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigCommand extends AbstractCommand {

    public final static ConfigCommand instance = new ConfigCommand();

    private final UserConfigManager configManager = UserConfigManager.instance;

    /**
     * 设置配置
     */
    public void setConfig(@NotNull Player sender, @NotNull CommandArguments args) {
        @SuppressWarnings("unchecked")
        var config = (Config<Object>) Objects.requireNonNull(args.get("config"));

        if (!config.hasPermission(sender)) {
            sender.sendMessage(i18n.translate("fakeplayer.command.config.set.error.no-permission", RED));
            return;
        }

        var value = Objects.requireNonNull(args.get("value"));
        configManager.setConfig(sender, config, value);
        sender.sendMessage(i18n.translate(
                "fakeplayer.command.config.set.success", GRAY,
                Placeholder.component("config", i18n.translate(config.translationKey(), GOLD)),
                Placeholder.component("value", text(value.toString(), WHITE))
        ));
    }

    /**
     * 获取所有配置
     */
    public void listConfig(@NotNull Player sender, @NotNull CommandArguments args) {
        CompletableFuture.runAsync(() -> {
            var components = Arrays.stream(Config.values()).map(config -> {
                var options = new ArrayList<>(config.options());
                var value = String.valueOf(configManager.getConfig(sender, config));
                return textOfChildren(
                        i18n.translate(config, GOLD),
                        text(": ", GRAY),
                        join(JoinConfiguration.separator(space()), options.stream().map(option -> {
                            if (option.equals(value)) {
                                return text("[" + option + "]", GREEN, TextDecoration.UNDERLINED);
                            } else {
                                return text("[" + option + "]", WHITE).clickEvent(runCommand("/fp config set " + config.key() + " " + option));
                            }
                        }).toList())
                );
            }).toList();

            var message = Components.join(components, newline());
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> sender.sendMessage(message));
        });
    }


}