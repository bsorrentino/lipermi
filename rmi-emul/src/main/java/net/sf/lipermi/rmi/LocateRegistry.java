package net.sf.lipermi.rmi;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 *
 */
public class LocateRegistry {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocateRegistry.class);

  @FunctionalInterface
  public interface RegistryProvider {
    Registry newRegistry( String host, int port ) throws Exception;
  }

  /**
   *
   */
  private static RegistryProvider _registryProvider;

  /**
   *
   * @param provider
   */
  public static void registerRegistryProvider( RegistryProvider provider ) {
    requireNonNull( provider, "Registry provider canargument is null!");
    _registryProvider = provider;
  }

  /**
   *
   */
  private static Registry _singletonRegistry = null;

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
    requireNonNull(_registryProvider, "Registry provider cannot be null!");
    log.trace( "createRegistry( port:{} )", port);

    if( _singletonRegistry==null ) {
      _singletonRegistry = _registryProvider.newRegistry(null,port);
    }
    return _singletonRegistry;
  }

  /**
   *
   * @param port
   * @return
   */
  public static Registry getRegistry(String host, int port) throws Exception {
    requireNonNull("Registry provider cannot be null!");
    requireNonNull( host, "host argument is null!");

    log.trace( "getRegistry( host:{}, port:{} )", host, port);

    if( _singletonRegistry==null ) {
      _singletonRegistry= _registryProvider.newRegistry(host, port);
    }
    return _singletonRegistry;
  }

}
