package com.siuzu.paradox.time;

public final class TimeFreeze {
    private static volatile boolean frozen = false;
    public static boolean isFrozen() { return frozen; }
    public static void toggle() { frozen = !frozen; }
}
