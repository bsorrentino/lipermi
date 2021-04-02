package net.sf.lipermi.rmi;

import net.sf.lipermi.Client;
import net.sf.lipermi.Server;
import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.filter.DefaultFilter;

import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 *
 */
public final class LocateRegistry {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocateRegistry.class);

  static class RegistryImpl implements Registry {
    private Client _client = null;
    private Server _server = null;

    private String _host = null;
    private final int port;

    public RegistryImpl(int port) {
      this.port = port;
    }

    public RegistryImpl(String host, int port) {
      this.port = port;
      this._host = host;
    }

    Optional<String> getHost() {
      return ofNullable(_host);

    }
    @Override
    public void bind(Class<?> ifc, Object obj) throws Exception {
      log.trace( "bind {}", ifc.getName());

      if( _client!=null ) {
        // WE ARE ON THE CLIENT
        UnicastRemoteObject.callHandler.exportObject(ifc,obj);
        return;
      }

      // WE ARE ON THE CLIENT
      if(_server==null) {

          _server = new Server();
          _server.bind( port, UnicastRemoteObject.callHandler, empty() );

      }

      UnicastRemoteObject.callHandler.registerGlobal( ifc, obj);
    }

    @Override
    public <T> T lookup(Class<T> ifc) throws Exception  {
      log.trace( "lookup {}", ifc.getName());

      // WE ARE ON THE SERVER
      if( _server!=null ) {

        return UnicastRemoteObject.callHandler.getExportedObject(ifc)
                .orElseThrow( () ->
                        new LipeRMIException( format("exported object with interface %s not found!", ifc.getName())));
      }

      // WE ARE ON THE CLIENT
      if( _client==null ) {

        final String host = getHost().orElseThrow( () -> new IllegalStateException("host has not been set!"));

        _client = new Client(host, port, UnicastRemoteObject.callHandler, new DefaultFilter());

      }

      return _client.getGlobal(ifc);
    }

    @Override
    public String[] list() throws Exception {

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
   * @return
   */
  public static Optional<Registry> getCurrentRegistry()  {
    return ofNullable(_singletonRegistry);
  }

    /**
     *
     * @param port
     * @return
     */
  public static Registry createRegistry(int port) throws Exception {
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
  public static Registry getRegistry(String host, int port) throws Exception {
    if( host == null ) throw new IllegalArgumentException("host argument is null!");
    log.trace( "getRegistry( host:{}, port:{} )", host, port);

    if( _singletonRegistry==null ) {
      _singletonRegistry= new RegistryImpl(host, port);
    }
    return _singletonRegistry;
  }

}
