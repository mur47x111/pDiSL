package ch.usi.dag.disl.scope;

import ch.usi.dag.disl.GeneralException;


@SuppressWarnings ("serial")
final class MalformedScopeException extends GeneralException {

    protected MalformedScopeException (final String message) {
        super (message);
    }

    protected MalformedScopeException (
        final String format, final Object ... args
    ) {
        super (format, args);
    }

}
