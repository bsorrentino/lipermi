package net.sf.lipermi.rmi;

public interface Registry {

  void bind(Class<?> ifc, Object obj) throws Exception;

  <T> T lookup(Class<T> ifc) throws Exception;

  default void rebind( Class<?> ifc, Object obj ) throws Exception {
    bind( ifc, obj );
  }

  default void unbind(Object object) {
    UnicastRemoteObject.unexportObject(object, true );
    //throw new UnsupportedOperationException("unbind is not supported yet!");
  }

  String[] list() throws Exception;

}
