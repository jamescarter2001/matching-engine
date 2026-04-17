package com.carter;

import com.carter.infra.MatchingEngineServer;
import com.carter.processor.OrderMessageProcessor;

import java.io.IOException;

public class MatchingEngineApplication {

    public static void main(String[] args) {
        final OrderMessageProcessor messageProcessor = new OrderMessageProcessor();

        try (MatchingEngineServer server = new MatchingEngineServer(8080)) {
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}