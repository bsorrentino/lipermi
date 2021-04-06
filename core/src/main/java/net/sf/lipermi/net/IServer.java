package net.sf.lipermi.net;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.IProtocolFilter;

import java.io.Closeable;
import java.io.IOException;

public interface IServer extends Closeable {

    void bind(int port, final CallHandler callHandler, IProtocolFilter filter) throws IOException;

}