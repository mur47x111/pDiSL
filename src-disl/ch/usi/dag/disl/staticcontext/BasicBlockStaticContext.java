package ch.usi.dag.disl.staticcontext;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.util.Insn;
import ch.usi.dag.disl.util.JavaNames;
import ch.usi.dag.disl.util.cfg.BasicBlock;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;


/**
 * Provides basic block related static context information for the method being
 * instrumented. method.
 * <p>
 * <b>Note:</b>This class is a work in progress, the API is being finalized.
 */
public class BasicBlockStaticContext extends AbstractStaticContext {

    private final Map <String, CtrlFlowGraph> __cfgCache = new HashMap <String, CtrlFlowGraph> ();

    protected CtrlFlowGraph methodCfg;

    //

    @Override
    public void staticContextData (final Shadow shadow) {
        super.staticContextData (shadow);

        final String key = JavaNames.methodUniqueName (
            staticContextData.getClassNode().name,
            staticContextData.getMethodNode().name,
            staticContextData.getMethodNode().desc
        );

        methodCfg = __cfgCache.computeIfAbsent (key, k -> createControlFlowGraph ());
    }


    protected CtrlFlowGraph createControlFlowGraph () {
        return new CtrlFlowGraph(staticContextData.getMethodNode());
    }


    /**
     * Returns total number of basic blocks in this method.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use the
     * {@link #getCount()} method instead.
     *
     * @return the number of basic blocks in this method.
     */
    @Deprecated
    public int getTotBBs () {
        return getCount ();
    }


    /**
     * Returns total number of basic blocks in this method.
     *
     * @return the number of basic blocks in this method.
     */
    public int getCount () {
        return methodCfg.getNodes ().size ();
    }


    /**
     * Calculates the size of this basic block in terms bytecode instructions.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use the
     * {@link #getSize()} method instead.
     *
     * @return size of this basic block.
     */
    @Deprecated
    public int getBBSize () {
        return getSize ();
    }


    /**
     * Calculates the size of this basic block in terms bytecode instructions.
     *
     * @return size of this basic block.
     */
    public int getSize () {
        return __getSize (getIndex ());
    }


    private int __getSize (final int index) {
        //
        // If the start instruction is also an end instruction,
        // then the size of the basic block is 1 instruction.
        //
        final BasicBlock bb = methodCfg.getNodes ().get (index);

        AbstractInsnNode insn = bb.getEntryNode ();
        final AbstractInsnNode exit = bb.getExitNode ();

        int result = 1;
        while (insn != exit) {
            result += Insn.isVirtual (insn) ? 0 : 1;
            insn = insn.getNext ();
        }

        return result;
    }


    /**
     * Returns the index of this basic block within the instrumented method.
     * <p>
     * <b>Note:</b> This method is being deprecated, please use the
     * {@link #getIndex()} method instead.
     *
     * @return index of this basic block within a method.
     */
    @Deprecated
    public int getBBindex () {
        return getIndex ();
    }


    /**
     * Returns the index of this basic block within the instrumented method.
     *
     * @return index of this basic block within a method.
     */
    public int getIndex () {
        return methodCfg.getIndex (staticContextData.getRegionStart ());
    }

}
