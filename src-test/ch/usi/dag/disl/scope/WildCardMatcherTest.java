package ch.usi.dag.disl.scope;

import org.junit.Assert;
import org.junit.Test;

public class WildCardMatcherTest {

    @Test
    public void emptyPatternMatchesOnlyEmptyString () {
        __testMatcher (
            "",
            __accept (""),
            __reject ("a", "A", "1", "!", "abc", "ABC", "123", "!@#")
        );
    }

    //

    @Test
    public void singleWildCardMatchesAnyString () {
        __assertMatch (
            true, "*",
            __accept ("", "a", "A", "1", "!", "abc", "ABC", "123", "!@#")
        );
    }

    @Test
    public void twoWildCardsMatchAnyString () {
        __assertMatch (
            true, "**",
            __accept ("", "a", "A", "1", "!", "abc", "ABC", "123", "!@#")
        );
    }

    //

    @Test
    public void trailingWildCardMatchesPrefix () {
        __testMatcher (
            "foo*",
            __accept ("foo", "fool", "fooBar", "foo123"),
            __reject ("", "!foo", "abcfoo", "123foo", "buffoon")
        );
    }

    @Test
    public void leadingWildCardMatchesSuffix () {
        __testMatcher (
            "*foo",
            __accept ("foo", "!foo", "abcfoo", "123foo"),
            __reject ("", "fool", "buffoon", "fooBar", "foo123")
        );
    }

    @Test
    public void middleWildCardMatchesPrefixAndSuffix () {
        __testMatcher (
            "foo*bar",
            __accept ("foobar", "foo!bar", "foo123bar", "foofoobarbar"),
            __reject ("", "foofoo", "barbar", "barfoo", "foo123", "123bar")
        );

        __testMatcher (
            "i*i",
            __accept ("ii", "iii", "iiii", "iiiii"),
            __reject ("ij", "ji", "iij", "jii", "ijij", "iijiij")
        );
    }

    @Test
    public void leadingAndTralingWildCardsMatchSubstring () {
        __testMatcher (
            "*foo*",
            __accept ("foo", "foo!", "!foo", "!foo!", "foo123", "123foo", "123foo123"),
            __reject ("", "f", "fo", "for", "o", "oo", "oof", "123")
        );
    }

    @Test
    public void leadingMiddleAndTrailingWildCardsMatchTwoSubstrings () {
        __testMatcher (
            "*foo*bar*",
            __accept (
                "foobar",
                "foobar!", "foo!bar", "foo!bar!", "!foobar", "!foobar!", "!foo!bar", "!foo!bar!",
                "foobar123", "foo123bar", "foo123bar123", "123foobar", "123foobar123", "123foo123bar", "123foo123bar123"
            ),
            __reject (
                "", "fbar", "fobar", "forbar", "foob", "fooba", "foobaz", "123",
                "barfoo",
                "barfoo!", "bar!foo", "bar!foo!", "!barfoo", "!barfoo!", "!bar!foo", "!bar!foo!",
                "barfoo123", "bar123foo", "bar123foo123", "123barfoo", "123barfoo123", "123bar123foo", "123bar123foo123"
            )
        );
    }

    //

    @Test
    public void simplePatternMatchesExactly() {
        __testMatcher (
            "i",
            __accept ("i"),
            __reject ("ii", "iii", "iiii", "ijji")
        );

        __testMatcher (
            "ii",
            __accept ("ii"),
            __reject ("i", "iii", "iiii", "iijjii")
        );

        __testMatcher (
            "iii",
            __accept ("iii"),
            __reject ("i", "ii", "iiii", "iiijiii")
        );
    }

    //

    private final void __testMatcher (final String pattern, final String [] positiveInputs, final String [] negativeInputs) {
        final WildCardMatcher m = WildCardMatcher.forPattern (pattern);
        if (positiveInputs != null) {
            __assertMatch (true, m, positiveInputs);
        }

        if (negativeInputs != null) {
            __assertMatch (false, m, negativeInputs);
        }
    }

    private final void __assertMatch (final boolean expected, final String pattern, final String ... inputs) {
        __assertMatch (expected, WildCardMatcher.forPattern (pattern), inputs);
    }

    private final void __assertMatch (final boolean expected, final WildCardMatcher m, final String ... inputs) {
        for (final String input : inputs) {
            Assert.assertEquals (
                String.format ("%s should%s match '%s'", m, expected ? "" : " not", input),
                expected, m.match (input));
        }
    }

    // Vocabulary

    private final String [] __accept (final String ... inputs) {
        return inputs;
    }

    private final String [] __reject (final String ... inputs) {
        return inputs;
    }

}
