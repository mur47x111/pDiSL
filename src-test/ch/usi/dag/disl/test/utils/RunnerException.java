package ch.usi.dag.disl.test.utils;

@SuppressWarnings ("serial")
final class RunnerException extends RuntimeException {


    public RunnerException (final String format, final Object... args) {
        super (String.format (format, args));
    }

    public RunnerException (
        final Throwable cause, final String format, final Object... args
    ) {
        super (String.format (format, args), cause);
    }

}
