package io.izzel.arclight.common.bridge.core.inventory.container;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryView;
import org.bukkit.Location;

public interface ContainerBridge {

    InventoryView bridge$getBukkitView();

    void bridge$transferTo(AbstractContainerMenu other, CraftHumanEntity player);

    Component bridge$getTitle();

    void bridge$setTitle(Component title);

    boolean bridge$isCheckReachable();

    /**
     * Позиция блока, из которого меню открыли. Ванильные меню отдают её через
     * {@link PosContainerBridge} по полю {@code access}; у модовых такого поля нет,
     * поэтому позицию запоминают в момент открытия, если провайдер меню — block entity.
     *
     * @return позиция или {@code null}, если она неизвестна
     */
    Location bridge$getOpenLocation();

    void bridge$setOpenLocation(Location location);
}
