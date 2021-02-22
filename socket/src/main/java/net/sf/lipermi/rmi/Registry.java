package net.sf.lipermi.rmi;

public interface Registry {

  void bind(Class<?> ifc, Object obj) throws Exception;

  <T> T lookup(Class<T> ifc) throws Exception;

  default void rebind( Class<?> ifc, Object obj ) throws Exception {
    bind( ifc, obj );
  }

  default void unbind(Class<?> ifc) {
    throw new UnsupportedOperationException("unbind is not supported yet!");
  }

  String[] list() throws Exception;

}
