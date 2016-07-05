package ch.usi.dag.disl.coderep;

import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.CodeTransformer;
import ch.usi.dag.disl.util.AsmHelper.Insns;


/**
 * Replaces premature return from method with an unconditional jump at the end
 * of the instruction list. This is done by adding a label at the end of the
 * given instruction list, and replacing all kinds of RETURN instructions with a
 * GOTO instruction to jump to the label at the end of the instruction list.
 */
final class ReplaceReturnsWithGotoCodeTransformer implements CodeTransformer {

    /*
     * @see CodeProcessor#process(InsnList)
     */
    @Override
    public void transform (final InsnList insns) {
        //
        // Collect all RETURN instructions.
        //
        final List <AbstractInsnNode> returnInsns = Insns.asList (insns)
            .stream ()
            .filter (insn -> AsmHelper.isReturn (insn))
            .collect (Collectors.toList ());

        if (returnInsns.size () > 1) {
            //
            // Replace all RETURN instructions with a GOTO instruction
            // that jumps to a label at the end of the instruction list.
            //
            final LabelNode targetLabel = new LabelNode ();

            returnInsns.forEach (insn -> {
                insns.insertBefore (insn, AsmHelper.jumpTo (targetLabel));
                insns.remove (insn);
            });

            insns.add (targetLabel);

        } else if (returnInsns.size () == 1) {
            // Remove the RETURN at the end of a method. No need for GOTO.
            insns.remove (returnInsns.get (0));
        }
    }

}
