package org.bsc.nonblockingsocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

import static java.nio.ByteBuffer.allocateDirect;
import static java.util.Optional.ofNullable;

/**
 *
 */
public abstract class AbstractSocketChannelServerAsync<C extends SelectableChannel> implements Closeable {
    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractSocketChannelServerAsync.class);

    public static final byte EOT = 0x04; // End Of Transmission

    /**
     *
     */
    public class SelectionKeyAttachment {
        public ByteBuffer readBuffer = allocateDirect(1024);
        public ByteBuffer writeBuffer;
    }

    private boolean enabled;

    @Override
    public void close() throws IOException {
        enabled = false;
    }

    /**
     *
     * @param selector
     * @param channel
     * @throws IOException
     */
    private void accept(Selector selector, C channel) throws IOException {
        final SocketChannel client = ((ServerSocketChannel)channel).accept();
        final SelectionKeyAttachment attachment = new SelectionKeyAttachment();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, attachment);
    }

    /**
     *
     * @param readBuffer
     * @return
     */
    private byte[] readBytes( ByteBuffer readBuffer ) {
        //read the data in the byte array
        byte[] messageBytes = new byte[readBuffer.remaining()];
        readBuffer.get(messageBytes);
        return messageBytes;
    }

    /**
     *
     * @param readBuffer
     * @return
     */
    private byte[] readBytesUntilEOT(ByteBuffer readBuffer ) {

        for (int pos = readBuffer.position(); pos < readBuffer.limit(); pos++) {
            if (readBuffer.get(pos) == EOT) { // EOT
                //end of message marker found, process the message
                byte[] messageBytes = new byte[pos - readBuffer.position()];
                //read the message into the bytes array
                readBuffer.get(messageBytes);
                //read the EOT marker
                readBuffer.get();

                return messageBytes;
            }
        }

        return new byte[0];
    }

    /**
     *
     * @param key
     * @throws IOException
     */
    private void readFromChannel(SelectionKey key) throws IOException {
        //reading from the channel depends on the protocol of the messages between client and the server
        //in this example client sends strings converted to the bytes using UTF-8 encoding and separated by the EOT (0x04) byte

        final SocketChannel socketChannel = (SocketChannel) key.channel();

        //we save the partially read data in the selection key
        final SelectionKeyAttachment attachment = (SelectionKeyAttachment) key.attachment();

        int count;

        //read the data while it is available
        while ((count = socketChannel.read(attachment.readBuffer)) > 0) {
            //prepare buffer for reading
            attachment.readBuffer.flip();

            if (attachment.readBuffer.hasRemaining()) {

                //read the data in the byte array
                byte[] messageBytes = readBytes(attachment.readBuffer);
                //byte[] messageBytes = readBytesUntilEOT(attachment.readBuffer);

                if( messageBytes.length > 0 ) {
                    processMessage(messageBytes, key);
                }
                else {
                    log.warn( "message contains 0 bytes");
                }
            }

            //prepare buffer for writing for the next read from the channel
            attachment.readBuffer.compact();

            if (!attachment.readBuffer.hasRemaining()) {
                //there is no place where to write, increase the buffer
                throw new IOException("Read buffer overflow");
            }
        }

        if (count < 0) {
            //EOF in the socket channel, close it
            socketChannel.close();
            key.cancel();
        }
    }

    /**
     *
     * @param key
     * @throws IOException
     */
    private void writeToChannel(SelectionKey key) throws IOException {

        final SelectionKeyAttachment keyAtt = (SelectionKeyAttachment) key.attachment();

        synchronized (keyAtt) {
            if (keyAtt.writeBuffer != null && keyAtt.writeBuffer.hasRemaining()) {
                //there is data to write to channel, write it
                final SocketChannel socketChannel = (SocketChannel) key.channel();

                while (keyAtt.writeBuffer.hasRemaining() && socketChannel.write(keyAtt.writeBuffer) > 0);
            }

            if (keyAtt.writeBuffer == null || !keyAtt.writeBuffer.hasRemaining()) {
                //no data left for writing, back to read only operations
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    /**
     *
     * @param messageBytes
     * @param key
     * @throws IOException
     */
    public final void postMessage( byte[] messageBytes, SelectionKey key) throws IOException {

        final SelectionKeyAttachment keyAtt = (SelectionKeyAttachment) key.attachment();

        //we synchronizing on keyAtt to access variables from different threads
        synchronized (keyAtt) {

            keyAtt.writeBuffer =
                    ofNullable( keyAtt.writeBuffer ).map( buffer ->
                            allocateDirect(buffer.capacity() + messageBytes.length)
                                    .put(buffer)
                    ).orElseGet( () -> allocateDirect(messageBytes.length) )
                    ;
            // prepare for writing
            keyAtt.writeBuffer.flip();
            keyAtt.writeBuffer.compact()
                    // add data to the buffer
                    .put(messageBytes)
                    // EOT symbol
                    //.put(EOT)
                    // prepare for read
                    .flip();

            //now the buffer contains our message, mark it as interested in writing
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            //wakeup selector to cancel current select operation
            key.selector().wakeup();
        }
    }

    /**
     *
     * @param messageBytes
     * @param key
     * @throws IOException
     */
    protected abstract void processMessage(byte[] messageBytes, SelectionKey key) throws IOException;

    protected void connect( Selector selector, C channel) throws IOException {}

    /**
     *
     * @param address
     * @throws IOException
     */
    public final void bind(InetSocketAddress address) throws IOException {

        final Selector selector = Selector.open();

        try (final ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

            serverSocket.bind(address);
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            enabled = true;

            while (enabled) {
                final int selectors = selector.select();
                log.trace("selectors selected # {}", selectors);

                if (selectors == 0) continue;

                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                final Iterator<SelectionKey> keys = selectedKeys.iterator();
                while (keys.hasNext()) {

                    final SelectionKey key = keys.next();

                    if (key.isAcceptable()) {
                        log.trace("isAcceptable");
                        accept(selector, (C) serverSocket);
                    }
                    if (key.isConnectable()) {
                        log.trace("isConnectable");
                        connect(selector, (C) serverSocket);
                    }
                    try {
                        if (key.isReadable()) {
                            log.trace("isReadable");
                            readFromChannel(key);
                        } else if (key.isWritable()) {
                            log.trace("isWritable");
                            writeToChannel(key);
                        }
                    } catch (IOException ex) {
                        log.error("error reading/writing from/to channel", ex);
                        final SelectableChannel channel = key.channel();
                        channel.close();
                    }

                    keys.remove();
                }
            }

        }
    }
}
