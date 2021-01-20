package net.sf.lipermi.rmi;

import net.sf.lipermi.Client;
import net.sf.lipermi.Server;
import net.sf.lipermi.exception.LipeRMIException;

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
    private Optional<Client> lazyClient = empty();
    private Optional<Server> lazyServer = empty();

    private Optional<String> host = empty();
    private final int port;

    public RegistryImpl(int port) {
      this.port = port;
    }

    public RegistryImpl(String host, int port) {
      this.port = port;
      this.host = Optional.of(host);
    }

    public void setHost( String value ) {
      if( host.isPresent() ) {

        if (!host.get().equalsIgnoreCase(value)) {
          throw new IllegalStateException(format("host is already set %s with different given value %s!", host.get(), value));
        }

        return; // ignored
      }

      host = Optional.of(value);
    }

    @Override
    public void bind(Class<?> ifc, Object obj) throws LipeRMIException {
      log.trace( "bind {}", ifc.getName());

      if(!lazyServer.isPresent()) {

        try {
          final Server server = new Server();
          server.bind( port, UnicastRemoteObject.callHandler, empty() );

          lazyServer = Optional.of(server);

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
      if( lazyServer.isPresent() ) {

        return UnicastRemoteObject.callHandler.getExportedObject(ifc)
                .orElseThrow( () ->
                        new LipeRMIException( format("exported object with interface %s not found!", ifc.getName())));
      }

      // WE ARE ON THE CLIENT
      if( !host.isPresent() ) throw new IllegalStateException("host has not been set!");

      if( !lazyClient.isPresent() ) {

        try {

          final Client client = new Client(host.get(), port, UnicastRemoteObject.callHandler, empty());
          lazyClient = Optional.of(client);

        } catch (IOException ex) {
          log.error( "new Client error", ex );
          throw new LipeRMIException(ex);
        }
      }
      return lazyClient.get().getGlobal(ifc);
    }

    @Override
    public String[] list() throws LipeRMIException {

      if( lazyServer.isPresent() || lazyClient.isPresent() ) {
        return UnicastRemoteObject.callHandler.getExportedObjects()
                .stream()
                .map( ri -> ri.getClassName())
                .toArray( String[]::new );
      }
      throw new LipeRMIException("the registry is not connected yet!");
    }

  }

  private static Optional<RegistryImpl> lazySingletonRegistry = empty();


  /**
   *
   * @param port
   * @return
   */
  public static Registry createRegistry(int port) throws LipeRMIException {
    log.trace( "createRegistry( port:{} )", port);

    if( !lazySingletonRegistry.isPresent() ) {
      final RegistryImpl reg = new RegistryImpl(port);
      lazySingletonRegistry = Optional.of( reg );
    }
    return lazySingletonRegistry.get();
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

      if( !lazySingletonRegistry.isPresent() ) {
        final RegistryImpl reg = new RegistryImpl(host, port);
        lazySingletonRegistry = Optional.of( reg );
      }
      lazySingletonRegistry.get().setHost(host);
      return lazySingletonRegistry.get();
    }
    catch( Exception ex ) {
      log.error( "getRegistry error", ex );

      throw new LipeRMIException(ex);
    }
  }
}
