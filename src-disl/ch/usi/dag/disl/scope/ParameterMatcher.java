package ch.usi.dag.disl.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.util.JavaNames;


/**
 * Method parameter matcher. Allows checking a method descriptor against a
 * parameter pattern. The pattern list can contain multiple wild card (..)
 * elements which represent zero or more parameters of any type. Conceptually,
 * this is a variant of the {@link WildCardMatcher} applied to sequences of
 * method parameters instead of characters.
 *
 * @author Lubomir Bulej
 */
abstract class ParameterMatcher {
    public static final String WILDCARD = "..";

    //

    private static final ParameterMatcher __matchAnyParams__ = new ParameterMatcher (WILDCARD) {
        @Override
        public boolean match (final String methodDesc) {
            return true;
        };
    };

    private static final ParameterMatcher __matchZeroParams__ = new ParameterMatcher ("") {
        private static final String __NO_PARAMETERS__ = "()";

        @Override
        public boolean match (final String methodDesc) {
            return methodDesc.startsWith (__NO_PARAMETERS__);
        };
    };

    //

    private static final class Generic extends ParameterMatcher {
        private final TypeMatcher [][] __cards;
        private final TypeMatcher [] __prefixCard;
        private final TypeMatcher [] __suffixCard;

        private Generic (
            final String pattern, final List <TypeMatcher []> cards
        ) {
            super (pattern);

            __prefixCard = cards.get (0);
            __suffixCard = cards.get (cards.size () - 1);
            __cards = cards.stream ()
                .filter (a -> a.length > 0)
                .toArray (size -> new TypeMatcher [size][]);
        }

        //

        @Override
        public boolean match (final String methodDesc) {
            final Type [] params = Type.getArgumentTypes (methodDesc);

            int paramsFromInclusive = 0;
            int paramsToExclusive = params.length;

            int cardsFromInclusive = 0;
            if (__prefixCard.length > 0) {
                // The first card should match at the beginning.
                if (!__matchCard (__prefixCard, params, 0, paramsToExclusive)) {
                    return false;
                }

                paramsFromInclusive += __prefixCard.length;
                cardsFromInclusive++;
            }

            int cardsToExclusive = __cards.length;
            if (__suffixCard.length > 0) {
                // The last card should match at the end
                final int tailFromInclusive = Math.max (0, paramsToExclusive - __suffixCard.length);
                if (!__matchCard (__suffixCard, params, tailFromInclusive, paramsToExclusive)) {
                    return false;
                }

                paramsToExclusive -= __suffixCard.length;
                cardsToExclusive--;
            }

            //
            // If the first and last cards are the same, they must overlap
            // on all parameters.
            //
            if (cardsFromInclusive > cardsToExclusive) {
                return (paramsToExclusive - paramsFromInclusive) == -params.length;
            }

            //
            // Otherwise try to match the pattern to the input. All parameter
            // cards must be found in the method parameters, in the correct
            // order, and without overlaps.
            //
            while (cardsFromInclusive < cardsToExclusive) {
                final TypeMatcher [] card = __cards [cardsFromInclusive];

                final int cardMatchIndex = __indexOf (card, params, paramsFromInclusive, paramsToExclusive);
                if (cardMatchIndex < 0) {
                    return false;
                }

                paramsFromInclusive = cardMatchIndex + card.length;
                cardsFromInclusive++;
            }

            return true;
        }

        private boolean __matchCard (
            final TypeMatcher [] card, final Type [] params,
            final int fromInclusive, final int toExclusive
        ) {
            if (card.length > (toExclusive - fromInclusive)) {
                // not enough parameters to match the card
                return false;
            }

            int paramIndex = fromInclusive;
            for (final TypeMatcher matcher : card) {
                final Type param = params [paramIndex++];
                if (!matcher.match (param.getDescriptor ())) {
                    return false;
                }
            }

            return true;
        }

        private int __indexOf (
            final TypeMatcher [] card, final Type [] params,
            final int fromInclusive, final int toExclusive
        ) {
            for (int index = fromInclusive; index < toExclusive; index++) {
                if (__matchCard (card, params, index, toExclusive)) {
                    return index;
                }
            }

            return -1;
        }

    }

    //

    private final String __pattern;

    private ParameterMatcher (final String pattern) {
        __pattern = pattern;
    }


    /**
     * @return The parameter pattern associated with this matcher.
     */
    public String pattern () {
        return __pattern;
    }


    @Override
    public String toString () {
        return String.format ("%s[(%s)]", JavaNames.simpleClassName (this), pattern ());
    }


    /**
     * @param methodDesc
     *        the method descriptor to test for match, may not be {@code null}.
     * @return {@code true} if the given method descriptor matches the pattern
     *         represented by this matcher, {@code false} otherwise.
     */
    public abstract boolean match (final String methodDesc);

    //

    /**
     * Creates a matcher for the given parameter pattern. The pattern can
     * contain multiple wild card (..) elements representing a sequence of zero
     * or more parameters of any type. An empty pattern will only match
     * descriptors for methods with no parameters.
     *
     * @param pattern
     *        the pattern to create the matcher for, may not be {@code null}.
     * @return a new {@link ParameterMatcher} for the given pattern.
     */
    public static ParameterMatcher forPattern (final String pattern) {
        final String trimmed = Objects.requireNonNull (pattern).trim ();

        //

        if (trimmed.isEmpty ()) {
            return __matchZeroParams__;

        } else if (trimmed.equals (WILDCARD)) {
            return __matchAnyParams__;

        } else {
            return new Generic (trimmed, __split (trimmed));
        }
    }


    private static final Pattern __splitter__ = Pattern.compile ("\\s*,\\s*");

    private static List <TypeMatcher []> __split (final String pattern) {
        assert !pattern.isEmpty ();

        final List <TypeMatcher []> result = new ArrayList <> ();
        final List <TypeMatcher> card = new ArrayList <> ();

        __splitter__.splitAsStream (pattern)
            .map (String::trim)
            .map (s -> s.isEmpty () ? WildCardMatcher.WILDCARD : s)
            .forEachOrdered (s -> {
                if (!s.equals (WILDCARD)) {
                    card.add (TypeMatcher.forPattern (s));

                } else {
                    // flush current card
                    result.add (card.toArray (new TypeMatcher [card.size ()]));
                    card.clear ();
                }
            });

        // flush last card
        result.add (card.toArray (new TypeMatcher [card.size ()]));
        return result;
    }

}