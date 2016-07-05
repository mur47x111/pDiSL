package ch.usi.dag.disl.coderep;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import ch.usi.dag.disl.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.CodeTransformer;
import ch.usi.dag.disl.util.AsmHelper.Insns;
import ch.usi.dag.disl.util.Insn;

final class RewriteThreadLocalVarAccessesCodeTransformer implements CodeTransformer {

    private final Set <String> __tlvIds;

    //

    public RewriteThreadLocalVarAccessesCodeTransformer (final Set <ThreadLocalVar> tlvs) {
        //
        // Generate a set of TLV identifiers for faster lookup.
        //
        // TODO LB: We do this for every class - make LocalVars support the check.
        //
        __tlvIds = tlvs.stream ().unordered ()
            .map (tlv -> tlv.getID ())
            .collect (Collectors.toSet ());
    }

    //

    @Override
    public void transform (final InsnList insns) {
        //
        // Scan the method code for GETSTATIC/PUTSTATIC instructions accessing
        // the static fields marked to be thread locals. Replace all the
        // static accesses with thread variable accesses.
        //
        // First select the instructions, then modify the instruction list.
        //
        final List <FieldInsnNode> fieldInsns = Insns.asList (insns)
            .stream ().unordered ()
            .filter (AsmHelper::isStaticFieldAccess)
            .map (insn -> (FieldInsnNode) insn)
            .filter (insn -> {
                final String fieldName = ThreadLocalVar.fqFieldNameFor (insn.owner, insn.name);
                return __tlvIds.contains (fieldName);
            })
            .collect (Collectors.toList ());

        fieldInsns.forEach (insn -> __rewriteThreadLocalVarAccess (insn, insns));
    }

    //

    private static final Type threadType = Type.getType (Thread.class);
    private static final String currentThreadName = "currentThread";
    private static final Type currentThreadType = Type.getMethodType (threadType);

    private static void __rewriteThreadLocalVarAccess (
        final FieldInsnNode fieldInsn, final InsnList insns
    ) {
        //
        // Issue a call to Thread.currentThread() and access a field
        // in the current thread corresponding to the thread-local
        // variable.
        //
        insns.insertBefore (fieldInsn, AsmHelper.invokeStatic (
            threadType, currentThreadName, currentThreadType
        ));

        if (Insn.GETSTATIC.matches (fieldInsn)) {
            insns.insertBefore (fieldInsn, AsmHelper.getField (
                threadType, fieldInsn.name, fieldInsn.desc
            ));

        } else if (Insn.PUTSTATIC.matches (fieldInsn)) {
            //
            // We need to execute a PUTFIELD instruction, which requires
            // two operands, but the current thread reference that we
            // currently have on the top of the stack needs to come after
            // the value that is to be stored.
            //
            // We therefore need to swap the two operands on the stack.
            // There is no easier way, unless we want to track where the
            // value to be stored was pushed on the stack and put the
            // currentThread() method invocation before it.
            //
            // For primitive operands, we just swap the values. For wide
            // operands, we need to rearrange 3 slots in total, with the
            // slot 0 becoming slot 2, and slots 1 and 2 becoming 0 and 1.
            //
            if (Type.getType (fieldInsn.desc).getSize () == 1) {
                insns.insertBefore (fieldInsn, new InsnNode (Opcodes.SWAP));

            } else {
                insns.insertBefore (fieldInsn, new InsnNode (Opcodes.DUP_X2));
                insns.insertBefore (fieldInsn, new InsnNode (Opcodes.POP));
            }


            insns.insertBefore (fieldInsn, AsmHelper.putField (
                threadType, fieldInsn.name, fieldInsn.desc
            ));

        } else {
            throw new AssertionError ("field insn is GETSTATIC/PUTSTATIC");
        }

        //
        // Remove the static field access instruction.
        //
        insns.remove (fieldInsn);
    }

}
