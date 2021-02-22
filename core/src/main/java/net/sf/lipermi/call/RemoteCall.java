package net.sf.lipermi.call;

/**
 * Class that holds method call informations.
 *
 * @date   05/10/2006
 * @author lipe
 */
public class RemoteCall implements IRemoteMessage {

    private static final long serialVersionUID = -4057457700512552099L;

    /**
     * Instance will receive the call
     */
    RemoteInstance remoteInstance;

    /**
     * Method's name
     */
    String methodId;

    /**
     * Method's argument
     */
    Object[] args;

    /**
     * The id is a number unique in client and server to identify the call
     */
    Long callId;

    public Object[] getArgs() {
        return args;
    }

    public Long getCallId() {
        return callId;
    }

    public RemoteInstance getRemoteInstance() {
        return remoteInstance;
    }

    public String getMethodId() {
        return methodId;
    }

    public RemoteCall(RemoteInstance remoteInstance, String methodId, Object[] args, Long callId) {
        this.remoteInstance = remoteInstance;
        this.methodId = methodId;
        this.args = args;
        this.callId = callId;
    }

}
