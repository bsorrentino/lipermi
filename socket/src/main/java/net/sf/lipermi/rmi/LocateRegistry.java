package net.sf.lipermi.rmi;

import net.sf.lipermi.Client;
import net.sf.lipermi.Server;
import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.filter.DefaultFilter;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;

/**
 *
 */
public final class LocateRegistry {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocateRegistry.class);

  static class RegistryImpl implements Registry {
    private Client _client = null;
    private Server _server = null;

    private Optional<String> host = empty();
    private final int port;

    public RegistryImpl(int port) {
      this.port = port;
    }

    public RegistryImpl(String host, int port) {
      this.port = port;
      this.host = Optional.of(host);
    }

    @Override
    public void bind(Class<?> ifc, Object obj) throws LipeRMIException {
      log.trace( "bind {}", ifc.getName());

      if(_server==null) {

        try {
          _server = new Server();
          _server.bind( port, UnicastRemoteObject.callHandler, empty() );

        } catch (IOException ex) {
          log.error( "new Server error", ex );
          throw new LipeRMIException(ex);
        }

      }
      UnicastRemoteObject.callHandler.registerGlobal( ifc, obj);
    }

    @Override
    public <T> T lookup(Class<T> ifc) throws LipeRMIException  {
      log.trace( "lookup {}", ifc.getName());

      // WE ARE ON THE SERVER
      if( _server!=null ) {

        return UnicastRemoteObject.callHandler.getExportedObject(ifc)
                .orElseThrow( () ->
                        new LipeRMIException( format("exported object with interface %s not found!", ifc.getName())));
      }

      // WE ARE ON THE CLIENT
      if( !host.isPresent() ) throw new IllegalStateException("host has not been set!");

      if( _client==null ) {

        try {

          _client = new Client(host.get(), port, UnicastRemoteObject.callHandler, new DefaultFilter());

        } catch (IOException ex) {
          log.error( "new Client error", ex );
          throw new LipeRMIException(ex);
        }
      }

      return _client.getGlobal(ifc);
    }

    @Override
    public String[] list() throws LipeRMIException {

      if( _server!=null || _client!=null ) {
        return UnicastRemoteObject.callHandler.getExportedObjects()
                .stream()
                .map( ri -> ri.getClassName())
                .toArray( String[]::new );
      }
      throw new LipeRMIException("the registry is not connected yet!");
    }

  }

  private static RegistryImpl _singletonRegistry = null;

  /**
   *
   * @param port
   * @return
   */
  public static Registry createRegistry(int port) throws LipeRMIException {
    log.trace( "createRegistry( port:{} )", port);

    if( _singletonRegistry==null ) {
      _singletonRegistry = new RegistryImpl(port);
    }
    return _singletonRegistry;
  }

  /**
   *
   * @param port
   * @return
   */
  public static Registry getRegistry(String host, int port) throws LipeRMIException {
    if( host == null ) throw new IllegalArgumentException("host argument is null!");
    log.trace( "getRegistry( host:{}, port:{} )", host, port);

    try {

      if( _singletonRegistry==null ) {
        _singletonRegistry= new RegistryImpl(host, port);
      }
      return _singletonRegistry;
    }
    catch( Exception ex ) {
      log.error( "getRegistry error", ex );

      throw new LipeRMIException(ex);
    }
  }
}
