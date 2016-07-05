package ch.usi.dag.disl.staticcontext.uid;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.JavaNames;


public abstract class AbstractMethodUid extends AbstractUniqueId {

    private static AbstractUniqueId __instance__;

    //

    // constructor for static context
    public AbstractMethodUid () {
        super ();
    }

    // constructor for singleton
    protected AbstractMethodUid (
        final AbstractIdCalculator idCalc, final String outputFileName
    ) {
        super (idCalc, outputFileName);
    }

    //

    @Override
    protected final String idFor () {
        final ClassNode classNode = staticContextData.getClassNode ();
        final MethodNode methodNode = staticContextData.getMethodNode ();

        return JavaNames.methodUniqueName (classNode.name, methodNode.name, methodNode.desc);
    }

    @Override
    protected final synchronized AbstractUniqueId getSingleton () {
        if (__instance__ == null) {
            __instance__ = _getInstance ();
        }

        return __instance__;
    }

    //

    protected abstract AbstractUniqueId _getInstance ();

}
