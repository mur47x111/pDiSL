package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;

import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.dynamicbypass.DynamicBypass;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.CodeTransformer;
import ch.usi.dag.disl.util.ReflectionHelper;


/**
 * Wraps a sequence of instructions with code that controls the dynamic bypass.
 * The bypass is enabled before the first instruction and disabled again after
 * the last instruction:
 *
 * <pre>
 *   DynamicBypass.activate();
 *   ... original snippet code ...
 *   DynamicBypass.deactivate();
 * </pre>
 */
class InsertDynamicBypassControlCodeTransformer implements CodeTransformer {

    private static final Method __dbActivate__ = ReflectionHelper.getMethod (DynamicBypass.class, "activate");
    private static final Method __dbDeactivate__ = ReflectionHelper.getMethod (DynamicBypass.class, "deactivate");

    //

    @Override
    public void transform (final InsnList insns) {
        insns.insert (AsmHelper.invokeStatic (__dbActivate__));
        insns.add (AsmHelper.invokeStatic (__dbDeactivate__));
    }

}
