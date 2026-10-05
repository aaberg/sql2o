package org.sql2o.quirks.parameterparsing.impl;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DefaultSqlParameterParsingStrategy}, which turns named parameters into question marks.
 *
 * <p>What matters is not only that parameters are replaced, but that a name that looks like one inside a string or
 * a comment is left alone.
 */
public class DefaultSqlParameterParsingStrategyTest {

    private final DefaultSqlParameterParsingStrategy strategy = new DefaultSqlParameterParsingStrategy();

    private String parse(String sql) {
        return strategy.parseSql(sql, new HashMap<String, List<Integer>>());
    }

    private Map<String, List<Integer>> parseWithMap(String sql) {
        final Map<String, List<Integer>> paramMap = new HashMap<>();
        strategy.parseSql(sql, paramMap);
        return paramMap;
    }

    @Test
    public void plainSqlIsUnchanged() {
        assertEquals("select id from some_table where id = 1", parse("select id from some_table where id = 1"));
    }

    @Test
    public void aNamedParameterBecomesAQuestionMarkAndIsRemembered() {
        final Map<String, List<Integer>> paramMap = new HashMap<>();

        final String parsed = strategy.parseSql("select * from t where id = :id", paramMap);

        assertEquals("select * from t where id = ?", parsed);
        assertEquals(List.of(1), paramMap.get("id"));
    }

    @Test
    public void theSameNameUsedTwiceKeepsBothPositions() {
        final Map<String, List<Integer>> paramMap = new HashMap<>();

        final String parsed = strategy.parseSql("select * from t where a = :x or b = :x", paramMap);

        assertEquals("select * from t where a = ? or b = ?", parsed);
        assertEquals(List.of(1, 2), paramMap.get("x"));
    }

    @Test
    public void differentNamesAreNumberedInTheOrderTheyAppear() {
        assertEquals(List.of(1), parseWithMap("select :second, :first").get("second"));
        assertEquals(List.of(2), parseWithMap("select :second, :first").get("first"));
    }

    @Test
    public void aParameterInsideASingleQuotedStringIsLeftAlone() {
        assertEquals("select ':notAParam', ?", parse("select ':notAParam', :yes"));
    }

    @Test
    public void aParameterInsideDoubleQuotesIsLeftAlone() {
        assertEquals("select \":notAParam\", ?", parse("select \":notAParam\", :yes"));
    }

    @Test
    public void aDoubleColonIsNotAParameter() {
        assertEquals("select a::b", parse("select a::b"));
    }

    @Test
    public void aParameterInsideALineCommentIsLeftAlone() {
        assertEquals("select 1 -- :notAParam\n, ?", parse("select 1 -- :notAParam\n, :yes"));
    }

    @Test
    public void aParameterInsideABlockCommentIsLeftAlone() {
        assertEquals("select /* :notAParam */ ?", parse("select /* :notAParam */ :yes"));
    }

    @Test
    public void aLineCommentRunsToTheEndOfTheLine() {
        assertEquals("select 1 -- a comment", parse("select 1 -- a comment"));
    }

    @Test
    public void aLineCommentWithoutANewlineRunsToTheEndOfTheStatement() {
        assertEquals("select 1 -- a comment", parse("select 1 -- a comment"));
    }

    @Test
    public void aBlockCommentRunsToItsEnd() {
        assertEquals("select /* a comment */ 1", parse("select /* a comment */ 1"));
    }

    @Test
    public void aBlockCommentSpanningLinesKeepsThem() {
        assertEquals("select /* one\ntwo */ 1", parse("select /* one\ntwo */ 1"));
    }

    /** An unterminated quote or comment means the statement itself is broken, but it must not hang or throw. */
    @Test
    public void anUnterminatedQuoteIsCopiedToTheEnd() {
        assertEquals("select 'abc", parse("select 'abc"));
    }

    @Test
    public void anUnterminatedBlockCommentIsCopiedToTheEnd() {
        assertEquals("select /* abc", parse("select /* abc"));
    }

    @Test
    public void anUnterminatedLineCommentIsCopiedToTheEnd() {
        assertEquals("select -- abc", parse("select -- abc"));
    }

    @Test
    public void aStarSlashInsideALineCommentDoesNotEndIt() {
        assertEquals("select -- a */ still comment", parse("select -- a */ still comment"));
    }

    /** The comment parsers look one character ahead, so a statement ending in a slash must not read past the end. */
    @Test
    public void aStatementEndingInASingleSlashIsFine() {
        assertEquals("select 1 /", parse("select 1 /"));
    }

    @Test
    public void aStatementEndingInASingleDashIsFine() {
        assertEquals("select 1 -", parse("select 1 -"));
    }

    @Test
    public void aStatementEndingInAColonIsFine() {
        assertEquals("select 1 :", parse("select 1 :"));
    }

    @Test
    public void anEmptyStatementStaysEmpty() {
        assertEquals("", parse(""));
    }

    /**
 * A character is only ever copied by the parser that claims it, so anything no parser accepts is dropped from the
 * statement, silently. With the default set that never happens, because DefaultParser claims everything and sits
 * last, but a subclass that hands out a shorter list can quietly turn a statement into an empty string. Worth
 * knowing before overriding getCharParsers.
 */
@Test
public void aCharacterNoParserClaimsIsDropped() {
        final DefaultSqlParameterParsingStrategy withoutCatchAll =
                new DefaultSqlParameterParsingStrategy() {
                    @Override
                    public CharParser[] getCharParsers(Map<String, List<Integer>> paramMap) {
                        return new CharParser[] {new QuoteParser()};
                    }
                };

        assertEquals("", withoutCatchAll.parseSql("select", new HashMap<String, List<Integer>>()));
        assertEquals("'a'", withoutCatchAll.parseSql("'a'", new HashMap<String, List<Integer>>()));
    }

    @Test
    public void theParsersAreOfferedInOrder() {
        final CharParser[] parsers = strategy.getCharParsers(new HashMap<String, List<Integer>>());

        assertTrue(parsers[0] instanceof QuoteParser);
        assertTrue(parsers[1] instanceof DoubleHyphensCommentParser);
        assertTrue(parsers[1 + 1] instanceof ForwardSlashCommentParser);
        assertTrue(parsers[3] instanceof ParameterParser);
        assertEquals(DefaultParser.class, parsers[4].getClass());
    }
}