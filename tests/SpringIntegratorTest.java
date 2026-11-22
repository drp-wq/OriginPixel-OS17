package com.originpixel.motion;

public final class SpringIntegratorTest {
    public static void main(String[] args) {
        var state = new SpringIntegrator.State(0, 0);
        for (int i = 0; i < 30; i++) state = SpringIntegrator.step(state, 1, 320, .85, 1.0 / 120.0);
        check(state.position() > 0 && state.position() < 1.2, "spring moves toward target without large overshoot");
        check(Math.abs(state.position() - 1) < .25, "spring approaches target over 250ms");
        boolean badStepRejected = false;
        try { SpringIntegrator.step(state, 1, 320, .85, .1); } catch (IllegalArgumentException expected) { badStepRejected = true; }
        check(badStepRejected, "large timestep is rejected to avoid unstable integration");
        System.out.println("SpringIntegratorTest PASS (3 assertions)");
    }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
}
