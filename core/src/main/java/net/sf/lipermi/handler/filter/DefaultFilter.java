package net.sf.lipermi.handler.filter;

import net.sf.lipermi.call.IRemoteMessage;

/**
 * Default protocol filter implementation.
 * Only forwards the data.
 *
 * @see net.sf.lipermi.handler.filter.GZipFilter
 */
public class DefaultFilter implements IProtocolFilter {

    public IRemoteMessage readObject(Object obj) {
        return (IRemoteMessage) obj;
    }

    public Object prepareWrite(IRemoteMessage message) {
        return message;
    }

}
