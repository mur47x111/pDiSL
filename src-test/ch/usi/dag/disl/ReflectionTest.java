package ch.usi.dag.disl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Type;

import ch.usi.dag.disl.Reflection.Class;
import ch.usi.dag.disl.Reflection.ClassLoader;
import ch.usi.dag.disl.Reflection.MissingClassException;
import ch.usi.dag.disl.util.ClassNodeHelper;


/**
 * Tests the basic functionality of the {@link Reflection} class.
 *
 * @author Lubomir Bulej
 */
public final class ReflectionTest {

    private final ClassLoader __scl = Reflection.systemClassLoader ();

    private java.lang.Class <?> __methodInnerClass = null;

    //

    interface Missable {}

    interface Presentable {}

    static abstract class MissingSuperClass {}

    static abstract class AbstractClass extends MissingSuperClass implements Presentable {}

    static class ConcreteClass extends AbstractClass {
        class InnerClass {}
    }

    static class LeafClass extends ConcreteClass implements Missable {}

    //

    static List <java.lang.Class <?>> __classesToLoad__ = Arrays.asList (
        Object.class,
        AbstractClass.class, Presentable.class,
        ConcreteClass.class, LeafClass.class
    );


    static List <java.lang.Class <?>> __primitiveClasses__ = Arrays.asList (
        void.class, boolean.class, byte.class, char.class, short.class,
        int.class, float.class, long.class, double.class
    );

    //

    @Before
    public void initReflection () throws IOException {
        class MethodInnerClass {}

        __classesToLoad__.stream ().forEach (this::__loadClass);
        __methodInnerClass = MethodInnerClass.class;
        __loadClass (__methodInnerClass);
    }

    private void __loadClass (final java.lang.Class <?> cls) {
        try {
            __scl.notifyClassLoaded (
                ClassNodeHelper.OUTLINE.load (cls.getName ())
            );
        } catch (final IOException ioe) {
            throw new RuntimeException (ioe);
        }
    }

    //
    // Primitive type tests
    //

    @Test
    public void findBuiltInPrimitiveClasses () {
        __primitiveClasses__.stream ().map (c -> __getClass (c).get ());
        __getClass (__methodInnerClass).get ();
    }


    @Test
    public void voidIsPrimitive () {
        final Class voidClass = __getClass (void.class).get ();
        Assert.assertTrue (voidClass.isPrimitive ());
    }


    @Test
    public void voidIsNotArray () {
        final Class voidClass = __getClass (void.class).get ();
        Assert.assertFalse (voidClass.isArray ());
    }


    @Test
    public void voidIsNotInterface () {
        final Class voidClass = __getClass (void.class).get ();
        Assert.assertFalse (voidClass.isInterface ());
    }


    @Test
    public void voidHasNoSuperClass () {
        final Class voidClass = __getClass (void.class).get ();
        Assert.assertFalse (voidClass.superClass ().isPresent ());
    }


    @Test
    public void voidImplementsNoInterfaces () {
        final Class voidClass = __getClass (void.class).get ();
        Assert.assertEquals (0, voidClass.interfaces ().count ());
    }

    //
    // Reference types
    //

    @Test
    public void findLoadedNonPrimitiveClasses () {
        __classesToLoad__.stream ().map (c -> __getClass (c).get ());
    }


    @Test
    public void returnEmptyOptionalForMissingClass () {
        Assert.assertFalse (__getClass (MissingSuperClass.class).isPresent ());
    }

    //
    // Object tests
    //

    @Test
    public void objectIsNotPrimitiveClass () {
        final Class objectClass = __getClass (Object.class).get ();
        Assert.assertFalse (objectClass.isPrimitive ());
    }


    @Test
    public void objectIsNotArray () {
        final Class objectClass = __getClass (Object.class).get ();
        Assert.assertFalse (objectClass.isArray ());
    }


    @Test
    public void objectIsNotInterface () {
        final Class objectClass = __getClass (Object.class).get ();
        Assert.assertFalse (objectClass.isInterface ());
    }


