package ch.usi.dag.dislserver;


import ch.usi.dag.disl.DiSL;

final class InstrMessage {

    private static final byte [] __EMPTY_ARRAY__ = new byte [0];

    //

    private final int __flags;

    private final byte [] __control;

    private final byte [] __payload;

    //

    public InstrMessage(
        final int flags, final byte [] control, final byte [] payload
    ) {
        __flags = flags;
        __control = control;
        __payload = payload;
    }

    //

    public int flags () {
        return __flags;
    }


    public byte [] control () {
        return __control;
    }


    public byte [] payload () {
        return __payload;
    }

    public String instrumentationJarPath () { return this.isSetupMessage() ? new String(this.payload()) : null; }

    //

    public boolean isShutdown () {
        return (__control.length == 0) && (__payload.length == 0);
    }


    public boolean isSetupMessage () { return (__flags & (DiSL.CodeOption.Flag.SETUP_MESSAGE)) != 0; }

    //

    /**
     * Creates a message containing a modified class bytecode.
     *
     * @param bytecode
     *      the bytecode of the modified class.
     */
    public static InstrMessage createClassModifiedResponse (final byte [] bytecode) {
        //
        // The flags are all reset, the control part of the network message
        // is empty, and the payload contains the modified class bytecode.
        //
        return new InstrMessage(0, __EMPTY_ARRAY__, bytecode);
    }


    /**
     * Creates a message indicating that a class was not modified.
     */
    public static InstrMessage createNoOperationResponse () {
        //
        // The flags are all reset, and both the control part and the
        // payload parts of the network message are empty.
        //
        return new InstrMessage(0, __EMPTY_ARRAY__, __EMPTY_ARRAY__);
    }

    /**
     * Creates a message indicating that a class was not modified.
     */
    public static InstrMessage createSetupSucceededResponse () {
        //
        // The flag is set to SETUP_MESSAGE, and both the control part and the
        // payload parts of the network message are empty.
        //
        return new InstrMessage(DiSL.CodeOption.Flag.SETUP_MESSAGE, __EMPTY_ARRAY__, __EMPTY_ARRAY__);
    }

    /**
     * Creates a message indicating a server-side error.
     */
    public static InstrMessage createErrorResponse (final String error) {
        //
        // The flags are all set, the control part of the network message
        // contains the error message, and the payload is empty.
        //
        return new InstrMessage(-1, error.getBytes (), __EMPTY_ARRAY__);
    }

}
