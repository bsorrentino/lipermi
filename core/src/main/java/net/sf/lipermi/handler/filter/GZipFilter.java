package net.sf.lipermi.handler.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.sf.lipermi.call.IRemoteMessage;


/**
 * GZip filter to compact data using GZip I/O streams.
 *
 * @author lipe
 * @date   07/10/2006
 *
 * @see net.sf.lipermi.handler.filter.DefaultFilter
 */
public class GZipFilter implements IProtocolFilter {

    public IRemoteMessage readObject(Object obj) {
        IRemoteMessage remoteMessage = null;
        GZIPInputStream gzis = null;
        ObjectInputStream ois = null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) obj);
            gzis = new GZIPInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = gzis.read()) != -1)
                baos.write(b);

            gzis.close();

            byte[] extractedObj = baos.toByteArray();

            bais = new ByteArrayInputStream(extractedObj);
            ois = new ObjectInputStream(bais);
            remoteMessage = (IRemoteMessage) ois.readUnshared();
            ois.close();
        }
        catch (Exception e) {
            throw new RuntimeException("Can't read message", e); //$NON-NLS-1$
        }
        finally {
            if (gzis != null)
                try {
                    gzis.close();
                } catch (IOException e) {}

            if (ois != null)
                try {
                    ois.close();
                } catch (IOException e) {}
        }
        return remoteMessage;
    }

    public Object prepareWrite(IRemoteMessage message) {
        Object objectToWrite = message;

        ObjectOutputStream oos = null;
        GZIPOutputStream gzos = null;
        try {
            // serialize obj
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            // oos.reset(); -- not needed here because the oos i
            //                 always a new instance, reseted.
            oos.writeUnshared(message);
            byte[] byteObj = baos.toByteArray();

            baos.reset();

            // compact the serialization
            gzos = new GZIPOutputStream(baos);
            gzos.write(byteObj);
            gzos.finish();
            byteObj = baos.toByteArray();

            objectToWrite = byteObj;
        }
        catch (Exception e) {
            throw new RuntimeException("Can't prepare message", e); //$NON-NLS-1$
        }
        finally {
            if (gzos != null)
                try {
                    gzos.close();
                } catch (IOException e) {}

            if (oos != null)
                try {
                    oos.close();
                } catch (IOException e) {}
        }
        return objectToWrite;
    }
}
