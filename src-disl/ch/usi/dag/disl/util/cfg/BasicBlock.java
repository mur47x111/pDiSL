package ch.usi.dag.disl.util.cfg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.exception.DiSLFatalException;


public class BasicBlock implements Iterable <AbstractInsnNode> {

    /** Index of this basic block within a method. */
    private final int __index;

    /** The entry instruction of this basic block. */
    private final AbstractInsnNode __entryNode;

    /** The exit instruction of this basic block. */
    private AbstractInsnNode __exitNode;

    /** The set of predecessor basic blocks. */
    private final Set<BasicBlock> predecessors = new HashSet <> ();

    /** The set of successor basic blocks. */
    private final Set<BasicBlock> successors = new HashSet <> ();

    // joins refer to the join point of a new cfg to an existing cfg in the
    // same method. NOTE that an exception handler is regarded as a new cfg
    // but not included in the normal execution cfg
    private final Set<BasicBlock> joins = new HashSet <> ();

    //

    public BasicBlock (
        final int index,
        final AbstractInsnNode entryNode, final AbstractInsnNode exitNode
    ) {
        __index = index;
        __entryNode = entryNode;
        __exitNode = exitNode;
    }

    public int getIndex() {
        return __index;
    }


    public AbstractInsnNode getEntryNode () {
        return __entryNode;
    }


    public void setExitNode (final AbstractInsnNode exitNode) {
        __exitNode = exitNode;
    }


    public AbstractInsnNode getExitNode () {
        return __exitNode;
    }


    public Set <BasicBlock> getPredecessors () {
        return predecessors;
    }


    public Set <BasicBlock> getSuccessors () {
        return successors;
    }


    public Set <BasicBlock> getJoins () {
        return joins;
    }

    //

    @Override
    public Iterator <AbstractInsnNode> iterator () {
        return new BasicBlockIterator ();
    }

    //

    class BasicBlockIterator implements Iterator <AbstractInsnNode> {

        private AbstractInsnNode current;


        public BasicBlockIterator () {
            current = __entryNode;
        }


        @Override
        public boolean hasNext () {
            return current != __exitNode.getNext ();
        }


        @Override
        public AbstractInsnNode next () {
            final AbstractInsnNode result = current;
            current = current.getNext ();
            return result;
        }


        @Override
        public void remove () {
            throw new DiSLFatalException ("Readonly iterator.");
        }

    }

}
