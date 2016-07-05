package ch.usi.dag.disl;


/**
 * Represents a general DiSL exception, which is a superclass for all
 * DiSL exception classes.
 *
 * @author Lubomir Bulej
 */
@SuppressWarnings ("serial")
public abstract class GeneralException extends RuntimeException {

    protected GeneralException (final String message) {
        super (message);
    }

    protected GeneralException (final Throwable cause) {
        super (cause);
    }

    protected GeneralException (
        final String format, final Object ... args
    ) {
        super (String.format (format, args));
    }

    protected GeneralException (
        final Throwable cause, final String format, final Object ... args
    ) {
        super (String.format (format, args), cause);
    }

}
