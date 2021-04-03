package net.sf.lipermi.rmi;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.BaseClient;
import net.sf.lipermi.net.IServer;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public abstract class AbstractRegistry implements Registry {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractRegistry.class);

    private BaseClient _client = null;
    private IServer _server = null;

    private String _host = null;
    private final int port;

    public AbstractRegistry(String host, int port ) {
        this.port = port;
        this._host = host;
    }

    protected abstract <S extends IServer> S newServer() throws Exception;

    protected abstract <C extends BaseClient> C newClient(String host, int port, CallHandler callHandler, IProtocolFilter filter) throws Exception;

    Optional<String> getHost() {
        return ofNullable(_host);
    }

    @Override
    public <T, C extends T> void bind(Class<T> ifc, C obj) throws Exception {
        log.trace( "bind {}", ifc.getName());

        if( _client!=null ) {
            // WE ARE ON THE CLIENT
            UnicastRemoteObject.callHandler.exportObject(ifc,obj);
            return;
        }

        // WE ARE ON THE CLIENT
        if(_server==null) {

            _server = newServer();
            _server.bind( port, UnicastRemoteObject.callHandler, null );

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
                            new Exception( format("exported object with interface %s not found!", ifc.getName())));
        }

        // WE ARE ON THE CLIENT
        if( _client==null ) {

            final String host = getHost().orElseThrow( () -> new IllegalStateException("host has not been set!"));

            _client = newClient(host, port, UnicastRemoteObject.callHandler, null);

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
        throw new Exception("the registry is not connected yet!");
    }


}
