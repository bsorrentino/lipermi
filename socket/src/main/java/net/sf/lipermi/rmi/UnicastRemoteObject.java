package net.sf.lipermi.rmi;

import net.sf.lipermi.handler.CallHandler;

import java.io.Serializable;

public class UnicastRemoteObject implements Serializable {

  static final CallHandler callHandler = new CallHandler();

  public static Object exportObject( Class<?> ifc, Object obj) throws Exception {
    callHandler.exportObject( ifc, obj);
    return obj;
  }
  public static boolean	unexportObject(Object obj, boolean force)  {
    throw new UnsupportedOperationException("unexportObject is not implemented yet!");
  }

  public UnicastRemoteObject() {
  }

  public UnicastRemoteObject(int port) {
  }

}
