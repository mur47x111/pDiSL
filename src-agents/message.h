#ifndef _MESSAGE_H_
#define _MESSAGE_H_

#include <jni.h>

#include "connection.h"
#include "shared/buffer.h"

static const jint INSTRUMENTATION_MESSAGE = 0;
static const jint ANALYSIS_MESSAGE = 1;

struct setup_message
{
    jint flags;
    jint msg_length;
    const uint8_t * msg;
};

struct instrumentation_message
{
    jint message_flags;
    jint control_size;
    jint classcode_size;
    const uint8_t * control;
    const uint8_t * classcode;
};


ssize_t message_send_setup(struct connection * conn, struct setup_message * msg);
void message_recv_setup (struct connection * conn, struct setup_message * msg);

ssize_t message_send_instr (struct connection * conn, struct instrumentation_message * msg);
void message_recv_instr (struct connection * conn, struct instrumentation_message * msg);


#endif /* _MESSAGE_H_ */