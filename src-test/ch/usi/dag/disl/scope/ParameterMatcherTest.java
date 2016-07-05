package ch.usi.dag.disl.scope;

import org.junit.Assert;
import org.junit.Test;


public class ParameterMatcherTest {

    @Test
    public void emptyPatternMatchesOnlyNoParameters () {
        __testMatcher (
            "",
            __accept ("()V", "()Ljava/lang/String;"),
            __reject ("(I)V", "(II)V", "(Ljava/lang/String;)I")
        );
    }

    //

    @Test
    public void singleWildCardMatchesAnyParameters () {
        __assertMatch (
            true, "..",
            __accept (
                "()V", "(I)V", "(LObject;)V",
                "(II)V", "(ILObject;)V", "(LObject;I)V",
                "(ILObject;I)V"
            )
        );
    }

    @Test
    public void leadingWildCardAndAnyTrailingParameterMatchAtLeastOneParameter () {
        __testMatcher (
            "..,*",
            __accept (
                "(I)V", "(LObject;)V",
                "(II)V", "(ILObject;)V", "(LObject;I)V", "(LObject;LObject;)V",
                "(ILObject;I)V", "(LObject;ILObject;)V"
            ),
            __reject ("()V")
        );
    }

    @Test
    public void trailingWildCardAndAnyLeadingParameterMatchAtLeastOneParameter () {
        __testMatcher (
            "*,..",
            __accept (
                "(I)V", "(LObject;)V",
                "(II)V", "(ILObject;)V", "(LObject;I)V", "(LObject;LObject;)V",
                "(ILObject;I)V", "(LObject;ILObject;)V"
            ),
            __reject ("()V")
        );
    }

    @Test
    public void twoWildCardsMatchAnyParameters () {
        __assertMatch (
            true, "..,..",
            __accept (
                "()V", "(I)V", "(LObject;)V",
                "(II)V", "(ILObject;)V", "(LObject;I)V",
                "(ILObject;I)V"
            )
        );
    }

    //

    @Test
    public void trailingWildCardMatchesPrefixSequence () {
        __testMatcher (
            "int,..",
            __accept ("(I)V", "(II)V", "(ILjava/lang/String;J)V"),
            __reject ("()V", "(JI)V", "(Ljava/lang/String;I)V")
        );

        __testMatcher (
            "[default].String,..",
            __accept ("(LString;)V", "(LString;I)V", "(LString;Ljava/lang/String;J)V"),
            __reject ("()V", "(JLString;)V", "(Ljava/lang/String;LString;)V")
        );

        __testMatcher (
            "*String,..",
            __accept ("(LString;)V", "(LSpecialString;I)V", "(Lspecial/String;Ljava/lang/String;J)V"),
            __reject ("()V", "(JLString;)V", "(Lutil/StringBuilder;LString;)V")
        );
    }

    @Test
    public void leadingWildCardMatchesSuffixSequence () {
        __testMatcher (
            "..,int",
            __accept ("(I)V", "(II)V", "(JLjava/lang/String;I)V"),
            __reject ("()V", "(IJ)V", "(ILjava/lang/String;)V")
        );

        __testMatcher (
            "..,[default].String",
            __accept ("(LString;)V", "(ILString;)V", "(Ljava/lang/String;JLString;)V"),
            __reject ("()V", "(LString;J)V", "(LString;Ljava/lang/String;)V")
        );

        __testMatcher (
            "..,*String",
            __accept ("(LString;)V", "(ILSpecialString;)V", "(Ljava/lang/String;JLspecial/String;)V"),
            __reject ("()V", "(LString;J)V", "(LString;Lutil/StringBuilder;)V")
        );
    }

