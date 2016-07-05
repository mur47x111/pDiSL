package ch.usi.dag.disl.scope;

/**
 * Represents a snippet scope and allows matching the scope against classes and
 * methods. The implementation <b>MUST</b> be thread-safe.
 */
public interface Scope {

    /**
     * Determines whether this scope matches the given class name (including
     * package name), method name, and method type descriptor.
     *
     * @param classInternalName
     *        <b>internal name</b> (i.e., delimited using slashes) of the class to match
     * @param methodName
     *        name of the method to match
     * @param methodDesc
     *        type descriptor of the method to match
     * @return {@code true} if the scope matches the given class name, method
     *         name, and method type descriptor, {@code false} otherwise.
     */
    boolean matches (String classInternalName, String methodName, String methodDesc);

}
