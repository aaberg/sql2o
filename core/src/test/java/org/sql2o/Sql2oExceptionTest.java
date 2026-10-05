package org.sql2o;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Sql2oException}. Nothing in the library uses the message-less or the cause-only constructor, so
 * they are pinned here rather than left to chance.
 */
public class Sql2oExceptionTest {

    @Test
    public void withoutArgumentsTheExceptionIsEmpty() {
        final Sql2oException ex = new Sql2oException();

        assertNull(ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void aMessageIsKept() {
        assertEquals("something went wrong", new Sql2oException("something went wrong").getMessage());
    }

    @Test
    public void aMessageAndACauseAreBothKept() {
        final Exception cause = new IllegalStateException("the real problem");
        final Sql2oException ex = new Sql2oException("could not read the row", cause);

        assertEquals("could not read the row", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void aCauseOnItsOwnBecomesTheMessageToo() {
        final Exception cause = new IllegalStateException("the real problem");
        final Sql2oException ex = new Sql2oException(cause);

        assertSame(cause, ex.getCause());
        assertEquals("java.lang.IllegalStateException: the real problem", ex.getMessage());
    }

    @Test
    public void itIsUncheckedSoCallersAreNotForcedToCatchIt() {
        assertTrue(RuntimeException.class.isAssignableFrom(Sql2oException.class));
    }
}