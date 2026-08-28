package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

/**
 * Настройки DAB (distance-based AI throttling) — мобы далеко от игроков
 * пересчитывают AI реже, ближние работают на полной скорости.
 */
@ConfigSerializable
public class DabSpec {

    @Setting("enabled")
    private boolean enabled = true;

    @Setting("start-distance")
    private int startDistance = 12;

    @Setting("activation-dist-mod")
    private int activationDistMod = 8;

    @Setting("max-tick-freq")
    private int maxTickFreq = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public int getStartDistance() {
        return startDistance;
    }

    public int getActivationDistMod() {
        return activationDistMod;
    }

    public int getMaxTickFreq() {
        return maxTickFreq;
    }
}
