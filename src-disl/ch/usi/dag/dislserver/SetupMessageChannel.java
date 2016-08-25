package ch.usi.dag.dislserver;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

/**
 * Created by alexandernorth on 24/08/16.
 */
public class SetupMessageChannel implements Closeable {
    private final SocketChannel __socket;

    private final ByteBuffer __head = ByteBuffer.allocateDirect (8).order (ByteOrder.BIG_ENDIAN);
    private ByteBuffer __body = ByteBuffer.allocateDirect (128 * 1024);

    private final ByteBuffer [] __sendBuffers = new ByteBuffer [] {
            __head, null
    };

    //

    public SetupMessageChannel(final SocketChannel socket) {
        __socket = socket;
    }

    //

    public SetupMessage recvMessage () throws IOException {
        //
        // request protocol:
        //
        // java int - flags
        // java int - message length (ml)
        // bytes[ml] - message
        //

        __head.rewind ();

        do {
            __socket.read (__head);
        } while (__head.hasRemaining ());

        //

        __head.rewind ();

        final int flags = __head.getInt ();
        final int messageLength = __head.getInt ();

        //

        __ensureBodyCapacity (messageLength);

        __body.rewind ();

        do {
            __socket.read (__body);
        } while (__body.hasRemaining ());

        //

        __body.rewind ();

        final byte [] message = new byte [messageLength];
        __body.get (message);

        return new SetupMessage(flags, new String(message));
    }


    public void sendMessage (final SetupMessage message) throws IOException {
        //
        // response protocol:
        //
        // java int - response flags
        // java int - control data length (cdl)
        // java int - payload data length (pdl)
        // bytes[cdl] - control data (nothing, or error message)
        // bytes[pdl] - payload data (instrumented class code)
        //

        __head.rewind ();

        __head.putInt (message.getFlags ());

        final byte[] msgBytes = message.getMsg ().getBytes();

        final int msgLength = msgBytes.length;
        __head.putInt (msgLength);

        //

        __sendBuffers [1] = ByteBuffer.wrap (msgBytes);

        //

        __head.rewind ();

        do {
            __socket.write (__sendBuffers);
        } while (__sendBuffers [1].hasRemaining ());
    }


    private void __ensureBodyCapacity (final int length) {
        if (__body.capacity () < length) {
            __body = ByteBuffer.allocateDirect (__align (length, 12));
        }

        __body.limit (length);
    }

    private static int __align (final int value, final int bits) {
        final int mask = -1 << bits;
        final int fill = (1 << bits) - 1;
        return (value + fill) & mask;
    }

    @Override
    public void close() throws IOException {
        __socket.close();
    }
}
