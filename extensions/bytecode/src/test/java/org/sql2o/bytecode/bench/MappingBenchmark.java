package org.sql2o.bytecode.bench;

import org.sql2o.DefaultResultSetHandlerFactoryBuilder;
import org.sql2o.ResultSetHandler;
import org.sql2o.ResultSetHandlerFactory;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;
import org.sql2o.bytecode.BytecodeResultSetHandlerFactoryBuilder;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;

/**
 * Bytecode against reflection, reading 20000 employees out of a fake result set.
 *
 * <p>There is no driver in it at all: {@link FakeResultSet} hands over prebuilt rows of the value types a driver would
 * have returned (String, java.sql.Date, BigDecimal), so the converters do the same work they do on a live base. What is
 * left is the mapping itself: reading a column, converting it, putting it somewhere.
 *
 * <p>No claim to being JMH, but none of the main traps of hand-rolled measurements either:
 * <ul>
 *   <li>both paths are warmed up before measuring, interleaved, so the JIT heats both and the drift splits evenly;</li>
 *   <li>the result is consumed into a checksum, which cannot be discarded as dead code;</li>
 *   <li>the two checksums are compared, which also shows that both paths read the same thing;</li>
 *   <li>the bytecode side runs with the fallback off, since a quietly fallen-back measurement would flatter reflection.</li>
 * </ul>
 *
 * <p>Run after compiling the tests (the name is not {@code *Test}, so surefire leaves it alone):
 * <pre>
 * mvn -B -pl extensions/bytecode -am test-compile
 * java -cp "extensions/bytecode/target/test-classes;extensions/bytecode/target/classes;core/target/classes;..." \
 *     org.sql2o.bytecode.bench.MappingBenchmark
 * </pre>
 */
public class MappingBenchmark {

    private static final String[] LABELS = {"fullName", "birthDate", "salary", "gender",
            "passportSeries", "passportNumber", "email", "phone"};

    private static final int ROWS = 20_000;
    private static final int WARMUP = 15;
    private static final int MEASURED = 10;

    private static Object[][] rows;
    private static ResultSetMetaData meta;

