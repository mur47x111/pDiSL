package ch.usi.dag.disl.staticcontext;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.JavaNames;


/**
 * Provides method related static context information for the method being
 * instrumented.
 */
public class MethodStaticContext extends AbstractStaticContext {

    /**
     * Returns the internal name of the instrumented class, i.e., a fully
     * qualified class name, with packages delimited by the '/' character.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getInternalName()} instead.
     */
    @Deprecated
    public String thisClassName () {
        return __classInternalName ();
    }


    /**
     * Returns the simple name of the instrumented class, i.e., a class name
     * without the package part of the name.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getSimpleName()} instead.
     */
    @Deprecated
    public String thisClassSimpleName () {
        return JavaNames.simpleClassName (__classInternalName ());
    }


    /**
     * Returns the canonical name of the instrumented class, i.e., a fully
     * qualified class name, with packages delimited by the '.' character.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getName()} instead.
     */
    @Deprecated
    public String thisClassCanonicalName () {
        return JavaNames.internalToType (__classInternalName ());
    }


    /**
     * Returns the internal name of the class enclosing the instrumented class,
     * or {@code null} if the instrumented class is not enclosed in another
     * class.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getOuterClassInternalName()} instead.
     */
    @Deprecated
    public String thisClassOuterClass () {
        return __classNode ().outerClass;
    }


    /**
     * Returns the name of the method enclosing the instrumented class, or
     * {@code null} if the class is not enclosed in a method.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getOuterMethodName()} instead.
     */
    @Deprecated
    public String thisClassOuterMethod () {
        return __classNode ().outerMethod;
    }


    /**
     * Returns outer method descriptor of the instrumented class.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getOuterMethodDescriptor()} instead.
     */
    @Deprecated
    public String thisClassOuterMethodDesc () {
        return __classNode ().outerMethodDesc;
    }


    /**
     * Returns the signature of the instrumented class, or {@code null} if the
     * class is not a generic type.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getSignature()} instead.
     */
    @Deprecated
    public String thisClassSignature () {
        return __classNode ().signature;
    }


    /**
     * Returns the name of the source file containing the instrumented class.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getSourceFile()} instead.
     */
    @Deprecated
    public String thisClassSourceFile () {
        return __classNode ().sourceFile;
    }


    /**
     * Returns the internal name of the super class of the instrumented class,
     * i.e., a fully qualified class name, with package names delimited by the
     * '/' character.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getSuperClassInternalName()} instead.
     */
    @Deprecated
    public String thisClassSuperName () {
        return __classNode ().superName;
    }


    /**
     * Returns class version as (ASM) integer of the instrumented class.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#getVersion()} instead.
     */
    @Deprecated
    public int thisClassVersion () {
        return __classNode ().version;
    }


    /**
     * Returns {@code true} if the instrumented class is abstract.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isAbstract()} instead.
     */
    @Deprecated
    public boolean isClassAbstract () {
        return __classAccessFlag (Opcodes.ACC_ABSTRACT);
    }


    /**
     * Returns {@code true} if the instrumented class is an annotation.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isAnnotation()} instead.
     */
    @Deprecated
    public boolean isClassAnnotation () {
        return __classAccessFlag (Opcodes.ACC_ANNOTATION);
    }


    /**
     * Returns {@code true} if the instrumented class is an enum.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isEnum()} instead.
     */
    @Deprecated
    public boolean isClassEnum () {
        return __classAccessFlag (Opcodes.ACC_ENUM);
    }


    /**
     * Returns {@code true} if the instrumented class is final.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isFinal()} instead.
     */
    @Deprecated
    public boolean isClassFinal () {
        return __classAccessFlag (Opcodes.ACC_FINAL);
    }


    /**
     * Returns {@code true} if the instrumented class is an interface.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isInterface()} instead.
     */
    @Deprecated
    public boolean isClassInterface () {
        return __classAccessFlag (Opcodes.ACC_INTERFACE);
    }


    /**
     * Returns {@code true} if the instrumented class is private.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isPrivate()} instead.
     */
    @Deprecated
    public boolean isClassPrivate () {
        return __classAccessFlag (Opcodes.ACC_PRIVATE);
    }


