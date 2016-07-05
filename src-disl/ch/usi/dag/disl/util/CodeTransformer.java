package ch.usi.dag.disl.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.tree.InsnList;


/**
 * Abstract code transformer. Takes a sequence of instructions and transforms it
 * in place.
 */
public interface CodeTransformer {

    /**
     * Transforms the given instruction sequence in-place.
     *
     * @param insns the instruction sequence to transform, must not be {@code null}
     */
    abstract void transform (InsnList insns);

    //

    static void apply (final InsnList insns, final CodeTransformer ... transformers) {
        Objects.requireNonNull (insns);
        Arrays.stream (transformers).forEachOrdered (t -> t.transform (insns));
    }


    static void apply (final InsnList insns, final List <CodeTransformer> transformers) {
        Objects.requireNonNull (insns);
        Objects.requireNonNull (transformers).stream ().forEachOrdered (
            t -> t.transform (insns)
        );
    }

}
