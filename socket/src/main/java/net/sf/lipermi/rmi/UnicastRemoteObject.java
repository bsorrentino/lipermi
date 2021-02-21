package net.sf.lipermi.rmi;

import net.sf.lipermi.Client;
import net.sf.lipermi.handler.CallHandler;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class UnicastRemoteObject {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnicastRemoteObject.class);

  static final CallHandler callHandler = new CallHandler();

  private static Client _client = null;

  public static Optional<Client> getClient() {
    return ofNullable(_client);
  }

  static void setClient(Client client) {
    _client = client;
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
  public static Object exportObject( Class<?> ifc, Object obj) throws Exception {
    callHandler.exportObject( ifc, obj);
    return obj;
  }

  /**
   *
   * @param obj
   * @param force
   * @return
   */
  public static boolean	unexportObject(Object obj, boolean force)  {
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
