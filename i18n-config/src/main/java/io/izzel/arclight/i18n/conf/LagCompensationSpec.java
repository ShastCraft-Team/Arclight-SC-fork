package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

/**
 * Компенсация лага — правки, которые не ускоряют сервер, но убирают у игрока
 * ощущение «всё сломалось», когда тик не укладывается в 50 мс.
 */
@ConfigSerializable
public class LagCompensationSpec {

    @Setting("block-breaking")
    private boolean blockBreaking = true;

    @Setting("max-catchup-ticks")
    private int maxCatchupTicks = 4;

    @Setting("movement")
    private boolean movement = true;

    @Setting("movement-stall-threshold-ms")
    private int movementStallThresholdMs = 200;

    public boolean isBlockBreaking() {
        return blockBreaking;
    }

    public int getMaxCatchupTicks() {
        return Math.max(1, maxCatchupTicks);
    }

    public boolean isMovement() {
        return movement;
    }

    public int getMovementStallThresholdMs() {
        return Math.max(50, movementStallThresholdMs);
    }
}
