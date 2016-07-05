package ch.usi.dag.disl.scope;

import java.util.ArrayList;

import ch.usi.dag.disl.util.JavaNames;


/**
 * String string matcher with wild card support. Allows matching a string
 * against a pattern which can contain wild cards (*), each representing a
 * sequence of zero or more characters.
 * <p>
 * The algorithm works by first splitting the pattern into <i>cards</i> at the
 * wild card boundaries. The input text is checked for occurrence of each
 * <i>card</i>, with any missing <i>card</i> resulting in a mismatch. The
 * <i>cards</i> are checked for occurrence sequentially, and if a <i>card</i> is
 * present in the text, the next </i>card</i> is checked for occurrence in the
 * text that follows the last <i>card</i>.
 * <p>
 * This code evolved from the <a href=
 * "http://www.adarshr.com/simple-implementation-of-wildcard-text-matching-using-java"
 * >simple implementation</a> by Adarsh Ramamurthy. It has been modified so that
 * the pattern needs to match the whole string, not a substring, and to use a
 * starting index instead of substrings in the sequential card matching.
 *
 * @author Adarsh Ramamurthy
 * @author Marek Lukas
 * @author Lubomir Bulej
 */
abstract class WildCardMatcher {
    public static final String WILDCARD = "*";
    private static final int __WILDCARD_LENGTH__ = WILDCARD.length ();

    //

    private static final WildCardMatcher __matchAny__ = new WildCardMatcher (WILDCARD) {
        @Override
        public boolean match (final String text) {
            return true;
        };
    };

    private static final WildCardMatcher __matchEmpty__ = new WildCardMatcher ("") {
        @Override
        public boolean match (final String text) {
            return text.isEmpty ();
        };
    };

    //

    private static final class Textual extends WildCardMatcher {
        private Textual (final String text) {
            super (text);
        }

        @Override
        public boolean match (final String text) {
            return _pattern.equals (text);
        };
    }

    //

    private static final class Generic extends WildCardMatcher {
        private final String [] __cards;
        private final String __prefixCard;
        private final String __suffixCard;

        private Generic (final String pattern, final ArrayList <String> cards) {
            super (pattern);

            __prefixCard = cards.get (0);
            __suffixCard = cards.get (cards.size () - 1);
            __cards = cards.stream ()
                .filter (s -> !s.isEmpty ())
                .toArray (size -> new String [size]);
        }


        @Override
        public boolean match (final String text) {
            int regionFromInclusive = 0;
            int regionToExclusive = text.length ();

            int cardsFromInclusive = 0;
            if (!__prefixCard.isEmpty ()) {
                // The first card should match the beginning.
                if (!text.startsWith (__prefixCard)) {
                    return false;
                }

                regionFromInclusive += __prefixCard.length ();
                cardsFromInclusive++;
            }

            int cardsToExclusive = __cards.length;
            if (!__suffixCard.isEmpty ()) {
                // The last card should match the end.
                if (!text.endsWith (__suffixCard)) {
                    return false;
                }

                regionToExclusive -= __suffixCard.length ();
                cardsToExclusive--;
            }

            //
            // If the first and last cards are the same, they must overlap
            // on the whole input.
            //
            if (cardsFromInclusive > cardsToExclusive) {
                return (regionToExclusive - regionFromInclusive) == -text.length ();
            }

            //
            // Otherwise try to match the pattern to the input. All cards from
            // the pattern must be found in the text, in the correct order, and
            // without overlaps.
            //
            final String region = text.substring (
                regionFromInclusive, regionToExclusive
            );

            int fromIndex = 0;
            while (cardsFromInclusive < cardsToExclusive) {
                final String card = __cards [cardsFromInclusive];
                final int cardMatchIndex = region.indexOf (card, fromIndex);
                if (cardMatchIndex < 0) {
                    return false;
                }

                fromIndex = cardMatchIndex + card.length ();
                cardsFromInclusive++;
            }

            return true;
        }
    };

    //

    protected final String _pattern;

    private WildCardMatcher (final String pattern) {
        _pattern = pattern;
    }


    /**
     * @return The pattern associated with this matcher.
     */
    public String pattern () {
        return _pattern;
    }


    @Override
    public String toString () {
        return String.format ("%s[%s]", JavaNames.simpleClassName (this), _pattern);
    }


    /**
     * @param text
     *        the text to match.
     * @return {@code true} if the input text matches the pattern associated
     *         with this matcher, {@code false} otherwise.
     */
    public abstract boolean match (final String text);

    //

    /**
     * Creates a matcher for the given pattern. An empty pattern will only match
     * empty text.
     *
     * @param pattern
     *        the pattern to create a matcher for.
     * @return a new {@link WildCardMatcher} for the given pattern.
     */
    public static WildCardMatcher forPattern (final String pattern) {
        assert pattern != null;

        //

        if (pattern.isEmpty ()) {
            return __matchEmpty__;

        } else if (pattern.equals (WILDCARD)) {
            return __matchAny__;

        } else if (!pattern.contains (WILDCARD)) {
            return new Textual (pattern);

        } else {
            final ArrayList <String> cards = __split (pattern);
            return new Generic (pattern, cards);
        }
    }


    private static ArrayList <String> __split (final String pattern) {
        assert !pattern.isEmpty ();

        final ArrayList <String> result = new ArrayList <> (5);
        final int charCount = pattern.length ();

        //
        // If the string starts or ends with a wildcard, an empty card needs to
        // be added to the list at the beginning or at the end, respectively.
        //
        int fromIndex = 0;
        while (fromIndex <= charCount) {
            final int position = pattern.indexOf (WILDCARD, fromIndex);
            if (position >= 0) {
                result.add (pattern.substring (fromIndex, position));
                fromIndex = position + __WILDCARD_LENGTH__;

            } else {
                // The last card -- may be empty if string ends with wildcard.
                result.add (pattern.substring (fromIndex));
                fromIndex = charCount + 1;
            }
        }

        return result;
    }


    /**
     * Performs a wild card matching for the given text and pattern. The pattern
     * can contain multiple wild card (*) characters representing a sequence of
     * zero or more characters.
     *
     * @param text
     *        the text to be tested for matches.
     * @param pattern
     *        the pattern to be matched for.
     * @return {@code true} if the pattern matches the text, {@code false}
     *         otherwise.
     */
    public static boolean match (final String text, final String pattern) {
        return forPattern (pattern).match (text);
    }

}