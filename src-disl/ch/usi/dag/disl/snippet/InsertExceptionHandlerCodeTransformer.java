package ch.usi.dag.disl.snippet;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.CodeTransformer;
import ch.usi.dag.disl.util.ReflectionHelper;


/**
 * Wraps the code sequence with a try-catch block that fails immediately if a
 * snippet produces an exception.
 */
final class InsertExceptionHandlerCodeTransformer implements CodeTransformer {

    private static final Method __printlnMethod__ = ReflectionHelper.getMethod (PrintStream.class, "println", String.class);
    private static final Method __printStackTraceMethod__ = ReflectionHelper.getMethod (Throwable.class, "printStackTrace");
    private static final Method __exitMethod__ = ReflectionHelper.getMethod (System.class, "exit", int.class);
    private static final Field __errField__ = ReflectionHelper.getField (System.class, "err");

    //

    final String __location;
    final List <TryCatchBlockNode> __tcbs;


    /**
     * Initializes this transformer with a location and an output list to which
     * to add a newly created try-catch block.
     *
     * @param location
     *        the location to which this code corresponds.
     * @param tcbs
     *        the list of {@link TryCatchBlockNode} instances to which to add
     *        the newly created try-catch block.
     */
    public InsertExceptionHandlerCodeTransformer (final String location, final List <TryCatchBlockNode> tcbs) {
        __location = Objects.requireNonNull (location);
        __tcbs = Objects.requireNonNull (tcbs);
    }

    //

    @Override
    public void transform (final InsnList insns) {
        //
        // The inserted code:
        //
        // TRY_BEGIN:       try {
        //                      ... original snippet code ...
        //                      goto HANDLER_END;
        // TRY_END:         } finally (e) {
        // HANDLER_BEGIN:       System.err.println(...);
        //                      e.printStackTrace();
        //                      System.exit(666);
        //                      throw e;
        // HANDLER_END:     }
        //
        // In the finally block, the exception will be at the top of the stack.
        //
        final LabelNode tryBegin = new LabelNode();
        insns.insert (tryBegin);

        final LabelNode handlerEnd = new LabelNode ();
        insns.add (AsmHelper.jumpTo (handlerEnd));

        final LabelNode tryEnd = new LabelNode ();
        insns.add (tryEnd);

        final LabelNode handlerBegin = new LabelNode ();
        insns.add (handlerBegin);

        // System.err.println(...);
        insns.add (AsmHelper.getStatic (__errField__));
        insns.add (AsmHelper.loadConst (String.format (
            "%s: failed to handle an exception", __location
        )));
        insns.add (AsmHelper.invokeVirtual (__printlnMethod__));

        // e.printStackTrace();
        insns.add (new InsnNode (Opcodes.DUP));
        insns.add (AsmHelper.invokeVirtual (__printStackTraceMethod__));

        // System.exit(666)
        insns.add (AsmHelper.loadConst (666));
        insns.add (AsmHelper.invokeStatic (__exitMethod__));

        // Re-throw the exception (just for proper stack frame calculation)
        insns.add (new InsnNode (Opcodes.ATHROW));
        insns.add (handlerEnd);

        // Add the exception handler to the list.
        __tcbs.add (new TryCatchBlockNode (tryBegin, tryEnd, handlerBegin, null));
    }

}
