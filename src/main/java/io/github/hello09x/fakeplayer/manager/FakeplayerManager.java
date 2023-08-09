package io.github.hello09x.fakeplayer.manager;

import io.github.hello09x.fakeplayer.Main;
import io.github.hello09x.fakeplayer.entity.FakePlayer;
import io.github.hello09x.fakeplayer.entity.SpawnOption;
import io.github.hello09x.fakeplayer.entity.action.Action;
import io.github.hello09x.fakeplayer.properties.FakeplayerProperties;
import io.github.hello09x.fakeplayer.repository.UsedIdRepository;
import io.github.hello09x.fakeplayer.repository.UserConfigRepository;
import io.github.hello09x.fakeplayer.repository.model.Configs;
import io.github.hello09x.fakeplayer.util.AddressUtils;
import io.github.hello09x.fakeplayer.util.Tasker;
import io.github.hello09x.fakeplayer.util.nms.NMS;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class FakeplayerManager {

    public final static FakeplayerManager instance = new FakeplayerManager();

    private final static Logger log = Main.getInstance().getLogger();

    private final FakeplayerProperties properties = FakeplayerProperties.instance;

    private final UsedIdRepository usedIdRepository = UsedIdRepository.instance;

    private final NameManager nameManager = NameManager.instance;

    private final FakeplayerList playerList = FakeplayerList.instance;

    private final UserConfigRepository userConfigRepository = UserConfigRepository.instance;

    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    private FakeplayerManager() {
        timer.scheduleAtFixedRate(() -> {
            if (Bukkit.getServer().getTPS()[1] < properties.getKaleTps()) {
                new BukkitRunnable() {
                    @Override
                            public void run() {
                                if (removeAll() > 0) {
                                    Bukkit.getServer().broadcast(translatable("[服务器过于卡顿, 已移除所有假人]").style(Style.style(RED, ITALIC)));
                                }
                            }
                        }.runTask(Main.getInstance());
                    }
                }, 0, 60, TimeUnit.SECONDS
        );
    }

    private static List<String> standardCommands(List<String> commands) {
        var ret = new ArrayList<String>(commands.size());
        for (var cmd : commands) {
            cmd = cmd.trim();
            if (cmd.startsWith("/")) {
                if (cmd.length() < 2) {
                    continue;
                }
                cmd = cmd.substring(1);
            }
            if (cmd.isBlank()) {
                continue;
            }
            ret.add(cmd);
        }
        return ret;
    }

    /**
     * 创建一个假人
     *
     * @param creator 创建者
     * @param spawnAt 生成地点
     */
    public @Nullable Player spawn(
            @NotNull CommandSender creator,
            @NotNull Location spawnAt
    ) {
        var playerLimit = properties.getPlayerLimit();
        if (!creator.isOp() && playerLimit != Integer.MAX_VALUE && getAll(creator).size() >= playerLimit) {
            creator.sendMessage(text("你创建的假人数量已达到上限", GRAY));
            return null;
        }

        var serverLimit = properties.getServerLimit();
        if (!creator.isOp() && serverLimit != Integer.MAX_VALUE && getAll().size() >= serverLimit) {
            creator.sendMessage(text("服务器假人数量已达到上限", GRAY));
            return null;
        }

        if (!creator.isOp() && properties.isDetectIp() && countByAddress(AddressUtils.getAddress(creator)) >= 1) {
            creator.sendMessage(text("你所在 IP 创建的假人数量已达到上限", GRAY));
            return null;
        }

        var player = new FakePlayer(
                creator.getName(),
                AddressUtils.getAddress(creator),
                nameManager.take(creator)
        );

        var bukkitPlayer = player.getBukkitPlayer();
        bukkitPlayer.playerListName(text(bukkitPlayer.getName()).style(Style.style(GRAY, ITALIC)));

        boolean invulnerable = true, lookAtEntity = true, collidable = true, pickupItems = true;
        if (creator instanceof Player p) {
            var creatorId = p.getUniqueId();
            invulnerable = userConfigRepository.selectOrDefault(creatorId, Configs.invulnerable);
            lookAtEntity = userConfigRepository.selectOrDefault(creatorId, Configs.look_at_entity);
            collidable = userConfigRepository.selectOrDefault(creatorId, Configs.collidable);
            pickupItems = userConfigRepository.selectOrDefault(creatorId, Configs.pickup_items);
        }
        player.spawn(new SpawnOption(
                spawnAt,
                invulnerable,
                collidable,
                lookAtEntity,
                pickupItems
        ));

        playerList.add(player);
        usedIdRepository.add(bukkitPlayer.getUniqueId());

        Tasker.later(() -> {
            dispatchCommands(bukkitPlayer, properties.getPreparingCommands());
            performCommands(bukkitPlayer, properties.getSelfCommands());
        }, 20);

        return bukkitPlayer;
    }

    /**
     * 获取一个假人
     *
     * @param creator 创建者
     * @param name    假人名称
     * @return 假人
     */
    public @Nullable Player get(@NotNull CommandSender creator, @NotNull String name) {
        return Optional
                .ofNullable(playerList.getByName(name))
                .filter(p -> p.getCreator().equals(creator.getName()))
                .map(FakePlayer::getBukkitPlayer)
                .orElse(null);
    }

    /**
     * 根据名称获取假人
     *
     * @param name 名称
     * @return 假人
     */
    public @Nullable Player get(@NotNull String name) {
        return Optional
                .ofNullable(playerList.getByName(name))
                .map(FakePlayer::getBukkitPlayer)
                .orElse(null);
    }

    /**
     * 根据名称删除假人
     *
     * @param name 名称
     * @return 名称对应的玩家不在线或者不是假人
     */
    public boolean remove(@NotNull String name) {
        var player = get(name);
        if (player == null) {
            return false;
        }

        player.kick();
        return true;
    }

    /**
     * 获取一个假人的创建者, 如果这个玩家不是假人, 则为 {@code null}
     *
     * @param player 假人
     * @return 假人的创建者
     */
    public @Nullable String getCreator(@NotNull Player player) {
        return Optional
                .ofNullable(playerList.getByUUID(player.getUniqueId()))
                .map(FakePlayer::getCreator)
                .orElse(null);
    }

    /**
     * 移除所有假人
     *
     * @return 移除的假人数量
     */
    public int removeAll() {
        var fakers = getAll();
        fakers.forEach(Player::kick);
        return fakers.size();
    }

    /**
     * @return 获取所有假人
     */
    public @NotNull List<Player> getAll() {
        return playerList.getAll().stream().map(FakePlayer::getBukkitPlayer).toList();
    }

    public void cleanup(@NotNull Player player) {
        var fakeplayer = playerList.removeByUUID(player.getUniqueId());
        if (fakeplayer == null) {
            return;
        }
        nameManager.giveback(fakeplayer.getSequenceName());
        if (properties.isDropInventoryOnQuiting()) {
            Action.dropInventory(NMS.getInstance().getServerPlayer(player));
        }
    }

    /**
     * 获取创建者创建的所有假人
     *
     * @param creator 创建者
     * @return 创建者创建的假人
     */
    public @NotNull List<Player> getAll(@NotNull CommandSender creator) {
        return playerList.getByCreator(creator.getName()).stream().map(FakePlayer::getBukkitPlayer).toList();
    }

    /**
     * 判断一名玩家是否是假人
     *
     * @param player 玩家
     * @return 是否是假人
     */
    public boolean isFake(@NotNull Player player) {
        return playerList.getByUUID(player.getUniqueId()) != null;
    }

    /**
     * 获取 IP 地址创建着多少个假人
     *
     * @param address IP 地址
     * @return 该 IP 地址创建着多少个假人
     */
    public long countByAddress(@NotNull String address) {
        return playerList
                .getAll()
                .stream()
                .filter(p -> p.getCreatorIp().equals(address))
                .count();
    }

    /**
     * 以假人身份执行命令
     *
     * @param player   假人
     * @param commands 命令
     */
    public void performCommands(@NotNull Player player, @NotNull List<String> commands) {
        if (commands.isEmpty()) {
            return;
        }
        if (!isFake(player)) {
            return;
        }

        for (var cmd : standardCommands(commands)) {
            if (!player.performCommand(cmd)) {
                log.warning("执行命令失败: " + cmd);
            }
        }
    }

    /**
     * 以控制台身份对玩家执行命令
     *
     * @param player   假人
     * @param commands 命令
     */
    public void dispatchCommands(@NotNull Player player, @NotNull List<String> commands) {
        if (commands.isEmpty()) {
            return;
        }

        if (!isFake(player)) {
            return;
        }

        var server = Bukkit.getServer();
        var sender = Bukkit.getConsoleSender();
        for (var cmd : standardCommands(commands)) {
            if (cmd.length() > 1) {
                cmd = cmd
                        .replace("%p", player.getName())
                        .replace("%u", player.getUniqueId().toString())
                        .replace("%c", Objects.requireNonNull(getCreator(player), "Missing creator id at fake player metadata"));
            }

            if (cmd.isBlank()) {
                continue;
            }

            if (!server.dispatchCommand(sender, cmd)) {
                log.warning("执行命令失败: " + cmd);
            }
        }
    }

    public void onDisable() {
        this.timer.shutdown();
    }

}
