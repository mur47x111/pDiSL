package ch.usi.dag.dislserver;

/**
 * Created by alexandernorth on 24/08/16.
 */
public class SetupMessage {
    private final int flags;
    private final String msg;

    public SetupMessage(int flags, String msg) {
        this.flags = flags;
        this.msg = msg;
    }

    public int getType(){
        return getFlags();
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
