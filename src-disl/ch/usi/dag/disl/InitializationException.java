package ch.usi.dag.disl;

/**
 * Represents a class of exceptions that can be thrown during DiSL
 * initialization.
 *
 * @author Lubomir Bulej
 */
@SuppressWarnings ("serial")
public class InitializationException extends GeneralException {

    public InitializationException (final String message) {
        super (message);
    }

    public InitializationException (final Throwable cause) {
        super (cause);
    }

    public InitializationException (
        final String format, final Object ... args
    ) {
        super (format, args);
    }

    public InitializationException (
        final Throwable cause, final String format, final Object ... args
    ) {
        super (cause, format, args);
    }

}
