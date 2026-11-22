package com.originpixel.performance;

import com.originpixel.motion.SpringIntegrator;

/** Reproducible host microbenchmark; reports algorithm cost, not Android frame performance. */
public final class OfflineBenchmark {
    public static void main(String[] args) {
        final int iterations = 1_000_000;
        var state = new SpringIntegrator.State(0, 0);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            state = SpringIntegrator.step(state, (i & 1) == 0 ? 1 : 0, 320, .85, 1.0 / 120.0);
        }
        long elapsed = System.nanoTime() - start;
        System.out.printf("iterations=%d elapsed_ms=%.3f ns_per_step=%.1f checksum=%.6f%n",
                iterations, elapsed / 1_000_000.0, (double) elapsed / iterations, state.position());
    }
}
