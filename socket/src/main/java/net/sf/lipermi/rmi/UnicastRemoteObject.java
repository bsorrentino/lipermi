package net.sf.lipermi.rmi;

import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.CallHandler;

import java.util.Optional;

public class UnicastRemoteObject {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnicastRemoteObject.class);

  static final CallHandler callHandler = new CallHandler();

  /**
   *
   * @param cInterface
   * @param objImplementation
   * @throws LipeRMIException
   */
  protected static void registerGlobal(Class cInterface, Object objImplementation) throws Exception {
    callHandler.registerGlobal(cInterface,objImplementation);
  }

  /**
   *
   * @param ifc
   * @param <T>
   * @return
   */
  public static <T> Optional<T> getExportedObject(Class<T> ifc) {
    return callHandler.getExportedObject(ifc);
  }

  /**
   *
   * @param ifc
   * @param obj
   * @return
   * @throws Exception
   */
  protected static Object exportObject( Class<?> ifc, Object obj) throws Exception {
    callHandler.exportObject( ifc, obj);
    return obj;
  }

  /**
   *
   * @param obj
   * @param force
   * @return
   */
  protected static boolean	unexportObject(Object obj, boolean force)  {
    log.warn( "unexportObject( obj:{}, force:{} ) is not implemented yet!", obj, force);
    return true;
    //throw new UnsupportedOperationException("unexportObject is not implemented yet!");
  }

  public UnicastRemoteObject() {
    log.trace( "new UnicastRemoteObject");
  }

  public UnicastRemoteObject(int port) {
    log.trace( "new UnicastRemoteObject({})", port);
  }

}
