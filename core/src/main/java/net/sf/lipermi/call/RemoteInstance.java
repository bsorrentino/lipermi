package net.sf.lipermi.call;

import java.io.Serializable;

/**
 * Class that holds informations about a remote instance,
 * making the instance unique in all remote JVM.
 * All remote instances have a generated random UUID,
 * except the global ones (registered with {@link net.sf.lipermi.handler.CallHandler#registerGlobal CallHandler}).
 *
 * @date   05/10/2006
 * @author lipe
 */

public class RemoteInstance implements Serializable {

    private static final long serialVersionUID = -4597780264243542810L;

    final String instanceId;
    final String className;

    public String getClassName() {
        return className;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public RemoteInstance(String instanceId, String className) {
        this.instanceId = instanceId;
        this.className = className;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoteInstance) {
            RemoteInstance ri = (RemoteInstance) obj;
            boolean instanceId = (getInstanceId() == ri.getInstanceId() || (getInstanceId() != null && getInstanceId().equals(ri.getInstanceId())));
            boolean className = (getClassName().equals(ri.getClassName()));
            return (className && instanceId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

}
