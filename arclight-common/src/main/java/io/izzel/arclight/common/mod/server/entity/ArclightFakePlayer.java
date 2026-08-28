package io.izzel.arclight.common.mod.server.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;

/**
 * Обёртка Bukkit над Forge {@code FakePlayer} — деплойеры Create, харвестеры AE2 и прочие
 * машины, действующие «от имени игрока».
 *
 * <p>У такого игрока {@code getHandle().connection == null}: сети за ним нет. Плагины этого
 * не знают и в слушателях событий спокойно зовут {@code updateInventory()}, шлют сообщения
 * или спрашивают пинг — на настоящем игроке это безобидно, а здесь даёт NPE прямо посреди
 * тика мода, то есть машина ломается из-за плагина. CMI и TAB делают такое особенно охотно.
 *
 * <p>Поэтому всё, что упирается в соединение, здесь превращается в тихий no-op.
 */
public class ArclightFakePlayer extends CraftPlayer {

    public ArclightFakePlayer(CraftServer server, ServerPlayer entity) {
        super(server, entity);
    }

    /** Есть ли за этим игроком настоящее соединение. */
    private boolean hasConnection() {
        return this.getHandle().connection != null;
    }

    @Override
    public boolean isOp() {
        GameProfile profile = this.getHandle().getGameProfile();
        return profile != null && profile.getId() != null && super.isOp();
    }

    @Override
    public void setOp(boolean value) {
    }

    @Override
    public void updateInventory() {
        if (hasConnection()) {
            super.updateInventory();
        }
    }

    @Override
    public void sendMessage(String message) {
        if (hasConnection()) {
            super.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(String... messages) {
        if (hasConnection()) {
            super.sendMessage(messages);
        }
    }

    @Override
    public int getPing() {
        return hasConnection() ? super.getPing() : 0;
    }

    @Override
    public void kickPlayer(String message) {
        if (hasConnection()) {
            super.kickPlayer(message);
        }
    }
}
