package ch.usi.dag.disl.scope;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.PotentialAssignment;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;


@RunWith (Theories.class)
public class ScopeMatcherTest {

    // smoke tests

    @Test
    public void testSimple() {
        __assertMatch (
            true, "my.pkg.TargetClass.main()",
            __method ("my/pkg/TargetClass", "main", "()V")
        );
    }

    @Test
    public void testComplete() {
        __assertMatch (
            true, "java.lang.String my.pkg.TargetClass.main(java.lang.String[])",
            __method ("my/pkg/TargetClass", "main", "([Ljava/lang/String;)Ljava/lang/String;")
        );
    }

    // method tests

    @Test
    public void testMethodWildCard() {
        __assertMatch (
            true, "java.lang.String my.pkg.TargetClass.*main(java.lang.String[])",
            __method ("my/pkg/TargetClass", "blablablamain", "([Ljava/lang/String;)Ljava/lang/String;")
        );
    }

    @Test
    public void testMethodAllWildCard() {
        __assertMatch (
            true, "my.pkg.TargetClass.*",
            __method ("my/pkg/TargetClass", "clinit", "()V"),
            __method ("my/pkg/TargetClass", "init", "()V"),
            __method ("my/pkg/TargetClass", "method_init", "()V")
        );
    }

    @Test
    public void testMethodInitWildCard() {
        __assertMatch (
            true, "my.pkg.TargetClass.*init",
            __method ("my/pkg/TargetClass", "clinit", "()V"),
            __method ("my/pkg/TargetClass", "init", "()V"),
            __method ("my/pkg/TargetClass", "method_init", "()V")
        );
    }

    // return tests

    @Test
    public void matchIntPrimitiveReturnType() {
        __testMatcher (
            "int my.pkg.TargetClass.method",
            __accept (__method ("my/pkg/TargetClass", "method", "()I")),
            __reject (__method ("my/pkg/TargetClass", "method", "()V"))
        );
    }

    @Test
    public void matchAnyReturnTypeUsingWildcard() {
        __assertMatch (
            true, "* my.pkg.TargetClass.method",
            __method ("my/pkg/TargetClass", "method", "()V"),
            __method ("my/pkg/TargetClass", "method", "()I"),
            __method ("my/pkg/TargetClass", "method", "()Ljava/lang/String;")
        );
    }

    @Test
    public void matchAnyReturnTypeUsingEmptyReturnType() {
        __assertMatch (
            true, "my.pkg.TargetClass.method",
            __method ("my/pkg/TargetClass", "method", "()V"),
            __method ("my/pkg/TargetClass", "method", "()I"),
            __method ("my/pkg/TargetClass", "method", "()Ljava/lang/String;")
        );
    }

    @Test
    public void matchAnyArrayReturnTypeWithAtLeastOneDimension() {
        __testMatcher (
            "*[] my.pkg.TargetClass.method",
            __accept (
                __method ("my/pkg/TargetClass", "method", "()[I"),
                __method ("my/pkg/TargetClass", "method", "()[[I"),
                __method ("my/pkg/TargetClass", "method", "()[Lmy/package/String;"),
                __method ("my/pkg/TargetClass", "method", "()[[Lmy/package/String;")
            ),
            __reject (
                __method ("my/pkg/TargetClass", "method", "()I"),
                __method ("my/pkg/TargetClass", "method", "()Ljava/lang/String;")
            )
        );
    }

    @Test
    public void matchAnyArrayReturnTypeWithAtLeastTwoDimensions() {
        __testMatcher (
            "*[][] my.pkg.TargetClass.method",
            __accept (
                __method ("my/pkg/TargetClass", "method", "()[[I"),
                __method ("my/pkg/TargetClass", "method", "()[[Lmy/package/String;")
            ),
            __reject (
                __method ("my/pkg/TargetClass", "method", "()I"),
                __method ("my/pkg/TargetClass", "method", "()[I"),
                __method ("my/pkg/TargetClass", "method", "()Ljava/lang/String;"),
                __method ("my/pkg/TargetClass", "method", "()[Lmy/package/String;")
            )
        );
    }

