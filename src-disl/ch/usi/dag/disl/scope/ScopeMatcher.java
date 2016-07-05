package ch.usi.dag.disl.scope;

import java.util.Objects;
import java.util.regex.Pattern;

import ch.usi.dag.disl.util.JavaNames;


/**
 * Filters methods based on class name, method name, method parameters and
 * return type. A filter is specified as follows:
 * <ul>
 * <b>[&lt;returnType&gt;] [&lt;className&gt;.]&lt;methodName&gt;
 * [(&lt;paramTypes&gt;)]</b>
 * </ul>
 * To match multiple methods, the individual filter elements (or their parts)
 * can be substituted with the "*" wild card character, which will expand to
 * zero or more non-whitespace characters. The individual filter elements have
 * the following meaning:
 * <p>
 * <dl>
 * <dt>&lt;returnType&gt;
 * <dd>The type of the method's return value, specified either as a fully
 * qualified class name (for reference types) or a primitive type name. If not
 * specified, the filter will match all return types. For example:
 * <p>
 * <dl>
 * <dt>* or nothing
 * <dd>matches all return types
 * <dt>int
 * <dd>matches methods returning a primitive integer type
 * <dt>*.String
 * <dd>matches methods returning a String class from any package, e.g.,
 * java.lang.String, or my.package.String.
 * <dt>*String
 * <dd>matches methods returning any class with a name ending with String from
 * any package, e.g., java.lang.String, my.package.String, or
 * my.package.BigString
 * <dt>*[]
 * <dd>matches methods returning any array type with one or more dimensions
 * <dt>*.String[][]
 * <dd>matches methods returning an array of Strings with two or more dimensions
 * </dl>
 * <dt>&lt;className&gt;
 * <dd>Fully qualified name of the class and method the filter is supposed to
 * match. If not specified, the filter will match all classes. The package part
 * of a class name can be omitted, which will make the filter to match all
 * packages. To match a class without a package name (i.e. in the default
 * package), specify the package name as "[default]". For example:
 * <p>
 * <dl>
 * <dt>* or nothing
 * <dd>matches all classes
 * <dt>TargetClass
 * <dd>matches a class named TargetClass in any package
 * <dt>[default].TargetClass
 * <dd>matches a class named TargetClass only in the default package, i.e., it
 * does not match my.pkg.TargetClass
 * <dt>TargetClass*
 * <dd>matches any class with a name starting with TargetClass in any package,
 * e.g., TargetClassFoo in any package, or TargetClassBar in any package
 * <dt>my.pkg.*Math
 * <dd>matches any class with a name ending with Math in the my.pkg package and
 * all sub packages, e.g., my.pkg.Math, my.pkg.FastMath, my.pkg.new.FastMath, or
 * my.pkg.new.fast.Math
 * </dl>
 * <dt>&lt;methodName&gt;
 * <dd>The name of the method the filter is supposed to match. This filter
 * element is mandatory, therefore to match any method name, the
 * &lt;methodName&gt; element must be replaced with a "*". To match class
 * initializers and class constructors, use their bytecode-internal names, i.e.,
 * "&lt;clinit&gt;" and "&lt;init&gt;", respectively. For example:
 * <p>
 * <dl>
 * <dt>*
 * <dd>matches all methods
 * <dt>*init&gt;
 * <dd>matches class initializer (&lt;clinit&gt;) and class constructor
 * (&lt;init&gt;)
 * </dl>
 * <p>
 * <dt>&lt;paramTypes&gt;
 * <dd>A comma-separated list of method parameter types. Each parameter type is
 * specified either as a fully qualified class name or a primitive type name.
 * The filter parameter list can end with "..", which matches all remaining
 * method parameters. If not specified, the filter matches all methods
 * regardless of their parameter types. For example:
 * <p>
 * <dl>
 * <dt>(..)
 * <dd>matches methods with any (including none) parameters, e.g., (), or (int)
 * <dt>(int, int, ..)
 * <dd>matches any method with at least two parameters, and the parameter list
 * starting with two integers, e.g., (int, int), (int, int, double), or (int,
 * int, Object, String)
 * <dt>(java.lang.String, java.lang.String[])
 * <dd>matches a method with exactly two parameters with matching types. The
 * types are matched verbatim, i.e., there is no matching based on subtyping.
 * </dl>
 * </dl>
 * To put it all together, consider the following complete examples:
 * <p>
 * <dl>
 * <dt>my.pkg.TargetClass.main(java.lang.String[])
 * <dd>matches the "main" method in class my.pkg.TargetClass which takes as a
 * parameter an array of Strings. In this case, the return type is not
 * important, because the parameter signature is fully specified.
 * <dt>int *
 * <dd>matches all methods returning an integer value
 * <dt>*(int, int, int)
 * <dd>matches all methods accepting three integer values
 * </dl>
 *
 * @author Lukas Marek
 * @author Lubomir Bulej
 */
public class ScopeMatcher implements Scope {

    private static final String __DEFAULT_PKG_WITH_SEPARATOR__ = "[default].";

    //

    private final TypeMatcher __returnTypeMatcher;
    private final WildCardMatcher __classNameMatcher;
    private final WildCardMatcher __methodNameMatcher;
    private final ParameterMatcher __parameterMatcher;

    //

    private ScopeMatcher (
        final TypeMatcher returnTypeMatcher, final WildCardMatcher classNameMatcher,
        final WildCardMatcher methodNameMatcher, final ParameterMatcher parameterMatcher
    ) {
        __returnTypeMatcher = returnTypeMatcher;
        __classNameMatcher = classNameMatcher;
        __methodNameMatcher = methodNameMatcher;
        __parameterMatcher = parameterMatcher;
    }

