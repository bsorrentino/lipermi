package net.sf.lipermi.rmi;

import net.sf.lipermi.exception.LipeRMIException;

public interface Registry {

  void bind(Class<?> ifc, Object obj) throws LipeRMIException;

  <T> T lookup(Class<T> ifc);

  default void rebind( Class<?> ifc, Object obj ) throws LipeRMIException {
    bind( ifc, obj );
  }

  default void unbind(Class<?> ifc) {
    throw new UnsupportedOperationException("unbind is not supported yet!");
  }

  default Class<?>[]	list() {
    return new Class<?>[] {};
  }

}