    @Test
    public void testReturnStartSepStringWildCard() {
        __assertMatch (
            true, "*.String my.pkg.TargetClass.method",
            __method ("my/pkg/TargetClass", "method", "()Ljava/lang/String;"),
            __method ("my/pkg/TargetClass", "method", "()Lmy/package/String;")
        );
    }

    @Test
    public void testReturnStartNoSepStringWildCard() {
        __assertMatch (
            true, "*String my.pkg.TargetClass.method",
            __method ("my/pkg/TargetClass", "method", "()Ljava/lang/String;"),
            __method ("my/pkg/TargetClass", "method", "()Lmy/package/String;"),
            __method ("my/pkg/TargetClass", "method", "()Lmy/package/BigString;")
        );
    }

    // classname tests

    /**
     * FIXED
     *
     * input:
     * java.lang.String main()
     *
     * result:
     * r=null c=java.lang.String m=main p=()
     *
     * correct:
     * r=java.lang.String c=* m=main p=()
     */
    @Test
    public void matchMethodWithoutClassName() {
        __assertMatch (
            true, "java.lang.String main()",
            __method ("my/pkg/TargetClass", "main", "()Ljava/lang/String;")
        );
    }

    /**
     * FIXED
     *
     * input:
     * java.*.String main()
     *
     * result:
     * r=null c=java.* m=String main p=()
     *
     * correct:
     * r=java.*.String c=* m=main p=()
     */
    @Test
    public void matchMethodWithoutClassNameWithReturnTypeWildcard() {
        __assertMatch (
            true, "java.*.String main()",
            __method ("my/pkg/TargetClass", "main", "()Ljava/special/String;")
        );
    }

    @Test
    public void testClassAllPackages() {
        __assertMatch (
            true, "TargetClass.method",
            __method ("TargetClass", "method", "()V"),
            __method ("my/pkg/TargetClass", "method", "()V")
        );
    }

    @Test
    public void testClassDefaultPackage() {
        __testMatcher (
            "[default].TargetClass.method",
            __accept (__method ("TargetClass", "method", "()V")),
            __reject (__method ("my/pkg/TargetClass", "method", "()V"))
        );
    }

    @Test
    public void testClassWildCard() {
        __assertMatch (
            true, "my.pkg.*TargetClass.method",
            __method ("my/pkg/TargetClass", "method", "()V"),
            __method ("my/pkg/pkg/TargetClass", "method", "()V"),
            __method ("my/pkg/AnotherTargetClass", "method", "()V"),
            __method ("my/pkg/pkg/AnotherTargetClass", "method", "()V")
        );
    }

    // parameter tests

    @Test
    public void testParameterAllRandom() {
        __assertMatch (
            true, "my.pkg.TargetClass.method(..)",
            __method ("my/pkg/TargetClass", "method", "()V"),
            __method ("my/pkg/TargetClass", "method", "(I)V"),
            __method ("my/pkg/TargetClass", "method", "([I)V"),
            __method ("my/pkg/TargetClass", "method", "([Ljava.lang.String;[I[I[I)V")
        );
    }

    @Test
    public void matchMethodWithNoParameters() {
        __testMatcher (
            "my.pkg.TargetClass.method()",
            __accept (__method ("my/pkg/TargetClass", "method", "()V")),
            __reject (
                __method ("my/pkg/TargetClass", "method", "(I)V"),
                __method ("my/pkg/TargetClass", "method", "([I)V"),
                __method ("my/pkg/TargetClass", "method", "([Ljava.lang.String;[I[I[I)V")
            )
        );
    }

    /**
     * FIXED
     *
     * details:
     * (int, int, int, ..) should not match (I)V
     */
    @Test
    public void testParameterEndRandom() {
        __testMatcher (
            "my.pkg.TargetClass.method(int, int, int, ..)",
            __accept (
                __method ("my/pkg/TargetClass", "method", "(III)V"),
                __method ("my/pkg/TargetClass", "method", "(IIII)V")
            ),
            __reject (
                __method ("my/pkg/TargetClass", "method", "(I)V"),
                __method ("my/pkg/TargetClass", "method", "(II)V"),
                __method ("my/pkg/TargetClass", "method", "(JIII)V")
            )
        );
    }

    // complete tests

