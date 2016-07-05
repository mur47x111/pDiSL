package ch.usi.dag.disl.localvar;

import org.objectweb.asm.Type;


public final class ThreadLocalVar extends AbstractLocalVar {

    private Object __initialValue;
    private final boolean __inheritable;

    //

    public ThreadLocalVar (
        final String className, final String fieldName,
        final Type type, final boolean inheritable
    ) {
        super (className, fieldName, type);
        __inheritable = inheritable;
    }


    /**
     * @return the initial value of this thread local variable.
     */
    public Object getInitialValue() {
        return __initialValue;
    }

    /**
     * Sets the initial value of this thread local variable.
     *
     * @param value the value to initialize the variable to
     */
    public void setInitialValue (final Object value) {
        __initialValue = value;
    }


    /**
     * Determines whether the initial value of this variable should
     * be inherited from the current {@link Thread}.
     *
     * @return {@code true} if the variable value is inheritable.
     */
    public boolean isInheritable () {
        return __inheritable;
    }
}
