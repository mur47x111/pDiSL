package ch.usi.dag.disl.staticcontext;

import org.objectweb.asm.Opcodes;

import ch.usi.dag.disl.util.JavaNames;


/**
 * Provides class related static context information for the method being
 * instrumented.
 */
public class ClassStaticContext extends AbstractStaticContext {

    /**
     * Returns the type name of the instrumented class, i.e., a fully
     * qualified class name, with packages delimited by the '.' character.
     */
    public String getName () {
        return JavaNames.internalToType (__classInternalName ());
    }


    /**
     * Returns the internal name of the instrumented class, i.e., a fully
     * qualified class name, with packages delimited by the '/' character.
     */
    public String getInternalName () {
        return __classInternalName ();
    }


    /**
     * Returns the simple name of the instrumented class, i.e., a class name
     * without the package part of the name.
     */
    public String getSimpleName () {
        return JavaNames.simpleClassName (__classInternalName ());
    }


    /**
     * Returns the internal name of the class enclosing the instrumented class,
     * or {@code null} if the instrumented class is not enclosed in another
     * class.
     */
    public String getOuterClassInternalName () {
        return staticContextData.getClassNode ().outerClass;
    }


    /**
     * Returns the name of the method enclosing the instrumented class, or
     * {@code null} if the class is not enclosed in a method.
     */
    public String getOuterMethodName () {
        return staticContextData.getClassNode ().outerMethod;
    }


    /**
     * Returns outer method descriptor of the instrumented class.
     */
    public String getOuterMethodDescriptor () {
        return staticContextData.getClassNode ().outerMethodDesc;
    }


    /**
     * Returns the signature of the instrumented class, or {@code null} if the
     * class is not a generic type.
     */
    public String getSignature () {
        return staticContextData.getClassNode ().signature;
    }


    /**
     * Returns the name of the source file containing the instrumented class.
     */
    public String getSourceFile () {
        return staticContextData.getClassNode ().sourceFile;
    }


    /**
     * Returns the internal name of the super class of the instrumented class,
     * i.e., a fully qualified class name, with package names delimited by the
     * '/' character.
     */
    public String getSuperClassInternalName () {
        return staticContextData.getClassNode ().superName;
    }


    /**
     * Returns class version as (ASM) integer of the instrumented class.
     */
    public int getVersion () {
        return staticContextData.getClassNode ().version;
    }


    /**
     * Returns {@code true} if the instrumented class is abstract.
     */
    public boolean isAbstract () {
        return __classAccessFlag (Opcodes.ACC_ABSTRACT);
    }


    /**
     * Returns {@code true} if the instrumented class is an annotation.
     */
    public boolean isAnnotation () {
        return __classAccessFlag (Opcodes.ACC_ANNOTATION);
    }


    /**
     * Returns {@code true} if the instrumented class is an enum.
     */
    public boolean isEnum () {
        return __classAccessFlag (Opcodes.ACC_ENUM);
    }


    /**
     * Returns {@code true} if the instrumented class is final.
     */
    public boolean isFinal () {
        return __classAccessFlag (Opcodes.ACC_FINAL);
    }


    /**
     * Returns {@code true} if the instrumented class is an interface.
     */
    public boolean isInterface () {
        return __classAccessFlag (Opcodes.ACC_INTERFACE);
    }


    /**
     * Returns {@code true} if the instrumented class is private.
     */
    public boolean isPrivate () {
        return __classAccessFlag (Opcodes.ACC_PRIVATE);
    }


    /**
     * Returns {@code true} if the instrumented class is protected.
     */
    public boolean isProtected () {
        return __classAccessFlag (Opcodes.ACC_PROTECTED);
    }


    /**
     * Returns {@code true} if the instrumented class is public.
     */
    public boolean isPublic () {
        return __classAccessFlag (Opcodes.ACC_PUBLIC);
    }


    /**
     * Returns {@code true} if the instrumented class is synthetic.
     */
    public boolean isSynthetic () {
        return __classAccessFlag (Opcodes.ACC_SYNTHETIC);
    }

    //

    private String __classInternalName () {
        return staticContextData.getClassNode ().name;
    }


    private boolean __classAccessFlag (final int flagMask) {
        final int access = staticContextData.getClassNode ().access;
        return (access & flagMask) != 0;
    }

}
