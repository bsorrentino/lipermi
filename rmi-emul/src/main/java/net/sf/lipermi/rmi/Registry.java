package net.sf.lipermi.rmi;

public interface Registry {

  <T, C extends T> void bind(Class<T> ifc, C obj) throws Exception;

  <T> T lookup(Class<T> ifc) throws Exception;

  default <T, C extends T> void rebind( Class<T> ifc, C obj ) throws Exception {
    bind( ifc, obj );
  }

  default void unbind(Object object) {
    UnicastRemoteObject.unexportObject(object, true );
    //throw new UnsupportedOperationException("unbind is not supported yet!");
  }

  String[] list() throws Exception;

}
