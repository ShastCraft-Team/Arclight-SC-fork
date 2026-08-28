package io.izzel.arclight.common.bridge.optimization;

public interface EntityBridge_ActivationRange {

    void bridge$inactiveTick();

    void bridge$updateActivation();

    /**
     * Интервал тика AI для этой сущности (DAB). 1 = полная скорость.
     */
    int bridge$dabPriority();

    /**
     * Аккумулирует минимальный (=ближайший игрок) приоритет за текущий тик.
     */
    void bridge$dabAccumulate(double distSq);
}
