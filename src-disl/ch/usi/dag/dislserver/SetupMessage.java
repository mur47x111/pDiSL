package ch.usi.dag.dislserver;

/**
 * Created by alexandernorth on 24/08/16.
 */
public class SetupMessage {

    public enum MessageType {

        INSTRUMENTATION_MESSAGE(Flag.INSTRUMENTATION_MESSAGE),
        ANALYSIS_MESSAGE(Flag.ANALYSIS_MESSAGE),
        INVALID_MESSAGE(Flag.INVALID_MESSAGE);

        private final int __flag;

        private MessageType (final int flag) {
            __flag = flag;
        }

        /**
         * Flags corresponding to individual code options. The flags are
         * used when communicating with DiSL agent.
         */
        public interface Flag {
            static final int INSTRUMENTATION_MESSAGE = 0;
            static final int ANALYSIS_MESSAGE = 1;
            static final int INVALID_MESSAGE = 2;
        }
    }

    private final int flags;
    private final String msg;

    public SetupMessage(int flags, String msg) {
        this.flags = flags;
        this.msg = msg;
    }

    public MessageType getType(){

        for (final MessageType option : MessageType.values ()) {
            if ((flags == option.__flag)) {
                return option;
            }
        }
        return MessageType.INVALID_MESSAGE;
    }

    public int getFlags() {
        return flags;
    }

    public String getMsg() {
        return msg;
    }

    public final static SetupMessage setupSuccessfulMessage(){
        return new SetupMessage(~0, "");
    }

    public final static SetupMessage setupFailedMessage(String failureMessage){
        return new SetupMessage(0, failureMessage);
    }
}