    public static void main(String[] args) throws Exception {
        final String mode = args.length > 0 ? args[0] : "both";
        final int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 0;

        generateRows();
        meta = new FakeMeta(LABELS);

        final Quirks quirks = new NoQuirks();
        final ResultSetHandlerFactory<Employee> bytecodePojo = bytecodeFactory(quirks, Employee.class);
        final ResultSetHandlerFactory<Employee> reflectivePojo = reflectiveFactory(quirks, Employee.class);
        final ResultSetHandlerFactory<EmployeeRecord> bytecodeRecord = bytecodeFactory(quirks, EmployeeRecord.class);
        final ResultSetHandlerFactory<EmployeeRecord> reflectiveRecord =
                reflectiveFactory(quirks, EmployeeRecord.class);

        if (seconds > 0) {
            // Profiling mode: one path spins for the given number of seconds so the sampler has something to
            // collect. The warmup is short — what follows is one sustained steady-state read.
            profile(mode, seconds, bytecodePojo, reflectivePojo, bytecodeRecord, reflectiveRecord);
            return;
        }

        // Interleaved warmup: every path is hot by the time measuring starts.
        for (int i = 0; i < WARMUP; i++) {
            fetch(bytecodePojo, MappingBenchmark::checksumOf);
            fetch(reflectivePojo, MappingBenchmark::checksumOf);
            fetch(bytecodeRecord, MappingBenchmark::checksumOfRecord);
            fetch(reflectiveRecord, MappingBenchmark::checksumOfRecord);
        }

        final long bytecodePojoCheck = fetch(bytecodePojo, MappingBenchmark::checksumOf);
        final long reflectivePojoCheck = fetch(reflectivePojo, MappingBenchmark::checksumOf);
        final long bytecodeRecordCheck = fetch(bytecodeRecord, MappingBenchmark::checksumOfRecord);
        final long reflectiveRecordCheck = fetch(reflectiveRecord, MappingBenchmark::checksumOfRecord);
        if (bytecodePojoCheck != reflectivePojoCheck || bytecodePojoCheck != bytecodeRecordCheck
                || bytecodePojoCheck != reflectiveRecordCheck) {
            throw new AssertionError("paths read differently: pojo " + bytecodePojoCheck + "/" + reflectivePojoCheck
                    + ", record " + bytecodeRecordCheck + "/" + reflectiveRecordCheck);
        }

        final long bytecodePojoNs = measure(bytecodePojo, MappingBenchmark::checksumOf);
        final long reflectivePojoNs = measure(reflectivePojo, MappingBenchmark::checksumOf);
        final long bytecodeRecordNs = measure(bytecodeRecord, MappingBenchmark::checksumOfRecord);
        final long reflectiveRecordNs = measure(reflectiveRecord, MappingBenchmark::checksumOfRecord);

        System.out.println("rows per scan : " + ROWS);
        System.out.println("checksum      : " + bytecodePojoCheck + " (all four paths agree)");
        System.out.printf("pojo  bytecode : %,d ns/row%n", bytecodePojoNs / ROWS);
        System.out.printf("pojo  reflect  : %,d ns/row%n", reflectivePojoNs / ROWS);
        System.out.printf("pojo  speedup  : %.2fx%n", (double) reflectivePojoNs / bytecodePojoNs);
        System.out.printf("record bytecode: %,d ns/row%n", bytecodeRecordNs / ROWS);
        System.out.printf("record reflect : %,d ns/row%n", reflectiveRecordNs / ROWS);
        System.out.printf("record speedup : %.2fx%n", (double) reflectiveRecordNs / bytecodeRecordNs);
        System.out.printf("alloc pojo bytecode : %,d bytes/row%n",
                allocatedPerRow(bytecodePojo, MappingBenchmark::checksumOf) / ROWS);
        System.out.printf("alloc pojo reflect  : %,d bytes/row%n",
                allocatedPerRow(reflectivePojo, MappingBenchmark::checksumOf) / ROWS);
        System.out.printf("alloc record bytecode: %,d bytes/row%n",
                allocatedPerRow(bytecodeRecord, MappingBenchmark::checksumOfRecord) / ROWS);
        System.out.printf("alloc record reflect : %,d bytes/row%n",
                allocatedPerRow(reflectiveRecord, MappingBenchmark::checksumOfRecord) / ROWS);
    }

    private interface RowCheck<T> {
        long check(T row);
    }

    private static void profile(String mode, int seconds,
                                ResultSetHandlerFactory<Employee> bytecodePojo,
                                ResultSetHandlerFactory<Employee> reflectivePojo,
                                ResultSetHandlerFactory<EmployeeRecord> bytecodeRecord,
                                ResultSetHandlerFactory<EmployeeRecord> reflectiveRecord) throws Exception {
        switch (mode) {
            case "reflective":
            case "reflective-pojo":
                profileOne(reflectivePojo, MappingBenchmark::checksumOf, mode, seconds);
                break;
            case "bytecode-record":
                profileOne(bytecodeRecord, MappingBenchmark::checksumOfRecord, mode, seconds);
                break;
            case "reflective-record":
                profileOne(reflectiveRecord, MappingBenchmark::checksumOfRecord, mode, seconds);
                break;
            default:
                profileOne(bytecodePojo, MappingBenchmark::checksumOf, "bytecode", seconds);
                break;
        }
    }

    private static <T> void profileOne(ResultSetHandlerFactory<T> factory, RowCheck<T> check,
                                       String mode, int seconds) throws Exception {
        for (int i = 0; i < 5; i++) {
            fetch(factory, check);
        }
        final long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        long scans = 0;
        long checksum = 0;
        while (System.nanoTime() < deadline) {
            checksum ^= fetch(factory, check);
            scans++;
        }
        System.out.println(mode + ": " + scans + " scans, checksum " + checksum);
    }

