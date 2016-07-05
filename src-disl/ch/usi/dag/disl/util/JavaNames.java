package ch.usi.dag.disl.util;


/**
 * Utility class providing methods for working with Java-specific class and
 * methods names.
 * <p>
 * <b>Note:</b> This class is not part of the DiSL API.
 *
 * @author Lubomir Bulej
 */
public final class JavaNames {

    private JavaNames () {
        // not to be instantiated
    }

    //

    private static final String __CONSTRUCTOR_NAME__ = "<init>";
    private static final String __INITIALIZER_NAME__ = "<clinit>";

    public static final boolean isConstructorName (final String name) {
        return __CONSTRUCTOR_NAME__.equals (name);
    }

    public static final boolean isInitializerName (final String name) {
        return __INITIALIZER_NAME__.equals (name);
    }

    //

    private static final char __TYPE_NAME_PKG_SEPARATOR_CHAR__ = '.';
    private static final char __INTERNAL_NAME_PKG_SEPARATOR_CHAR__ = '/';


    /**
     * @return Type type name for the given internal class name.
     */
    public static String internalToType (final String internalName) {
        return internalName.replace (
            __INTERNAL_NAME_PKG_SEPARATOR_CHAR__, __TYPE_NAME_PKG_SEPARATOR_CHAR__
        );
    }

    /**
     * @return Internal class name for the given type name.
     */
    public static String typeToInternal (final String typeName) {
        return typeName.replace (
            __TYPE_NAME_PKG_SEPARATOR_CHAR__, __INTERNAL_NAME_PKG_SEPARATOR_CHAR__
        );
    }

    //

    private static final String __CLASS_FILE_EXTENSION__ = ".class";

    public static String appendClassFileExtension (final String name) {
        return name + __CLASS_FILE_EXTENSION__;
    }

    public static String stripClassFileExtension (final String name) {
        final int extensionStart = name.lastIndexOf (__CLASS_FILE_EXTENSION__);
        return (extensionStart >= 0) ? name.substring (0, extensionStart) : name;
    }

    public static boolean hasClassFileExtension (final String name) {
        return name.endsWith (__CLASS_FILE_EXTENSION__);
    }

    //

    public static boolean hasPackageName (final String name) {
        return __lastIndexOfPkgSeparator (name) >= 0;
    }

    public static boolean internalNameHasPackage (final String name) {
        return name.indexOf (__INTERNAL_NAME_PKG_SEPARATOR_CHAR__, 0) >= 0;
    }

    public static boolean typeNameHasPackage (final String name) {
        return name.indexOf (__TYPE_NAME_PKG_SEPARATOR_CHAR__, 0) >= 0;
    }

    //

    private static final String __INTERNAL_NAME_PKG_SEPARATOR__ =
        String.valueOf (__INTERNAL_NAME_PKG_SEPARATOR_CHAR__);

    public static String joinInternal (final String ... elements) {
        return String.join (__INTERNAL_NAME_PKG_SEPARATOR__, elements);
    }


    private static final String __TYPE_NAME_PKG_SEPARATOR__ =
        String.valueOf (__TYPE_NAME_PKG_SEPARATOR_CHAR__);

    public static String typeNameJoin (final String ... elements) {
        return String.join (__TYPE_NAME_PKG_SEPARATOR__, elements);
    }

    //

    public static String simpleClassName (final Object object) {
        return simpleClassName (object.getClass ().getName ());
    }

    public static String simpleClassName (final String name) {
        return name.substring (__lastIndexOfPkgSeparator (name) + 1);
    }

    public static String packageName (final String name) {
        final int endIndex =  __lastIndexOfPkgSeparator (name);
        return (endIndex >= 0) ? name.substring (0, endIndex) : "";
    }


    private static int __lastIndexOfPkgSeparator (final String name) {
        final int idx = name.lastIndexOf (__TYPE_NAME_PKG_SEPARATOR_CHAR__);
        return (idx >= 0) ? idx : name.lastIndexOf (__INTERNAL_NAME_PKG_SEPARATOR_CHAR__);
    }

    //

    public static String methodName (final String owner, final String name) {
        return String.format ("%s.%s", owner, name);
    }

    public static String methodUniqueName (final String owner, final String name, final String desc) {
        return String.format ("%s.%s%s", owner, name, desc);
    }

}
