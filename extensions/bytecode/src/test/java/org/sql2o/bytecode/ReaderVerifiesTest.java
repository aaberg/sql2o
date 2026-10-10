package org.sql2o.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.quirks.NoQuirks;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every class this extension emits has to pass the verifier, checked here rather than when a row arrives.
 *
 * <p>A reader that does not verify is a {@link VerifyError} thrown by the class loader on the first query of that shape,
 * with a message about a stack that points at nothing a caller wrote. Finding it here instead means a failing test names
 * the fixture that produced it.
 */
public class ReaderVerifiesTest {

    public static class PublicFields {
        public int number;
        public String text;
        public boolean flag;
    }

    public static class WithAccessors {
        private int number;
        private String text;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public record Primitives(int number, long big, double fraction, boolean flag, String text) {
    }

    public record OnlyReferences(String text, Object anything) {
    }

    static class PackagePrivate {
        String value;

        PackagePrivate() {
        }
    }

    public static class Wide {
        public String c0;
        public String c1;
        public String c2;
        public String c3;
        public String c4;
        public String c5;
        public String c6;
        public String c7;
    }

    @Test
    public void aClassOfPublicFieldsVerifies() {
        verify(PublicFields.class, "number", "text", "flag");
    }

    @Test
    public void aClassWithSettersVerifies() {
        verify(WithAccessors.class, "number", "text");
    }

    @Test
    public void aClassThatIsNotPublicVerifies() {
        verify(PackagePrivate.class, "value");
    }

    @Test
    public void aRecordOfPrimitivesAndReferencesVerifies() {
        verify(Primitives.class, "number", "big", "fraction", "flag", "text");
    }

    @Test
    public void aRecordOfReferencesOnlyVerifies() {
        verify(OnlyReferences.class, "text", "anything");
    }

    @Test
    public void aWideShapeVerifies() {
        verify(Wide.class, "c0", "c1", "c2", "c3", "c4", "c5", "c6", "c7");
    }

    /**
     * Compiles a shape and verifies the bytes that were defined, so that a reader which would not verify fails
     * here rather than at the point of use.
     */
    private static void verify(Class<?> type, String... columns) {
        final Settings settings = new Settings(new NamingConvention(false, false), new NoQuirks(), true);
        final RowPlanCompiler compiler = new RowPlanCompiler(type, settings, columns);
        final RowPlanCompiler.Outcome outcome = compiler.compile(java.util.Map.of());

        // A refusal is the one outcome with no bytes to verify, and it is a valid answer rather than a defect.
        assertTrue(outcome.compiled(), "not compiled: " + outcome.refusal());

        final StringWriter report = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(outcome.bytes()), false, new PrintWriter(report));
        assertTrue(report.toString().isEmpty(), () -> "the reader does not verify:\n" + report);
    }
}
