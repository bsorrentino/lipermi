package net.sf.lipermi;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.BaseClient;

import java.io.IOException;
import java.net.Socket;


/**
 * The LipeRMI client.
 * Connects to a LipeRMI Server in a address:port
 * and create local dynamic proxys to call remote
 * methods through a simple interface.
 *
 * @author lipe
 * @date   05/10/2006
 *
 * @see    net.sf.lipermi.handler.CallHandler
 * @see    BaseClient
 */
public class SocketClient extends BaseClient {

    public SocketClient(String address, int port, CallHandler callHandler, IProtocolFilter filter) throws IOException {
        super(new FullDuplexSocketStreamAdapter(new Socket( address, port )), callHandler, filter);
    }

}
