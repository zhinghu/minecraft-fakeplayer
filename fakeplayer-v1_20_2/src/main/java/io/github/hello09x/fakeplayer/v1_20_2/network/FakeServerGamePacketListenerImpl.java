package io.github.hello09x.fakeplayer.v1_20_2.network;

import io.github.hello09x.fakeplayer.api.spi.NMSServerGamePacketListener;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FakeServerGamePacketListenerImpl extends ServerGamePacketListenerImpl implements NMSServerGamePacketListener {

    private final FakeplayerManager manager = Main.getInjector().getInstance(FakeplayerManager.class);

    public FakeServerGamePacketListenerImpl(
            @NotNull MinecraftServer server,
            @NotNull Connection connection,
            @NotNull ServerPlayer player,
            @NotNull CommonListenerCookie cookie
    ) {
        super(server, connection, player, cookie);
        Optional.ofNullable(Bukkit.getPlayer(player.getUUID()))
                .map(CraftPlayer.class::cast)
                .ifPresent(p -> p.addChannel(BUNGEE_CORD_CORRECTED_CHANNEL));
    }

    @Override
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket p) {
            this.handleCustomPayloadPacket(p.payload());
        } else if (packet instanceof ClientboundSetEntityMotionPacket p) {
            this.handleClientboundSetEntityMotionPacket(p);
        }
    }

    /**
     * 玩家被击退的动作由客户端完成, 假人没有客户端因此手动完成这个动作
     */
    public void handleClientboundSetEntityMotionPacket(@NotNull ClientboundSetEntityMotionPacket packet) {
        if (packet.getId() == this.player.getId() && this.player.hurtMarked) {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                this.player.hurtMarked = true;
                this.player.lerpMotion(packet.getXa(), packet.getYa(), packet.getZa());
            });
        }
    }

    private void handleCustomPayloadPacket(@NotNull CustomPacketPayload payload) {
        var channel = payload.id().getNamespace() + ":" + payload.id().getPath();
        if (!channel.equals(BUNGEE_CORD_CORRECTED_CHANNEL)) {
            return;
        }

        var buf = new FriendlyByteBuf(Unpooled.buffer(0, 1048576));
        payload.write(buf);
        var message = buf.array();

        var recipient = Bukkit
                .getOnlinePlayers()
                .stream()
                .filter(manager::isNotFake)
                .findAny()
                .orElse(null);

        if (recipient == null) {
            return;
        }

        recipient.sendPluginMessage(Main.getInstance(), BUNGEE_CORD_CHANNEL, message);
    }

}