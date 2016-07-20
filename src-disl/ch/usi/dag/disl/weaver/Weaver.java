package ch.usi.dag.disl.weaver;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal.Initialize;
import ch.usi.dag.disl.exception.InvalidContextUsageException;
import ch.usi.dag.disl.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.processor.generator.PIResolver;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Shadow.WeavingRegion;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.SnippetCode;
import ch.usi.dag.disl.staticcontext.generator.SCGenerator;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.AsmHelper.Insns;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Map;

// The weaver instruments byte-codes into java class.
public class Weaver {

    /**
     * Transforms static fields to synthetic local variables.
     * <p>
     * <b>Note:</b> This methods automatically updates the
     * {@link MethodNode#maxLocals maxLocals} field in the given
     * {@link MethodNode}.
     */
    private static void static2Local (
        final MethodNode methodNode,
        final List<SyntheticLocalVar> syntheticLocalVars
    ) {
        final InsnList instructions = methodNode.instructions;
        final AbstractInsnNode first = instructions.getFirst ();

        //
        // Insert code to initialize synthetic local variables (unless marked to
        // be left uninitialized) at the beginning of a method.
        //
        // TODO Respect the BEST_EFFORT initialization type.
        //
        for (final SyntheticLocalVar var : syntheticLocalVars) {
            if (var.getInitialize () == Initialize.NEVER) {
                continue;
            }

            //
            // If the variable has initialization code, just copy it. It will
            // still refer to the static variable, but that will be fixed in
            // the next step.
            //
            if (var.hasInitCode ()) {
                instructions.insertBefore (
                    first, AsmHelper.cloneInstructions (var.getInitCode ())
                );

            } else {
                //
                // Otherwise, just initialize it with a default value depending
                // on its type. The default value for arrays is null, like for
                // objects, but they also need an additional CHECKCAST
                // instruction to make the verifier happy.
                //
                final Type type = var.getType ();
                if (type.getSort () != Type.ARRAY) {
                    instructions.insertBefore (first, AsmHelper.loadDefault (type));
                } else {
                    instructions.insertBefore (first, AsmHelper.loadNull ());
                    instructions.insertBefore (first, AsmHelper.checkCast (type));
                }

                //
                // For now, just issue an instruction to store the value into
                // the original static field. The transformation to local
                // variable comes in the next step.
                //
                instructions.insertBefore (first, AsmHelper.putStatic (
                    var.getOwner (), var.getName (), type.getDescriptor ()
                ));
            }
        }


        //
        // Scan the method code for GETSTATIC/PUTSTATIC instructions accessing
        // the static fields marked as synthetic locals. Replace all the
        // static accesses with local variables.
        //
        // TODO LB: iterate over a copy unless we are sure an iterator is OK
        for (final AbstractInsnNode insn : instructions.toArray ()) {
            final int opcode = insn.getOpcode ();
            if (!AsmHelper.isStaticFieldAccess (opcode)) {
                continue;
            }

            final FieldInsnNode fieldInsn = (FieldInsnNode) insn;
            final String varId = SyntheticLocalVar.fqFieldNameFor (fieldInsn.owner, fieldInsn.name);

            //
            // Try to find the static field being accessed among the synthetic
            // local fields, and determine the local variable index and local
            // slot index while doing that.
            //
            int index = 0, count = 0;
            for (final SyntheticLocalVar var : syntheticLocalVars) {
                if (varId.equals (var.getID ())) {
                    break;
                }

                index += var.getType ().getSize ();
                count++;
            }

            if (count == syntheticLocalVars.size ()) {
                // Static field not found among the synthetic locals.
                continue;
            }

            //
            // Replace the static field access with local variable access.
            //
            final Type type = Type.getType (fieldInsn.desc);
            final int slot = methodNode.maxLocals + index;

            instructions.insertBefore (fieldInsn,
                (opcode == Opcodes.GETSTATIC) ?
                AsmHelper.loadVar (type, slot) : AsmHelper.storeVar (type, slot)
            );

            instructions.remove (fieldInsn);
        }

        //
        // Adjust maxLocals to allow for the new local variables.
        //
        methodNode.maxLocals += syntheticLocalVars.size ();
    }


