package com.carter.infra;

import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

@Slf4j
public class MatchingEngineServer {

    private final Selector selector;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    private final DirectBuffer bufferWrapper = new UnsafeBuffer();
    private final int port;

    public MatchingEngineServer(int port) throws IOException {
        this.port = port;
        this.selector = Selector.open();
    }

    public void start() {
        try (final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            log.info("Bound to {}", port);


            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);


            // Start listening for clients
            while (true) {
                selector.select();
                final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    final SelectionKey key = iterator.next();
                    iterator.remove();

                    // Process the client connection
                    if (key.isAcceptable()) {
                        accept(key);
                    }
                    if (key.isReadable()) {
                        read(key);
                    }

                }
            }
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void accept(final SelectionKey key) throws IOException {
        final SocketChannel client;
        try (final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel()) {
            client = serverSocketChannel.accept();
        }
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        log.info("Accepted connection from {}", client.getRemoteAddress());
    }

    private void read(final SelectionKey key) throws IOException {
        final SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();

        final int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            client.close();
            key.cancel();
        }
//        context.setClient(client);
//        context.setSelectionKey(key);
//        bufferWrapper.wrap(buffer);
//        ingressProcessor.dispatch(bufferWrapper, 0, buffer.capacity());
    }

}
