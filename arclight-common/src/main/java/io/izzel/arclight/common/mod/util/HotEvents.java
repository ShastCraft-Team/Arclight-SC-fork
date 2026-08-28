package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.i18n.ArclightConfig;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;

/**
 * Fast-path проверки для горячих Bukkit-событий: если событие никто не слушает,
 * его не нужно ни создавать (аллокация + CraftBlock-обёртки), ни диспатчить.
 * <p>
 * Проверка выполняется на каждый вызов (не кешируется), потому что плагины могут
 * регистрировать слушатели в рантайме. Стоимость - одно чтение поля + длина массива.
 * <p>
 * ВАЖНО: пропускать событие можно только там, где код после него не читает из
 * события значения (или подставляет дефолтные) - см. места использования.
 */
public final class HotEvents {

    private HotEvents() {
    }

    private static final boolean DISABLE_PHYSICS =
        ArclightConfig.spec().getOptimization().isDisableBlockPhysicsEvent();

    /** true = событие нужно создавать и стрелять. */
    public static boolean physics() {
        return !DISABLE_PHYSICS && BlockPhysicsEvent.getHandlerList().getRegisteredListeners().length > 0;
    }

    public static boolean redstone() {
        return BlockRedstoneEvent.getHandlerList().getRegisteredListeners().length > 0;
    }

    public static boolean fade() {
        return BlockFadeEvent.getHandlerList().getRegisteredListeners().length > 0;
    }

    public static boolean spread() {
        return BlockSpreadEvent.getHandlerList().getRegisteredListeners().length > 0;
    }

    public static boolean grow() {
        return BlockGrowEvent.getHandlerList().getRegisteredListeners().length > 0;
    }

    public static boolean form() {
        return BlockFormEvent.getHandlerList().getRegisteredListeners().length > 0
            || EntityBlockFormEvent.getHandlerList().getRegisteredListeners().length > 0;
    }

    public static boolean vehicleUpdate() {
        return VehicleUpdateEvent.getHandlerList().getRegisteredListeners().length > 0;
    }

    public static boolean vehicleMove() {
        return VehicleMoveEvent.getHandlerList().getRegisteredListeners().length > 0;
    }
}