    /**
     * The exact thread allocation over several scans, in bytes. Unlike JFR samples this is not an estimate:
     * the counter counts every byte. Divided by the row count, so the garbage per row is visible.
     */
    private static <T> long allocatedPerRow(ResultSetHandlerFactory<T> factory, RowCheck<T> check) throws Exception {
        final var mx = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        final long id = Thread.currentThread().getId();
        // Warming the counter up: the first measurement after a GC, so it does not read leftovers.
        System.gc();
        fetch(factory, check);
        final long before = mx.getThreadAllocatedBytes(id);
        for (int i = 0; i < MEASURED; i++) {
            fetch(factory, check);
        }
        return (mx.getThreadAllocatedBytes(id) - before) / MEASURED;
    }

    /** The mean time of one full scan over {@link #MEASURED} passes, in nanoseconds. */
    private static <T> long measure(ResultSetHandlerFactory<T> factory, RowCheck<T> check) throws Exception {
        System.gc();
        long total = 0;
        long sum = 0;
        for (int i = 0; i < MEASURED; i++) {
            final long started = System.nanoTime();
            sum ^= fetch(factory, check);
            total += System.nanoTime() - started;
        }
        // The checksum is there so the read cannot be optimized away.
        if (sum == 0x12345678L) {
            System.out.println("unreachable");
        }
        return total / MEASURED;
    }

    /** One full scan of the rows through the factory's handler; returns a checksum over the rows. */
    private static <T> long fetch(ResultSetHandlerFactory<T> factory, RowCheck<T> check) throws Exception {
        final ResultSetHandler<T> handler = factory.newResultSetHandler(meta);
        final ResultSet rs = new FakeResultSet(rows, meta);
        long sum = 0;
        while (rs.next()) {
            sum = sum * 31 + check.check(handler.handle(rs));
        }
        return sum;
    }

    private static long checksumOf(Employee row) {
        long check = 0;
        check = check * 31 + row.fullName.hashCode();
        check = check * 31 + row.birthDate.hashCode();
        check = check * 31 + row.salary.unscaledValue().longValue();
        check = check * 31 + row.gender.ordinal();
        check = check * 31 + row.passportSeries.hashCode();
        check = check * 31 + row.passportNumber.hashCode();
        check = check * 31 + row.email.hashCode();
        check = check * 31 + row.phone.hashCode();
        return check;
    }

    private static long checksumOfRecord(EmployeeRecord row) {
        long check = 0;
        check = check * 31 + row.fullName().hashCode();
        check = check * 31 + row.birthDate().hashCode();
        check = check * 31 + row.salary().unscaledValue().longValue();
        check = check * 31 + row.gender().ordinal();
        check = check * 31 + row.passportSeries().hashCode();
        check = check * 31 + row.passportNumber().hashCode();
        check = check * 31 + row.email().hashCode();
        check = check * 31 + row.phone().hashCode();
        return check;
    }

    private static <T> ResultSetHandlerFactory<T> bytecodeFactory(Quirks quirks, Class<T> type) {
        final BytecodeResultSetHandlerFactoryBuilder builder = new BytecodeResultSetHandlerFactoryBuilder();
        builder.setQuirks(quirks);
        builder.setFallbackAllowed(false);
        return builder.newFactory(type);
    }

    private static <T> ResultSetHandlerFactory<T> reflectiveFactory(Quirks quirks, Class<T> type) {
        final DefaultResultSetHandlerFactoryBuilder builder = new DefaultResultSetHandlerFactoryBuilder();
        builder.setQuirks(quirks);
        return builder.newFactory(type);
    }

    /** The same values and the same value types a driver would have returned: strings, a date, a decimal. */
    private static void generateRows() {
        rows = new Object[ROWS][LABELS.length];
        for (int i = 0; i < ROWS; i++) {
            rows[i][0] = "Employee Name Surname " + i;
            rows[i][1] = java.sql.Date.valueOf(LocalDate.of(1970 + i % 50, 1 + i % 12, 1 + i % 28));
            rows[i][2] = new BigDecimal("100000." + i % 100);
            rows[i][3] = (i % 2 == 0 ? Gender.MALE : Gender.FEMALE).name();
            rows[i][4] = "45" + String.format("%02d", i % 100);
            rows[i][5] = String.format("%06d", i);
            rows[i][6] = "employee" + i + "@example.com";
            rows[i][7] = "+7-900-000-" + String.format("%04d", i);
        }
    }
}
