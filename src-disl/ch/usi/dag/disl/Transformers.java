package ch.usi.dag.disl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class Transformers {

    private final List <Transformer> __transformers;

    //

    private Transformers (final List <Transformer> transformers) {
        // not to be instantiated from outside
        __transformers = transformers;
    }

    public byte [] apply (
        final byte [] originalBytes
    ) throws TransformerException {
        byte [] result = originalBytes;

        for (final Transformer transformer : __transformers) {
            try {
                final byte [] bytes = transformer.transform (result);
                if (bytes != null) {
                    result = bytes;
                }

            } catch (final Exception e) {
                throw new TransformerException (
                    e, "transformation failed in %s", transformer
                );
            }
        }

        return result;
    }

    //

    /**
     * Loads and instantiates {@link Transformer} classes.
     */
    public static Transformers load (final Stream <String> transformers) {
        try {
            return new Transformers (
                transformers
                    .map (className -> __createTransformer (className))
                    .collect (Collectors.toList ())
            );
        } catch (final Exception e) {
            throw new InitializationException (
                e, "failed to load class transformers"
            );
        }
    }


    private static Transformer __createTransformer (final String className) {
        final Class <?> resolvedClass = __resolveTransformer (className);
        if (Transformer.class.isAssignableFrom (resolvedClass)) {
            return __instantiateTransformer (resolvedClass);
        } else {
            throw new InitializationException (
                "%s does not implement %s",
                className, Transformer.class.getName ()
            );
        }
    }


    private static Class <?> __resolveTransformer (final String className) {
        try {
            return Class.forName (className);

        } catch (final Exception e) {
            throw new InitializationException (
                e, "failed to resolve transformer %s", className
            );
        }
    }


    private static Transformer __instantiateTransformer (
        final Class <?> transformerClass
    ) {
        try {
            return (Transformer) transformerClass.newInstance ();

        } catch (final Exception e) {
            throw new InitializationException (
                e, "failed to instantiate transformer %s",
                transformerClass.getName ()
            );
        }
    }

}
