package io.izzel.arclight.common.mixin.core.network;

import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.mod.util.ArclightPingEvent;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Mixin(ServerStatusPacketListenerImpl.class)
public class ServerStatusNetHandlerMixin {

    @Unique
    private static final long arclight$cacheNanos =
        Math.max(0L, ArclightConfig.spec().getOptimization().getPingCacheSeconds()) * 1_000_000_000L;

    @Unique
    private static volatile Object[] arclight$cachedStatus; // [0] = ServerStatus, [1] = Long expiry(nanos)

    @Redirect(method = "handleStatusRequest", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$handleServerPing(Connection networkManager, Packet<?> packetIn) {
        try {
            if (arclight$cacheNanos > 0) {
                Object[] cached = arclight$cachedStatus;
                if (cached != null && System.nanoTime() < (Long) cached[1]) {
                    networkManager.send(new ClientboundStatusResponsePacket((ServerStatus) cached[0]));
                    return;
                }
            }
            var server = ServerLifecycleHooks.getCurrentServer();
            // единый снимок: и событие, и сэмпл, и счётчик работают с одним массивом
            Object[] players = server.getPlayerList().players.toArray();
            ArclightPingEvent event = new ArclightPingEvent(networkManager, server, players);
            Bukkit.getPluginManager().callEvent(event);
            // плагины могли скрыть игроков через iterator().remove() (null-элементы)
            int online = 0;
            List<GameProfile> profiles = new ArrayList<>(players.length);
            for (Object o : players) {
                ServerPlayer player = (ServerPlayer) o;
                if (player != null) {
                    online++;
                    if (player.allowsListing()) {
                        profiles.add(player.getGameProfile());
                    } else {
                        profiles.add(MinecraftServer.ANONYMOUS_PLAYER_PROFILE);
                    }
                }
            }
            if (!server.hidesOnlinePlayers() && !profiles.isEmpty()) {
                Collections.shuffle(profiles);
                profiles = profiles.subList(0, Math.min(profiles.size(), SpigotConfig.playerSample));
            }
            // второй аргумент - реальный онлайн, сэмпл только для тултипа
            ServerStatus.Players playerSample = new ServerStatus.Players(event.getMaxPlayers(), online, (server.hidesOnlinePlayers()) ? Collections.emptyList() : profiles);
            ServerStatus ping = new ServerStatus(
                CraftChatMessage.fromString(event.getMotd(), true)[0],
                Optional.of(playerSample),
                Optional.of(new ServerStatus.Version(server.getServerModName() + " " + server.getServerVersion(), SharedConstants.getCurrentVersion().getProtocolVersion())),
                (event.icon.value != null) ? Optional.of(new ServerStatus.Favicon(event.icon.value)) : Optional.empty(),
                server.enforceSecureProfile(),
                Optional.of(new net.minecraftforge.network.ServerStatusPing())
            );
            if (arclight$cacheNanos > 0) {
                arclight$cachedStatus = new Object[]{ping, System.nanoTime() + arclight$cacheNanos};
            }
            networkManager.send(new ClientboundStatusResponsePacket(ping));
        } catch (Throwable t) {
            // сбой (например, упавший слушатель плагина) не должен глотать ответ - шлём ванильный
            networkManager.send(packetIn);
        }
    }
}
