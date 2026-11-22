package com.originpixel.systemui.core;

public final class SurfaceStateContractTest {
    public static void main(String[] args) {
        SurfaceStateContract contract = new SurfaceStateContract();
        check(contract.onGesture(SurfaceStateContract.Gesture.HORIZONTAL).state() == SurfaceStateContract.State.HIDDEN, "horizontal gesture does not open surfaces");
        check(contract.onGesture(SurfaceStateContract.Gesture.PULL_DOWN).state() == SurfaceStateContract.State.PEEK, "first pull opens peek");
        check(contract.onGesture(SurfaceStateContract.Gesture.PULL_DOWN).surface() == SurfaceStateContract.Surface.CONTROL_CENTER, "second pull switches to control center");
        check(contract.snapshot().sensitiveContentVisible(), "unlocked expanded content may be visible");
        var locked = contract.onKeyguardChanged(true);
        check(locked.surface() == SurfaceStateContract.Surface.LOCK_SCREEN, "keyguard selects lock surface");
        check(locked.state() == SurfaceStateContract.State.PEEK && !locked.sensitiveContentVisible(), "locking collapses and redacts content");
        System.out.println("SurfaceStateContractTest PASS (6 assertions)");
    }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
}