    // Return a successor label of weaving location corresponding to
    // the input 'end'.
    private static LabelNode getEndLabel (
        final MethodNode methodNode, AbstractInsnNode instr
    ) {
        if (Insns.FORWARD.nextRealInsn (instr) != null) {
            final LabelNode branch = new LabelNode();
            methodNode.instructions.insert(instr, branch);

            final JumpInsnNode jump = new JumpInsnNode(Opcodes.GOTO, branch);
            methodNode.instructions.insert(instr, jump);
            instr = jump;
        }

        // Create a label just after the 'GOTO' instruction.
        final LabelNode label = new LabelNode();
        methodNode.instructions.insert(instr, label);
        return label;
    }


    // generate a try catch block node given the scope of the handler
    public static TryCatchBlockNode getTryCatchBlock (
        final MethodNode methodNode,
        AbstractInsnNode start, AbstractInsnNode end
    ) {
        final InsnList insns = methodNode.instructions;

        int new_start_offset = insns.indexOf(start);
        final int new_end_offset = insns.indexOf(end);

        for (final TryCatchBlockNode tcb : methodNode.tryCatchBlocks) {
            final int start_offset = insns.indexOf (tcb.start);
            final int end_offset = insns.indexOf (tcb.end);

            if (
                AsmHelper.offsetBefore (insns, new_start_offset, start_offset)
                && AsmHelper.offsetBefore (insns, start_offset, new_end_offset)
                && AsmHelper.offsetBefore (insns, new_end_offset, end_offset)
            ) {
                new_start_offset = start_offset;

            } else if (
                AsmHelper.offsetBefore (insns, start_offset, new_start_offset)
                && AsmHelper.offsetBefore (insns, new_start_offset, end_offset)
                && AsmHelper.offsetBefore (insns, end_offset, new_end_offset)
            ) {
                new_start_offset = end_offset;
            }
        }

        start = insns.get (new_start_offset);
        end = insns.get (new_end_offset);

        final LabelNode startLabel = (LabelNode) start;
        final LabelNode endLabel = getEndLabel (methodNode, end);

        return new TryCatchBlockNode (startLabel, endLabel, endLabel, null);
    }


    private static void __insert (
        final MethodNode methodNode,
        final SCGenerator staticInfoHolder, final PIResolver piResolver,
        final WeavingInfo info, final Snippet snippet, final SnippetCode code,
        final Shadow shadow, final AbstractInsnNode loc, final ClassLoader classLoader
    ) throws InvalidContextUsageException {
        // exception handler will discard the stack and push the
        // exception object. Thus, before entering this snippet,
        // weaver must backup the stack and restore when exiting
        if (code.containsHandledException() && info.stackNotEmpty(loc)) {
            final InsnList backup = info.backupStack (loc, methodNode.maxLocals);
            final InsnList restore = info.restoreStack (loc, methodNode.maxLocals);

            methodNode.maxLocals += info.getStackHeight (loc);

            methodNode.instructions.insertBefore (loc, backup);
            methodNode.instructions.insert (loc, restore);
        }

        final WeavingCode wc = new WeavingCode (
            info, methodNode, code, snippet, shadow, loc, classLoader
        );

        wc.transform (staticInfoHolder, piResolver, false);

        methodNode.instructions.insert (loc, wc.getiList ());
        methodNode.tryCatchBlocks.addAll (wc.getTCBs ());
    }


