package net.sf.lipermi.handler;

import net.sf.lipermi.call.RemoteCall;
import net.sf.lipermi.call.RemoteInstance;
import net.sf.lipermi.call.RemoteReturn;
import net.sf.lipermi.exception.LipeRMIException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static java.lang.String.format;


/**
 * A handler who know a RemoteInstance and it
 * local implementations. Used to delegate calls to
 * correct implementation objects.
 *
 * Local implementation objects must register with
 * methods {@link net.sf.lipermi.handler.CallHandler#registerGlobal registerGlobal} and
 * {@link net.sf.lipermi.handler.CallHandler#exportObject exportObject} to work remotelly.
 *
 * @author lipe
 * @date   05/10/2006
 *
 * @see       net.sf.lipermi.call.RemoteInstance
 */
public class CallHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CallHandler.class);

    private Map<RemoteInstance, Object> exportedObjects = new HashMap<RemoteInstance, Object>();


    public void registerGlobal(Class cInterface, Object objImplementation) throws LipeRMIException {
        exportObject(cInterface, objImplementation, null);
    }

    public void exportObject(Class<?> cInterface, Object exportedObject) throws LipeRMIException {
        UUID objUUID = java.util.UUID.randomUUID();
        String instanceId = objUUID.toString();

        exportObject(cInterface, exportedObject, instanceId);
    }

    private void exportObject(Class<?> cInterface, Object objImplementation, String instanceId) throws LipeRMIException {

        log.trace( "exportObject( class:{}, impl:{}, id:{}", cInterface,objImplementation,instanceId );

        if (!cInterface.isAssignableFrom(objImplementation.getClass()))
            throw new LipeRMIException(format("Class %s is not assignable from %s", objImplementation.getClass().getName(), cInterface.getName())); //$NON-NLS-1$

        for (RemoteInstance remoteInstance : exportedObjects.keySet()) {
            if ((remoteInstance.getInstanceId() == instanceId || (remoteInstance.getInstanceId() != null && remoteInstance.getInstanceId().equals(instanceId))) && remoteInstance.getClassName().equals(cInterface.getName())) {
                throw new LipeRMIException(format("Class %s already has a implementation class", cInterface.getName()));                 //$NON-NLS-1$
            }
        }

        final RemoteInstance remoteInstance = new RemoteInstance(instanceId, cInterface.getName());
        exportedObjects.put(remoteInstance, objImplementation);
    }

    public RemoteReturn delegateCall(RemoteCall remoteCall) throws LipeRMIException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException {

        final Object implementator = exportedObjects.get(remoteCall.getRemoteInstance());
        if (implementator == null)
            throw new LipeRMIException(format("Class %s doesn't have implementation", remoteCall.getRemoteInstance().getClassName()));

        RemoteReturn remoteReturn;

        Method implementationMethod = null;

        for (Method method : implementator.getClass().getMethods()) {
            String implementationMethodId = method.toString();
            implementationMethodId = implementationMethodId.replace(implementator.getClass().getName(), remoteCall.getRemoteInstance().getClassName());

            if (implementationMethodId.endsWith(remoteCall.getMethodId())) {
                implementationMethod = method;
                break;
            }
        }

        if (implementationMethod == null)
            throw new NoSuchMethodException(remoteCall.getMethodId());

        try {

            implementationMethod.setAccessible(true);

            Object methodReturn = implementationMethod.invoke(implementator, remoteCall.getArgs());

            if (exportedObjects.containsValue(methodReturn)) {
                methodReturn = getRemoteReference(methodReturn).orElse(null);
            }

            remoteReturn = new RemoteReturn(false, methodReturn, remoteCall.getCallId());
        } catch (InvocationTargetException e) {
            remoteReturn = new RemoteReturn(true, e, remoteCall.getCallId());
        }

        return remoteReturn;
    }

    /**
     *
     * @return all exported objects
     */
    public Collection<RemoteInstance> getExportedObjects() {
        return exportedObjects.keySet();
    }

    /**
     *
     * @param ifc
     * @param <T>
     * @return
     */
    public <T> Optional<T> getExportedObject(Class<T> ifc ) {
        return exportedObjects.entrySet().stream()
                .filter( e -> e.getKey().getClassName().equals(ifc.getName()) )
                .map( e -> (T)e.getValue() )
                .findFirst();
    }

    /**
     *
     * @param obj
     * @return
     */
    Optional<RemoteInstance> getRemoteReference(Object obj) {
        return exportedObjects.entrySet().stream()
                .filter( e -> obj == e.getValue() )
                .map( e -> e.getKey() )
                .findFirst();
//        for (RemoteInstance remoteInstance : exportedObjects.keySet()) {
//            Object exportedObj = exportedObjects.get(remoteInstance);
//            if (exportedObj == obj)
//                return remoteInstance;
//        }
//        return null;
    }

    public static Class<?>[] typeFromObjects(Object[] objects) {
        Class<?>[] argClasses = null;
        if (objects != null) {
            argClasses = new Class[objects.length];
            for (int n = 0; n < objects.length; n++) {
                Object obj = objects[n];
                argClasses[n++] = obj.getClass();
            }
        }
        return argClasses;
    }
}
