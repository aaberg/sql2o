package org.sql2o.bytecode.bench;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordingFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Топ методов по CPU-сэмплам и топ аллокаций из JFR-записи. Только JDK, без зависимостей.
 *
 * <p>Использование: сначала прогон под записью, потом разбор:
 * <pre>
 * java -XX:StartFlightRecording:filename=bench.jfr,settings=profile,dumponexit=true ... MappingBenchmark bytecode 25
 * java -cp &lt;test-classes&gt; org.sql2o.bytecode.bench.JfrTop bench.jfr
 * </pre>
 */
public class JfrTop {

    public static void main(String[] args) throws Exception {
        final Map<String, Long> samples = new HashMap<>();
        final Map<String, Long> allocated = new HashMap<>();
        final long[] sampleCount = {0};

        try (RecordingFile recording = new RecordingFile(Path.of(args[0]))) {
            while (recording.hasMoreEvents()) {
                final RecordedEvent event = recording.readEvent();
                final String type = event.getEventType().getName();
                if (type.equals("jdk.ExecutionSample")) {
                    sampleCount[0]++;
                    if (event.getStackTrace() == null) {
                        continue;
                    }                    for (RecordedFrame frame : event.getStackTrace().getFrames()) {
                        if (frame.isJavaFrame()) {
                            samples.merge(frame.getMethod().getType().getName() + "."
                                    + frame.getMethod().getName(), 1L, Long::sum);
                        }
                    }
                } else if (type.equals("jdk.ObjectAllocationSample")) {
                    final long weight = event.getLong("weight");
                    String site = "?";
                    if (event.getStackTrace() != null) {
                        for (RecordedFrame frame : event.getStackTrace().getFrames()) {
                            if (frame.isJavaFrame()) {
                                site = frame.getMethod().getType().getName() + "."
                                        + frame.getMethod().getName();
                                break;
                            }
                        }
                    }
                    allocated.merge(event.getClass("objectClass").getName() + " @ " + site,
                            weight, Long::sum);
                }
            }
        }

        System.out.println("== CPU: top frames of " + sampleCount[0] + " samples ==");
        top(samples, 25).forEach(entry ->
                System.out.printf("%6.1f%% %s%n", 100.0 * entry.getValue() / sampleCount[0], entry.getKey()));

        final long totalAllocated = allocated.values().stream().mapToLong(Long::longValue).sum();
        System.out.println("== allocations: top sites, " + totalAllocated + " sampled bytes ==");
        top(allocated, 20).forEach(entry ->
                System.out.printf("%6.1f%% %s%n", 100.0 * entry.getValue() / totalAllocated, entry.getKey()));
    }

    private static List<Map.Entry<String, Long>> top(Map<String, Long> counts, int limit) {
        final List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return entries.subList(0, Math.min(limit, entries.size()));
    }
}