    public static void instrument (
        final ClassNode classNode, final MethodNode methodNode,
        final Map <Snippet, List <Shadow>> snippetShadows,
        final List <SyntheticLocalVar> syntheticLocalVars,
        final SCGenerator staticInfoHolder, final PIResolver piResolver, final ClassLoader classLoader
    ) throws InvalidContextUsageException {

        final WeavingInfo info = new WeavingInfo(classNode, methodNode, snippetShadows);
        for (final Snippet snippet : info.getSortedSnippets ()) {
            final List <Shadow> shadows = snippetShadows.get (snippet);
            final SnippetCode code = snippet.getCode ();

            // skip snippet with empty code
            if (code == null) {
                continue;
            }

            // Instrument

            //
            // For @Before snippets, insert the snippet code just before the
            // region entry.
            //
            if (snippet.hasAnnotation (Before.class)) {
                final int initialMaxLocals = methodNode.maxLocals;
                int phaseMaxLocals = initialMaxLocals;

                for (final Shadow shadow : shadows) {
                    final AbstractInsnNode loc = shadow.getWeavingRegion ().getStart ();

                    __insert (
                        methodNode, staticInfoHolder, piResolver, info,
                        snippet, code, shadow, loc, classLoader
                    );

                    //
                    // Reset method max locals after each snippet, but keep
                    // track of the max locals for all snippets in this phase.
                    //
                    phaseMaxLocals = Math.max (phaseMaxLocals, methodNode.maxLocals);
                    methodNode.maxLocals = initialMaxLocals;
                }

                //
                // Set max locals to the max level reached in this phase.
                //
                methodNode.maxLocals = phaseMaxLocals;
            }


            //
            // For regular after (after returning), insert the snippet
            // after each adjusted exit of a region.
            //
            if (snippet.hasAnnotation (AfterReturning.class) || snippet.hasAnnotation (After.class)) {
                final int initialMaxLocals = methodNode.maxLocals;
                int phaseMaxLocals = initialMaxLocals;

                for (final Shadow shadow : shadows) {
                    for (final AbstractInsnNode loc : shadow.getWeavingRegion ().getEnds ()) {
                        __insert (
                            methodNode, staticInfoHolder, piResolver, info,
                            snippet, code, shadow, loc, classLoader
                        );

                        //
                        // Reset method max locals after each snippet, but keep
                        // track of the max locals for all snippets in this phase.
                        //
                        phaseMaxLocals = Math.max (phaseMaxLocals, methodNode.maxLocals);
                        methodNode.maxLocals = initialMaxLocals;
                    }
                }

                //
                // Set max locals to the max level reached in this phase.
                //
                methodNode.maxLocals = phaseMaxLocals;
            }


            //
            // For exceptional after (after throwing), wrap the region with
            // a try-finally clause and append the snippet as an exception
            // handler.
            //
            if (snippet.hasAnnotation(AfterThrowing.class) || snippet.hasAnnotation(After.class)) {
                final int initialMaxLocals = methodNode.maxLocals;
                int phaseMaxLocals = initialMaxLocals;

                for (final Shadow shadow : shadows) {
                    // after-throwing inserts the snippet once, and marks
                    // the start and the very end as the scope
                    final WeavingRegion region = shadow.getWeavingRegion ();
                    final AbstractInsnNode loc = region.getAfterThrowEnd ();

                    final WeavingCode wc = new WeavingCode (
                        info, methodNode, code, snippet, shadow, loc, classLoader
                    );

                    wc.transform (staticInfoHolder, piResolver, true);

                    // Create a try-catch clause
                    final TryCatchBlockNode tcb = getTryCatchBlock (
                        methodNode, region.getAfterThrowStart (), loc
                    );

                    methodNode.instructions.insert (tcb.handler, wc.getiList ());

                    methodNode.tryCatchBlocks.add (tcb);
                    methodNode.tryCatchBlocks.addAll (wc.getTCBs ());

                    //
                    // Reset method max locals after each snippet, but keep
                    // track of the max locals for all snippets in this phase.
                    //
                    phaseMaxLocals = Math.max (phaseMaxLocals, methodNode.maxLocals);
                    methodNode.maxLocals = initialMaxLocals;
                }

                //
                // Set max locals to the max level reached in this phase.
                //
                methodNode.maxLocals = phaseMaxLocals;
            }
        }

        static2Local(methodNode, syntheticLocalVars);
        AdvancedSorter.sort(methodNode);
    }

}
