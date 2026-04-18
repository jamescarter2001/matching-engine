package com.carter.infra;

import com.carter.processor.OrderProcessor;
import com.carter.session.SessionContext;
import com.carter.util.ResourcePool;
import lombok.extern.slf4j.Slf4j;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.sbe.benchmarks.fix.MessageHeaderDecoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

@Slf4j
public class MatchingEngineServer implements AutoCloseable {

    private static final int NEW_ORDER_TEMPLATE_ID = 68;

    private final Selector selector;
    private final ResourcePool<SessionContext> sessionPool;
    private final DirectBuffer directBuffer = new UnsafeBuffer();
    private final int port;

    private final OrderProcessor orderProcessor = new OrderProcessor();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

    public MatchingEngineServer(int port) throws IOException {
        this.port = port;
        this.selector = Selector.open();
        this.sessionPool = new ResourcePool<>(() -> new SessionContext(ByteBuffer.allocateDirect(1024)));
        orderProcessor.start();
    }

    public void start() {
        try (final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            log.info("Bound to {}", port);

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // Start listening for clients
            while (true) {
                selector.select(this::processConnection);
            }
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void processConnection(SelectionKey key) {
        try {
            // Process the client connection
            if (key.isAcceptable()) {
                accept(key);
            } else if (key.isReadable()) {
                read(key);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void accept(final SelectionKey key) throws IOException {
        final SocketChannel client;
        try (final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel()) {
            client = serverSocketChannel.accept();
        }
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, sessionPool.acquire());
        log.info("Accepted connection from {}", client.getRemoteAddress());
    }

    private void read(final SelectionKey key) throws IOException {
        final SocketChannel client = (SocketChannel) key.channel();
        final SessionContext ctx = (SessionContext) key.attachment();
        final ByteBuffer buffer = ctx.buffer();

        final int bytesRead = client.read(buffer);

        if (bytesRead == 0) {
            // nothing to do
            return;
        }

        if (bytesRead == -1) {
            client.close();
            buffer.clear();
            key.cancel();
            sessionPool.release(ctx);
        }

        buffer.flip();
        directBuffer.wrap(buffer);

        while (buffer.remaining() >= MessageHeaderDecoder.ENCODED_LENGTH) {
            headerDecoder.wrap(directBuffer, 0);

            int blockLength = headerDecoder.blockLength();
            int msgLength = headerDecoder.encodedLength() + blockLength;

            if (buffer.remaining() < msgLength) {
                return;
            }

            int templateId = headerDecoder.templateId();
            int version = headerDecoder.version();

            switch (templateId) {
                case NEW_ORDER_TEMPLATE_ID -> orderProcessor.processNewOrder(directBuffer, MessageHeaderDecoder.ENCODED_LENGTH, blockLength, version);
                default -> log.warn("Unknown message format: {}", templateId);
            }

            buffer.position(buffer.position() + msgLength);
        }
    }

    @Override
    public void close() throws IOException {
        selector.close();
    }
}
