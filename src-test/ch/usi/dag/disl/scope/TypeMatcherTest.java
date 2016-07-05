package ch.usi.dag.disl.scope;

import org.junit.Assert;
import org.junit.Test;

public class TypeMatcherTest {

    @Test
    public void wildCardMatchesAnyType () {
        __assertMatch (true, "*", __accept ("", "V", "Ljava/lang/String;"));
    }

    @Test
    public void emptyPatternMatchesNoType () {
        __assertMatch (false, "", __reject ("", "V", "Ljava/lang/String;"));
    }

    //

    @Test
    public void matchPrimitiveType () {
        final String [] negativeInputs = __reject (
            "", "V", "Ljava/lang/String;"
        );

        __testMatcher ("boolean", __accept ("Z"), negativeInputs);
        __testMatcher ("byte", __accept ("B"), negativeInputs);
        __testMatcher ("char", __accept ("C"), negativeInputs);
        __testMatcher ("short", __accept ("S"), negativeInputs);
        __testMatcher ("int", __accept ("I"), negativeInputs);
        __testMatcher ("float", __accept ("F"), negativeInputs);
        __testMatcher ("long", __accept ("J"), negativeInputs);
        __testMatcher ("double", __accept ("D"), negativeInputs);
    }

    @Test
    public void matchFullyQualifiedObjectType () {
        __testMatcher (
            "java.lang.String",
            __accept ("Ljava/lang/String;"),
            __reject ("", "V", "Ljava/lang/Object;")
        );
    }

    @Test
    public void matchDefaultPackageObjectType () {
        __testMatcher (
            "[default].Class",
            __accept ("LClass;"),
            __reject ("", "V", "Ljava/lang/Object;")
        );
    }

    @Test
    public void matchAnyOneOrMoreDimensionalArrayType() {
        __testMatcher (
            "*[]",
            __accept ("[I", "[[I", "[[[LString;"),
            __reject ("I", "LString;")
        );
    }

    @Test
    public void matchAnyTwoOrMoreDimensionalArrayType() {
        __testMatcher (
            "*[][]",
            __accept ("[[I", "[[[I", "[[[[LString;"),
            __reject ("I", "[I", "[LString;")
        );
    }

    //

    @Test
    public void matchClassNameSuffixInAnyPackage () {
        __testMatcher (
            "*String",
            __accept (
                "LString;", "LDefaultString;", "Ljava/lang/String;",
                "Lmy/special/MutableString;"
            ),
            __reject (
                "LStringer;", "LDefaultStringer;", "Ljava/lang/Stringifier;",
                "Ljava/util/StringBuilder"
            )
        );
    }

    @Test
    public void matchClassNameInAnyPackage() {
        __testMatcher (
            "*.String",
            __accept (
                "LString;", "Lspecial/String;", "Ljava/lang/String;"
            ),
            __reject (
                "LDefaultString;", "LStringBuilder;",
                "Lspecial/DefaultString;", "Ljava/util/StringBuilder;"
            )
        );
    }


    //

    private final void __testMatcher (final String pattern, final String [] positiveInputs, final String [] negativeInputs) {
        final TypeMatcher m = TypeMatcher.forPattern (pattern);
        if (positiveInputs != null) {
            __assertMatch (true, m, positiveInputs);
        }

        if (negativeInputs != null) {
            __assertMatch (false, m, negativeInputs);
        }
    }

    private final void __assertMatch (final boolean expected, final String pattern, final String ... inputs) {
        __assertMatch (expected, TypeMatcher.forPattern (pattern), inputs);
    }

    private final void __assertMatch (final boolean expected, final TypeMatcher m, final String ... inputs) {
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
