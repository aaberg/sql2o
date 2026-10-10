package org.sql2o.quirks.parameterparsing.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the two comment parsers, which have to look at the character after the one they were called for.
 *
 * <p>Going through {@link DefaultSqlParameterParsingStrategy} for these leaves combinations untested, because the
 * strategy only calls them on characters that could plausibly start a comment.
 */
public class CommentParserTest {

    @Test
    public void aBlockCommentStartsWithSlashStar() {
        final ForwardSlashCommentParser parser = new ForwardSlashCommentParser();

        assertTrue(parser.canParse('/', "/*", 0));
        assertTrue(parser.canParse('/', "select /* x */", 7));
    }

    @Test
    public void aSlashFollowedByAnythingElseIsNotAComment() {
        final ForwardSlashCommentParser parser = new ForwardSlashCommentParser();

        assertFalse(parser.canParse('/', "/x", 0));
        assertFalse(parser.canParse('/', "//", 0));
    }

    @Test
    public void anotherCharacterIsNotACommentEvenWithAStarBehind() {
        final ForwardSlashCommentParser parser = new ForwardSlashCommentParser();

        assertFalse(parser.canParse('*', "*", 0));
        assertFalse(parser.canParse('x', "x*", 0));
    }

    @Test
    public void aSlashWithNothingBehindItIsNotAComment() {
        final ForwardSlashCommentParser parser = new ForwardSlashCommentParser();

        assertFalse(parser.canParse('/', "/", 0));
        assertFalse(parser.canParse('/', "", 0));
    }

    @Test
    public void aBlockCommentEndsOnASlashStar() {
        final ForwardSlashCommentParser parser = new ForwardSlashCommentParser();

        parser.init();

        assertFalse(parser.isEndComment('/'));
        assertFalse(parser.isEndComment('*'));
        // the star puts the parser one step away from the end
        assertTrue(parser.isEndComment('/'));
        // and it forgets again once the pair did not materialise
        assertFalse(parser.isEndComment('x'));
    }

    @Test
    public void aLineCommentStartsWithTwoDashes() {
        final DoubleHyphensCommentParser parser = new DoubleHyphensCommentParser();

        assertTrue(parser.canParse('-', "--", 0));
        assertTrue(parser.canParse('-', "select -- x", 7));
    }

    @Test
    public void aDashFollowedByAnythingElseIsNotAComment() {
        final DoubleHyphensCommentParser parser = new DoubleHyphensCommentParser();

        assertFalse(parser.canParse('-', "-x", 0));
        assertFalse(parser.canParse('-', "select - x", 7));
    }

    @Test
    public void anotherCharacterIsNotALineCommentEvenWithADashBehind() {
        final DoubleHyphensCommentParser parser = new DoubleHyphensCommentParser();

        assertFalse(parser.canParse(' ', " -", 0));
        assertFalse(parser.canParse('x', "x-", 0));
    }

    @Test
    public void aDashWithNothingBehindItIsNotAComment() {
        final DoubleHyphensCommentParser parser = new DoubleHyphensCommentParser();

        assertFalse(parser.canParse('-', "-", 0));
        assertFalse(parser.canParse('-', "", 0));
    }

    @Test
    public void aLineCommentEndsOnANewline() {
        final DoubleHyphensCommentParser parser = new DoubleHyphensCommentParser();

        assertFalse(parser.isEndComment('x'));
        assertTrue(parser.isEndComment('\n'));
    }
}