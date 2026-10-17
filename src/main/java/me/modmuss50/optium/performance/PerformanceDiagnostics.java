package me.modmuss50.optium.performance;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Opt-in diagnostics for the Indigo-to-OptiFine block-model bridge.
 *
 * Disabled by default. This class records only the duration around the existing
 * bridge callback; it does not alter rendering decisions or move work between
 * threads. Enable for a diagnostic run with the JVM argument
 * {@code -Doptium.profile.indigo=true}.
 *
 * Values are cumulative for the process lifetime. Instrumented timings include
 * time spent in the existing callback and exclude the counter-update work.
 */
public final class PerformanceDiagnostics {
    private static final boolean INDIGO_PROFILE_ENABLED =
            Boolean.getBoolean("optium.profile.indigo");
    private static final long REPORT_INTERVAL_NANOS =
            TimeUnit.SECONDS.toNanos(10);

    private static final LongAdder INDIGO_CALLS = new LongAdder();
    private static final LongAdder INDIGO_TOTAL_NANOS = new LongAdder();
    private static final AtomicLong INDIGO_MAX_NANOS = new AtomicLong();
    private static final AtomicLong NEXT_REPORT_NANOS =
            new AtomicLong(System.nanoTime() + REPORT_INTERVAL_NANOS);

    static {
        if (INDIGO_PROFILE_ENABLED) {
            System.out.println("[Optium] Indigo bridge profiling enabled; "
                    + "reports are cumulative and instrumentation adds overhead.");
        }
    }

    private PerformanceDiagnostics() {
    }

    public static boolean isIndigoProfilingEnabled() {
        return INDIGO_PROFILE_ENABLED;
    }

    /**
     * Record a completed bridge callback. The caller must pass a non-negative
     * duration measured immediately around the existing callback.
     */
    public static void recordIndigoCallback(long elapsedNanos) {
        if (!INDIGO_PROFILE_ENABLED || elapsedNanos < 0L) {
            return;
        }

        INDIGO_CALLS.increment();
        INDIGO_TOTAL_NANOS.add(elapsedNanos);
        updateMaximum(elapsedNanos);
        reportWhenDue();
    }

    private static void updateMaximum(long elapsedNanos) {
        long previous;
        do {
            previous = INDIGO_MAX_NANOS.get();
            if (elapsedNanos <= previous) {
                return;
            }
        } while (!INDIGO_MAX_NANOS.compareAndSet(previous, elapsedNanos));
    }

    private static void reportWhenDue() {
        long now = System.nanoTime();
        long deadline = NEXT_REPORT_NANOS.get();
        if (now < deadline
                || !NEXT_REPORT_NANOS.compareAndSet(
                        deadline, now + REPORT_INTERVAL_NANOS)) {
            return;
        }

        long calls = INDIGO_CALLS.sum();
        long totalNanos = INDIGO_TOTAL_NANOS.sum();
        long maximumNanos = INDIGO_MAX_NANOS.get();
        double averageMicros = calls == 0L
                ? 0.0D : (double) totalNanos / (double) calls / 1_000.0D;

        System.out.println(String.format(Locale.ROOT,
                "[Optium profile] Indigo block-model bridge (cumulative): "
                        + "calls=%d, avg=%.2f us/call, max=%.2f us",
                calls, averageMicros, maximumNanos / 1_000.0D));
    }
}