    @Test
    public void middleWildCardMatchesPrefixAndSuffixSequences () {
        __testMatcher (
            "int,..,long",
            __accept ("(IJ)V", "(IZJ)V", "(IJIJ)", "(ILjava/lang/String;J)V"),
            __reject ("()V", "(II)V", "(JJ)V", "(JI)V", "(IJJI)V", "(JIIJ)V", "(ILjava/lang/String;)V", "(Ljava/lang/String;J)V")
        );

        __testMatcher (
            "[default].String,..,Object",
            __accept ("(LString;Ljava/lang/Object;)V", "(LString;ILjava/lang/Object;)V", "(LString;Ljava/lang/String;LObject;)V"),
            __reject ("()V", "(LString;LString;)V", "(LObject;LObject;)V", "(LObject;LString;)V", "(LString;Ljava/lang/String;)V")
        );

        __testMatcher (
            "*String,..,*Object",
            __accept (
                "(LString;LObject;)V", "(Lspecial/String;ILspecial/Object;)V",
                "(LSpecialString;Lutil/StringBuilder;LSpecialObject;)V"
            ),
            __reject (
                "()V", "(Lspecial/String;LSpecialString;)V",
                "(Lspecial/Object;LSpecialObject;)V", "(LSpecialObject;LString;)V",
                "(LString;Lutil/HashMap;)V", "(Lutil/HashMap;LObject;)V"
            )
        );
    }

    @Test
    public void leadingAndTralingWildCardsMatchParameterSequence() {
        __testMatcher (
            "..,int,..",
            __accept (
                "(I)V", "(IJLjava/lang/String;)V", "(JLjava/lang/String;I)V",
                "(JILlava/lang/String;)V"
            ),
            __reject ("()V", "(J)V", "(Ljava/lang/String;)V")
        );

        __testMatcher (
            "..,[default].String,..",
            __accept (
                "(LString;)V", "(LString;JLignore/String;)V",
                "(Lignore/String;JLString;)V", "(Lignore/String;LString;J)V"
            ),
            __reject ("()V", "(Lignore/String;J)V", "(JLjava/lang/String;)V")
        );

        __testMatcher (
            "..,*String,..",
            __accept (
                "(LString;)V", "(LSpecialString;JLutil/StringBuilder;)V",
                "(Lutil/StringBuilder;JLspecial/String;)V",
                "(LStringBuilder;Lspecial/String;J)V"
            ),
            __reject ("()V", "(LStringBuilder;J)V", "(JLutil/StringBuilder;)V")
        );
    }

    @Test
    public void leadingMiddleAndTrailingWildCardsMatchTwoParameterSequences () {
        __testMatcher (
            "..,int,..,long,..",
            __accept ("(IJ)V", "(ZIZJZ)V", "(IJIJ)", "(IIILObject;JJJ)V"),
            __reject ("()V", "(II)V", "(JJ)V", "(JI)V", "(JJII)V", "(JZZI)V")
        );

        __testMatcher (
            "..,int,long,..,float,double,..",
            __accept ("(IJFD)V", "(ZIJLObject;FDZ)V", "(IIJJFFDD)V"),
            __reject ("()V", "(FDIJ)V", "(IFJD)V", "(IZJLObject;FZD)V")
        );
    }

    //

    @Test
    public void matchExactParameters () {
        __testMatcher (
            "int, int, int",
            __accept ("(III)V"),
            __reject ("()V", "(II)V", "(IIII)V", "(IIIJIII)V")
        );
    }

    //

    private final void __testMatcher (final String pattern, final String [] positiveInputs, final String [] negativeInputs) {
        final ParameterMatcher m = ParameterMatcher.forPattern (pattern);
        if (positiveInputs != null) {
            __assertMatch (true, m, positiveInputs);
        }

        if (negativeInputs != null) {
            __assertMatch (false, m, negativeInputs);
        }
    }

    private final void __assertMatch (final boolean expected, final String pattern, final String ... inputs) {
        __assertMatch (expected, ParameterMatcher.forPattern (pattern), inputs);
    }

    private final void __assertMatch (final boolean expected, final ParameterMatcher m, final String ... inputs) {
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
