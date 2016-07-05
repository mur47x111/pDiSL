package ch.usi.dag.disl.guardcontext;

import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.Reflection;
import ch.usi.dag.disl.Reflection.Class;
import ch.usi.dag.disl.Reflection.Method;
import ch.usi.dag.disl.staticcontext.AbstractStaticContext;


/**
 * Provides access to reflective information about the currently instrumented
 * method.
 * <p>
 * <b>Note:</b> This static context can only be used in guard methods, because
 * its methods return other than primitive types and strings, which is required
 * for using a static context in a snippet.
 */
public final class ReflectionStaticContext extends AbstractStaticContext {

    public Class thisClass () {
        return Reflection.systemClassLoader ().classForInternalName (
            staticContextData.getClassNode ().name
        ).get ();
    }

    public Method thisMethod () {
        final MethodNode mn = staticContextData.getMethodNode ();
        return thisClass().methodForSignature (mn.name + mn.desc).get ();
    }

}
