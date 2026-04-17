package com.carter;

import com.carter.infra.MatchingEngineServer;

import java.io.IOException;

public class MatchingEngineApplication {

    public static void main(String[] args) {
        try (MatchingEngineServer server = new MatchingEngineServer(8080)) {
            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}