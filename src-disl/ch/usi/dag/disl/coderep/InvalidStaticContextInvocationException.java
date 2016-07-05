package ch.usi.dag.disl.coderep;

import org.objectweb.asm.tree.MethodInsnNode;


@SuppressWarnings ("serial")
final class InvalidStaticContextInvocationException extends RuntimeException {

    private final MethodInsnNode __insn;

    //

    public InvalidStaticContextInvocationException (
        final String message, final MethodInsnNode insn
    ) {
        super (message);
        __insn = insn;
    }

    public MethodInsnNode insn () {
        return __insn;
    }
}
