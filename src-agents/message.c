#include "message.h"

#include "common.h"
#include "connection.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef MINGW
#include <sys/uio.h>
#endif

//

#ifdef MINGW

static inline ssize_t
__send_setup (struct connection * conn, struct setup_message * msg, void * header, const size_t header_size) {
    ssize_t sent = connection_send (conn, header, header_size);
    sent += connection_send (conn, msg->msg, msg->msg_length);
    return sent;
}

static inline ssize_t
__send (struct connection * conn, struct instrumentation_message * msg, void * header, const size_t header_size) {
    ssize_t sent = connection_send (conn, header, header_size);
    sent += connection_send (conn, msg->control, msg->control_size)
    sent += connection_send (conn, msg->classcode, msg->classcode_size);
    
    return sent;
}


static inline void
__recv (struct connection * conn, void * control, const size_t control_size, void * classcode, const size_t classcode_size) {
    connection_recv (conn, control, control_size);
    connection_recv (conn, classcode, classcode_size);
}

#else

static inline ssize_t
__send_setup (struct connection * conn, struct setup_message * msg, void * header, const size_t header_size) {
    struct iovec iovs[] = {
        { .iov_base = header, .iov_len = header_size },
        { .iov_base = msg->msg, .iov_len = msg->msg_length },
    };
    return connection_send_iov (conn, &iovs [0], sizeof_array (iovs));

}

static inline ssize_t
__send (struct connection * conn, struct instrumentation_message * msg, void * header, const size_t header_size) {
    struct iovec iovs[] = {
        { .iov_base = header, .iov_len = header_size },
        { .iov_base = (void *) msg->control, .iov_len = msg->control_size },
        { .iov_base = (void *) msg->classcode, .iov_len = msg->classcode_size }
    };
    return connection_send_iov (conn, &iovs [0], sizeof_array (iovs));

}


static inline void
__recv (struct connection * conn, void * control, const size_t control_size, void * classcode, const size_t classcode_size) {
    struct iovec iovs [2] = {
        { .iov_base = control, .iov_len = control_size },
        { .iov_base = classcode, .iov_len = classcode_size }
    };

    connection_recv_iov (conn, &iovs [0], sizeof_array (iovs));
}

#endif /* !MINGW */



static inline uint8_t *
__alloc_buffer (size_t len) {
    //
    // Allocate a buffer with an extra (zeroed) byte, but only if the
    // requested buffer length is greater than zero. Return NULL otherwise.
    //
    if (len == 0) {
        return NULL;
    }

    //

    uint8_t * buf = (uint8_t *) malloc (len + 1);
    check_error (buf == NULL, "failed to allocate buffer");

    buf [len] = '\0';
    return buf;
}

ssize_t message_send_setup(struct connection * conn, struct setup_message * msg){
    jint ints [] = {
        htonl (msg->flags),
        htonl(msg->msg_length)
    };

    ssize_t sent = __send_setup (conn, msg, &ints [0], sizeof (ints));
    assert (sent == (ssize_t) (sizeof (ints)));

    //

    dprintf ("sent %ld bytes ... done\n", sent);
}

void message_recv_setup (struct connection * conn, struct setup_message * msg){
    assert (conn != NULL);
    assert (msg != NULL);

    dlprintf ("receiving message: ");

    jint ints [2];
    connection_recv (conn, &ints [0], sizeof (ints));


    jint response_flags = ntohl (ints [0]);

    //

    jint control_size = ntohl (ints [1]);

    uint8_t * control = __alloc_buffer (control_size);

    __recv (conn, control, control_size, NULL, 0);

    //
    // Update message fields only after the whole message was read.
    //
    msg->flags = response_flags;
    msg->msg_length = control_size;
    msg->msg = control;
}


ssize_t message_send_instr (struct connection * conn, struct instrumentation_message * msg){
    assert (conn != NULL);
    assert (msg != NULL);


    dprintf (
        "sending message: flags %08x, control %d, code %d ... ",
        msg->message_flags,
        msg->control_size,
        msg->classcode_size
        );

    //

    jint ints [] = {
        htonl (msg->message_flags),
        htonl (msg->control_size),
        htonl (msg->classcode_size)
    };

    ssize_t sent = __send (conn, msg, &ints [0], sizeof (ints));
    assert (sent == (ssize_t) (sizeof (ints) + msg->control_size + msg->classcode_size));

    //

    dprintf ("sent %ld bytes ... done\n", sent);

    return sent;
}

void message_recv_instr (struct connection * conn, struct instrumentation_message * msg){
    assert (conn != NULL);
    assert (msg != NULL);

    dlprintf ("receiving message: ");

    //
    // First, receive the flags, the control and class code sizes.
    // Second, receive the control and class code data.
    // The ordering of receive calls is determined by the protocol.
    //
    jint ints [3];
    connection_recv (conn, &ints [0], sizeof (ints));


    jint response_flags = ntohl (ints [0]);

    //

    jint control_size = ntohl (ints [1]);
    uint8_t * control = __alloc_buffer (control_size);

    jint classcode_size = ntohl (ints [2]);
    uint8_t * classcode = __alloc_buffer (classcode_size);

    __recv (conn, control, control_size, classcode, classcode_size);

    //
    // Update message fields only after the whole message was read.
    //
    msg->message_flags = response_flags;
    msg->control_size = control_size;
    msg->classcode_size = classcode_size;
    msg->control = control;
    msg->classcode = classcode;

    //

    dprintf (
        "flags %08x, control %d, code %d ... done\n",
        response_flags, control_size, classcode_size
        );
}

