package ch.usi.dag.disl.snippet;

import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.util.CodeTransformer;
import ch.usi.dag.disl.util.AsmHelper.Insns;

/**
 * Shifts access to local variable slots by a given offset.
 */
final class ShiftLocalVarSlotCodeTransformer implements CodeTransformer {

    private final int __offset;

    public ShiftLocalVarSlotCodeTransformer (final int offset) {
        __offset = offset;
    }

    //

    @Override
    public void transform (final InsnList insns) {
        Insns.asList (insns).stream ().forEach (insn -> {
            if (insn instanceof VarInsnNode) {
                ((VarInsnNode) insn).var += __offset;

            } else if (insn instanceof IincInsnNode) {
                ((IincInsnNode) insn).var += __offset;
            }
        });
    }

}
