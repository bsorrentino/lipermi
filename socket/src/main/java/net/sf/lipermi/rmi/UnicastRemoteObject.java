package net.sf.lipermi.rmi;

import net.sf.lipermi.handler.CallHandler;

public class UnicastRemoteObject {

  static final CallHandler callHandler = new CallHandler();

  public static Object exportObject( Class<?> ifc, Object obj) throws Exception {
    callHandler.exportObject( ifc, obj);
    return obj;
  }

}