    //

    private static final class ScopeParts {
        String returnType;
        String className;
        String methodName;
        String parameters;
    }

    /**
     * Parses a scope pattern and returns a {@link Scope} instance that
     * can be used to match fully qualified method names.
     *
     * @param pattern the scope pattern, may not be {@code null} or empty.
     * @return
     */
    public static Scope forPattern (final String pattern) {
        final String trimmed = Objects.requireNonNull (pattern).trim ();
        if (trimmed.isEmpty ()) {
            throw new MalformedScopeException ("scope expression is empty");
        }

        final ScopeParts parts = __splitPattern (trimmed);

        //
        // Construct the pattern using internal class names to avoid internal
        // to canonical conversions for classes being matched.
        //
        return new ScopeMatcher (
            TypeMatcher.forPattern (parts.returnType),
            __classNameMatcherForPattern (parts.className),
            WildCardMatcher.forPattern (parts.methodName),
            ParameterMatcher.forPattern (parts.parameters)
        );
    }


    private static WildCardMatcher __classNameMatcherForPattern (final String input) {
        return WildCardMatcher.forPattern (
            JavaNames.typeToInternal (__fixupClassName (input))
        );
    }

    private static String __fixupClassName (final String input) {
        if (input.startsWith (WildCardMatcher.WILDCARD)) {
            // This will already match any prefix, no modifications needed.
            return input;

        } else if (!JavaNames.typeNameHasPackage (input)) {
            // Add package wild card to classes without package specification.
            return JavaNames.typeNameJoin (WildCardMatcher.WILDCARD, input);

        } else if (input.startsWith (__DEFAULT_PKG_WITH_SEPARATOR__)) {
            // Strip the [default] package specifier, leaving the separator.
            return input.substring (__DEFAULT_PKG_WITH_SEPARATOR__.length () - 1);

        } else {
            return input;
        }
    }

    //

    private static ScopeParts __splitPattern (final String input) {
        final ScopeParts result = new ScopeParts ();

        //
        // Split out the parameter block delimited by opening and closing
        // parentheses. The block is optional, so if there is no opening
        // parenthesis, there should be no closing parenthesis.
        //
        final int openIndex = input.indexOf ("(");
        final int closeIndex = input.indexOf (")");

        if (openIndex < 0 && closeIndex < 0) {
            // no parameter specification, matches any parameter
            __splitReturnTypeAndMethod (input, result);
            result.parameters = ParameterMatcher.WILDCARD;

        } else if (openIndex >= 0 && openIndex < closeIndex) {
            __splitReturnTypeAndMethod (input.substring (0, openIndex), result);
            result.parameters = input.substring (openIndex + 1, closeIndex);

        } else {
            throw new MalformedScopeException (
                "missing or misplaced closing parenthesis: %s", input
            );
        }

        return result;
    }

    //

    private static final Pattern __spaceSplitter__ = Pattern.compile ("\\s+");

    private static void __splitReturnTypeAndMethod (
        final String input, final ScopeParts result
    ) {
        //
        // Split on white space. If there are two elements, there is a return
        // type. If there is only one element, there is no return type
        // specification. Any other case is wrong.
        //
        final String [] parts = __spaceSplitter__.split (input.trim ());
        if (parts.length == 2) {
            result.returnType = parts [0];
            __splitClassAndMethod (parts [1], result);

        } else if (parts.length == 1) {
            // match any return type
            result.returnType = WildCardMatcher.WILDCARD;
            __splitClassAndMethod (parts [0], result);

        } else {
            throw new MalformedScopeException (
                "invalid return type or method specification: %s", input
            );
        }
    }


    private static void __splitClassAndMethod (
        final String input, final ScopeParts result
    ) {
        //
        // Split on the last dot. If there are no dots, there is no class name,
        // so we match on any class name.
        //
        final int dotIndex = input.lastIndexOf ('.');
        if (dotIndex < 0) {
            result.className = WildCardMatcher.WILDCARD;
            result.methodName = input;

        } else if (1 <= dotIndex && dotIndex < input.length () - 1) {
            result.className = input.substring (0, dotIndex);
            result.methodName = input.substring (dotIndex + 1);

        } else {
            throw new MalformedScopeException ("invalid method qualifier: %s", input);
        }
    }

    //

    @Override
    public boolean matches (
        final String classInternalName, final String methodName, final String methodDesc
    ) {
        //
        // Add empty package to classes without any package specification. This
        // will make them matchable by scopes that match on class name but not
        // on package name. We must be able to match default packages as well.
        //
        final String className = JavaNames.internalNameHasPackage (classInternalName) ?
            classInternalName : JavaNames.joinInternal ("", classInternalName);

        //
        // Match class name, method name, parameters, and return type.
        //
        if (!__classNameMatcher.match (className)) {
            return false;
        }

        if (!__methodNameMatcher.match (methodName)) {
            return false;
        }

        if (!__parameterMatcher.match (methodDesc)) {
            return false;
        }

        final String returnTypeDesc = __getReturnTypeDescriptor (methodDesc);
        return __returnTypeMatcher.match (returnTypeDesc);
    }


    private static String __getReturnTypeDescriptor (final String methodDesc) {
        //
        // Avoids allocation in Type.getReturnType().toString() by instead
        // returning a substring of the original string.
        //
        return methodDesc.substring (methodDesc.indexOf (')') + 1);
    }


    @Override
    public String toString () {
        return String.format ("r='%s' c='%s' m='%s' p=(%s)",
            __returnTypeMatcher.pattern (), __classNameMatcher.pattern (),
            __methodNameMatcher.pattern (), __parameterMatcher.pattern ()
        );
    }
}