    /**
     * Returns {@code true} if the instrumented class is protected.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isProtected()} instead.
     */
    @Deprecated
    public boolean isClassProtected () {
        return __classAccessFlag (Opcodes.ACC_PROTECTED);
    }


    /**
     * Returns {@code true} if the instrumented class is public.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isPublic()} instead.
     */
    @Deprecated
    public boolean isClassPublic () {
        return __classAccessFlag (Opcodes.ACC_PUBLIC);
    }


    /**
     * Returns {@code true} if the instrumented class is synthetic.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use
     * {@link ClassStaticContext#isSynthetic()} instead.
     */
    @Deprecated
    public boolean isClassSynthetic () {
        return __classAccessFlag (Opcodes.ACC_SYNTHETIC);
    }


    // *** Method ***


    /**
     * Returns the plain name of this method. This name does includes neither
     * the class name, nor the parameter type descriptor.
     *
     * @return The name of this method.
     */
    public String thisMethodName () {
        return __methodName ();
    }


    /**
     * Returns the fully qualified (internal) name of the instrumented method,
     * i.e., including the (internal) name of the class containing the method.
     */
    public String thisMethodFullName () {
        return JavaNames.methodName (__classInternalName (), __methodName ());
    }


    /**
     * Returns the fully qualified (internal) name of the instrumented method,
     * i.e., including the (internal) name of the class containing the method.
     */
    public String getUniqueInternalName () {
        return JavaNames.methodUniqueName (
            __classInternalName (), __methodName (), __methodNode ().desc
        );
    }


    /**
     * Returns the descriptor of the instrumented method.
     */
    public String thisMethodDescriptor () {
        return __methodNode ().desc;
    }


    /**
     * Returns the generic signature of the instrumented method or {@code null}
     * if the method is not a generic method.
     */
    public String thisMethodSignature () {
        return __methodNode ().signature;
    }


    /**
     * Returns {@code true} if this method is a constructor.
     */
    public boolean isMethodConstructor () {
        return JavaNames.isConstructorName (__methodName ());
    }


    /**
     * Returns {@code true} if this method is a class initializer.
     */
    public boolean isMethodInitializer () {
        return JavaNames.isInitializerName (__methodName ());
    }


    /**
     * Returns {@code true} if the instrumented method is a bridge.
     */
    public boolean isMethodBridge () {
        return __methodAccessFlag (Opcodes.ACC_BRIDGE);
    }


    /**
     * Returns {@code true} if the instrumented method is final.
     */
    public boolean isMethodFinal () {
        return __methodAccessFlag (Opcodes.ACC_FINAL);
    }


    /**
     * Returns {@code true} if the instrumented method is private.
     */
    public boolean isMethodPrivate () {
        return __methodAccessFlag (Opcodes.ACC_PRIVATE);
    }


    /**
     * Returns {@code true} if the instrumented method is protected.
     */
    public boolean isMethodProtected () {
        return __methodAccessFlag (Opcodes.ACC_PROTECTED);
    }


    /**
     * Returns {@code true} if the instrumented method is public.
     */
    public boolean isMethodPublic () {
        return __methodAccessFlag (Opcodes.ACC_PUBLIC);
    }


    /**
     * Returns {@code true} if the instrumented method is static.
     */
    public boolean isMethodStatic () {
        return __methodAccessFlag (Opcodes.ACC_STATIC);
    }


    /**
     * Returns {@code true} if the instrumented method is synchronized.
     */
    public boolean isMethodSynchronized () {
        return __methodAccessFlag (Opcodes.ACC_SYNCHRONIZED);
    }


    /**
     * Returns {@code true} if the instrumented method accepts a variable number
     * of arguments.
     */
    public boolean isMethodVarArgs () {
        return __methodAccessFlag (Opcodes.ACC_VARARGS);
    }


    //

    private String __classInternalName () {
        return __classNode ().name;
    }


    private boolean __classAccessFlag (final int flagMask) {
        final int access = __classNode ().access;
        return (access & flagMask) != 0;
    }


    private ClassNode __classNode () {
        return staticContextData.getClassNode ();
    }


    private String __methodName () {
        return __methodNode ().name;
    }


    private MethodNode __methodNode () {
        return staticContextData.getMethodNode ();
    }


    private boolean __methodAccessFlag (final int flagMask) {
        final int access = __methodNode ().access;
        return (access & flagMask) != 0;
    }

}
