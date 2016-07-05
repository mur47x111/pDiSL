package ch.usi.dag.disl.staticcontext;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.util.AsmHelper.Insns;
import ch.usi.dag.disl.util.Insn;


/**
 * <b>NOTE: This class is work in progress</b> <br>
 * <br>
 * Provides static information about the first instruction of this instrumented
 * region.
 */
public class InstructionStaticContext extends AbstractStaticContext {

    /**
     * Returns the opcode of the first instruction of this instrumented region.
     */
    public int getOpcode () {
        final AbstractInsnNode insn = staticContextData.getRegionStart ();
        return Insns.FORWARD.firstRealInsn (insn).getOpcode ();
    }


    /**
     * Returns a zero-based index of the first instruction of this instrumented
     * region in the method bytecode.
     */
    public int getIndex () {
        final AbstractInsnNode startInsn = staticContextData.getRegionStart ();
        final InsnList insns = staticContextData.getMethodNode ().instructions;

        //
        // There is a region, therefore there must be instructions.
        // No need to check for empty instruction list.
        //
        AbstractInsnNode insn = insns.getFirst ();

        int result = 0;
        while (insn != startInsn) {
            result += Insn.isVirtual (insn) ? 0 : 1;
            insn = insn.getNext ();
        }

        return result;
    }

}