    @Test
    public void testCompleteAllReturnPattern() {
        __assertMatch (
            true, "int *",
            __method ("my/pkg/TargetClass", "method", "()I"),
            __method ("my/pkg/TargetClass", "method", "(I)I"),
            __method ("my/pkg/TargetClass", "method", "(Ljava.lang.String;)I"),
            __method ("TargetClass", "method", "()I")
        );
    }

    @Test
    public void testCompleteAllAcceptPattern() {
        __testMatcher (
            "*(int, int, int)",
            __accept (
                __method ("TargetClass", "method", "(III)I"),
                __method ("my/pkg/TargetClass", "method", "(III)V")
            ),
            __reject (
                __method ("my/pkg/TargetClass", "method", "(II)I"),
                __method ("my/pkg/TargetClass", "method", "(IIII)I"),
                __method ("my/pkg/TargetClass", "method", "(Ljava.lang.String;)I")
            )
        );
    }

    @Test(expected=MalformedScopeException.class)
    public void throwOnEmptyScope() throws MalformedScopeException {
        ScopeMatcher.forPattern ("");
    }

    @Test
    public void matchScopeWithClassWildcard() {
        __assertMatch (
            true, "*.foo*",
            __method ("my/pkg/TargetClass", "foo", "()V")
        );
    }

    @Test(expected=MalformedScopeException.class)
    public void throwOnMissingClassNameWhenExpected() {
        ScopeMatcher.forPattern(".foo*");
    }

    //

    @Rule
    public ExpectedException exception = ExpectedException.none ();

    //

    public static class EmptyMethodSupplier extends ParameterSupplier {
        @Override
        public List <PotentialAssignment> getValueSources (final ParameterSignature sig) {
            return __stringAssignments (
                "my.pkg.Class.",
                "my.pkg.Class.()",
                "* my.pkg.Class.()",
                "* my.pkg.Class.(..)"
            );
        }
    }

    @Theory
    public void throwOnMissingMethodName (
        @ParametersSuppliedBy (EmptyMethodSupplier.class) final String pattern
    ) {
        exception.expect (MalformedScopeException.class);
        ScopeMatcher.forPattern (pattern);
    }

    //

    public static class MisplacedParenthesisSupplier extends ParameterSupplier {
        @Override
        public List <PotentialAssignment> getValueSources (final ParameterSignature sig) {
            return __stringAssignments (
                "my.pkg.Class.method(",
                "my.pkg.Class.method)",
                "my.pkg.Class.method)("
            );
        }
    }

    @Theory
    public void throwOnMisplacedParenthesis (
        @ParametersSuppliedBy (MisplacedParenthesisSupplier.class) final String pattern
    ) throws MalformedScopeException {
        exception.expect (MalformedScopeException.class);
        ScopeMatcher.forPattern (pattern);
    }

    //

    private final void __testMatcher (final String pattern, final String [][] positiveInputs, final String [][] negativeInputs) {
        final Scope m = ScopeMatcher.forPattern (pattern);
        if (positiveInputs != null) {
            __assertMatch (true, m, positiveInputs);
        }

        if (negativeInputs != null) {
            __assertMatch (false, m, negativeInputs);
        }
    }

    private final void __assertMatch (final boolean expected, final String pattern, final String [] ... inputs) {
        __assertMatch (expected, ScopeMatcher.forPattern (pattern), inputs);
    }

    private final void __assertMatch (final boolean expected, final Scope m, final String [] ... inputs) {
        for (final String [] input : inputs) {
            Assert.assertEquals (
                String.format ("%s should%s match '%s'", m, expected ? "" : " not", String.join (" ", input)),
                expected, m.matches (input [0], input [1], input [2]));
        }
    }

    // Vocabulary

    private final String [] __method (final String className, final String methodName, final String methodDesc) {
        return new String [] { className, methodName, methodDesc };
    }

    private final String [][] __accept (final String [] ... inputs) {
        return inputs;
    }

    private final String [][] __reject (final String [] ... inputs) {
        return inputs;
    }
    //

    private static List <PotentialAssignment> __stringAssignments (final String ... cases) {
        return Arrays.stream (cases)
            .map (s -> PotentialAssignment.forValue ("\""+s+"\"", s))
            .collect (Collectors.toList ());
    }

}
