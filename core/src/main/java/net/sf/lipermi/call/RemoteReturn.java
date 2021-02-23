package net.sf.lipermi.call;

/**
 * Class that holds method return information.
 *
 * @date   05/10/2006
 * @author lipe
 */
public class RemoteReturn implements IRemoteMessage {

    private static final long serialVersionUID = -2353656699817180281L;

    /**
     * The return is a throwable to be thrown?
     */
    boolean throwing;

    /**
     * Returning object
     */
    Object ret;

    /**
     * Call id which generated this return
     */
    long callId;

    public Long getCallId() {
        return callId;
    }

    public Object getRet() {
        return ret;
    }

    public boolean isThrowing() {
        return throwing;
    }

    public RemoteReturn(boolean throwing, Object ret, long callId) {
        this.throwing = throwing;
        this.ret = ret;
        this.callId = callId;
    }

}