    @Test
    public void objectHasNoSuperClass () {
        final Class objectClass = __getClass (Object.class).get ();
        Assert.assertFalse (objectClass.superClass ().isPresent ());
    }


    @Test
    public void objectImplementsNoInterfaces () {
        final Class objectClass = __getClass (Object.class).get ();
        Assert.assertEquals (0, objectClass.interfaces ().count ());
    }

    //
    // An interface
    //

    @Test
    public void presentableIsNotPrimitive () {
        final Class presentableClass = __getClass (Presentable.class).get ();
        Assert.assertFalse (presentableClass.isPrimitive ());
    }

    @Test
    public void presentableIsNotArray () {
        final Class presentableClass = __getClass (Presentable.class).get ();
        Assert.assertFalse (presentableClass.isArray ());
    }

    @Test
    public void presentableIsInterface () {
        final Class presentableClass = __getClass (Presentable.class).get ();
        Assert.assertTrue (presentableClass.isInterface ());
    }


    //
    //
    //

    @Test
    public void concreteRunnableSuperClassIsAbstractRunnable () {
        final Class concreteClass = __getClass (ConcreteClass.class).get ();
        final Class abstractClass = __getClass (AbstractClass.class).get ();
        Assert.assertEquals (abstractClass, concreteClass.superClass ().get ());
    }


    @Test (expected = MissingClassException.class)
    public void throwOnMissingSuperClass () {
        __getClass (AbstractClass.class).get ().superClass ();
    }


    @Test
    public void abstractClassImplementsPresentable () {
        final Class presentableClass = __getClass (Presentable.class).get ();
        final Class abstractClass = __getClass (AbstractClass.class).get ();
        Assert.assertTrue (abstractClass.interfaces ().anyMatch (
            cl -> cl.equals (presentableClass)
        ));
    }


    @Test
    public void leafClassImplementsMissingInterfaceType () {
        final Class leafClass = __getClass (LeafClass.class).get ();
        Assert.assertTrue (leafClass.interfaceTypes ().anyMatch (
            it -> it.equals (Type.getType (Missable.class)))
        );
    }


    @Test (expected = MissingClassException.class)
    public void throwOnMissingImplementedInterface () {
        final Class leafClass = __getClass (LeafClass.class).get ();
        Assert.assertTrue (leafClass.interfaces().anyMatch (
            it -> it.typeName ().equals (Missable.class.getName ())
        ));
    }

    //

    final java.lang.Class <?> __globalClasses [] = {
        void.class,
        int.class, int[].class, int[][].class,
        Object.class, Object[].class, Object[][].class,
        Presentable.class, Missable[].class
    };


    /**
     * Ensures type name correspondence between Java type names and our type
     * names. The internal names have no counterpart in Java.
     * <p>
     * <b>Note:</b> Just to clarify, Java also supports canonical names, which
     * are the same as type names, but use the '.' character instead of '$' as a
     * separator between the outer and inner classes. Java simple names are then
     * last element of a canonical name.
     * <p>
     * Java class names like type names, but produce a descriptor for array
     * types.
     */
    @Test
    public void typeNamesMatch () {
        for (final java.lang.Class <?> cl : __globalClasses) {
            Assert.assertEquals (
                cl.getTypeName (),
                __getClass (cl).get ().typeName ()
            );
        }
    }


    /**
     * Ensures that getting the {@link Class} instance for the same instance
     * twice returns equal objects. This is especially important for array
     * {@link Class} instances that are generated on the fly.
     */
    @Test
    public void classInstancesMatch () {
        for (final java.lang.Class <?> cl : __globalClasses) {
            Assert.assertEquals (__getClass (cl).get (), __getClass (cl).get ());
        }
    }

    //

    private Optional <Class> __getClass (final java.lang.Class <?> cls) {
        return __scl.classForType (Type.getType (cls));
    }
}
