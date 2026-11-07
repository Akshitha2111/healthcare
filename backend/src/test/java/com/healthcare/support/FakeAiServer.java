package com.healthcare.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/** A stand-in for the Flask service, built on the JDK's HTTP server (no extra test dependencies). */
public final class FakeAiServer {
    public static final FakeAiServer INSTANCE = start();

    public enum Mode { OK, DROP_CONNECTION, SERVER_ERROR }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpServer server;
    public volatile Mode mode = Mode.OK;
    public volatile long delayMillis = 0;

    private FakeAiServer(HttpServer server) {
        this.server = server;
    }

    private static FakeAiServer start() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.setExecutor(Executors.newCachedThreadPool());
            FakeAiServer fake = new FakeAiServer(s);
            s.createContext("/predict", fake::predict);
            s.createContext("/symptoms", fake::symptoms);
            s.createContext("/health", ex -> respond(ex, 200, "{\"status\":\"ok\"}"));
            s.start();
            return fake;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        mode = Mode.OK;
        delayMillis = 0;
    }

    private void predict(HttpExchange ex) throws IOException {
        byte[] body = ex.getRequestBody().readAllBytes();
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        if (mode == Mode.DROP_CONNECTION) {
            ex.close(); // no response at all -> client sees an I/O error
            return;
        }
        if (mode == Mode.SERVER_ERROR) {
            respond(ex, 500, "{\"success\":false,\"error\":{\"code\":\"INTERNAL_ERROR\",\"message\":\"boom\"}}");
            return;
        }
        JsonNode symptoms = MAPPER.readTree(new String(body, StandardCharsets.UTF_8)).path("symptoms");
        StringBuilder used = new StringBuilder();
        for (JsonNode s : symptoms) {
            String name = s.asText().trim().toLowerCase().replace(' ', '_');
            if (name.equals("bogus")) {
                respond(ex, 400,
                        "{\"success\":false,\"error\":{\"code\":\"INVALID_SYMPTOMS\",\"message\":\"unsupported symptom(s): bogus\"}}");
                return;
            }
            if (used.length() > 0) {
                used.append(',');
            }
            used.append('"').append(name).append('"');
        }
        String first = symptoms.get(0).asText().trim().toLowerCase().replace(' ', '_');
        respond(ex, 200, "{\"success\":true,\"model_version\":\"1.0.0-test\",\"model_name\":\"fake_model\","
                + "\"score_type\":\"uncalibrated_model_score\",\"top_prediction\":\"Fungal infection\","
                + "\"predictions\":[{\"rank\":1,\"disease\":\"Fungal infection\",\"score\":0.61},"
                + "{\"rank\":2,\"disease\":\"Allergy\",\"score\":0.21},{\"rank\":3,\"disease\":\"Drug Reaction\",\"score\":0.08}],"
                + "\"symptoms_used\":[" + used + "],"
                + "\"explanation\":{\"method\":\"occlusion\",\"influential_symptoms\":[{\"symptom\":\"" + first
                + "\",\"label\":\"X\",\"influence\":0.3}],\"caveat\":\"c\"},"
                + "\"description\":\"A fungal skin condition (test data).\","
                + "\"general_guidance\":[\"See a clinician.\"],\"urgent_care_notice\":\"Seek urgent care if severe.\","
                + "\"limitations\":[\"Educational only.\"],\"disclaimer\":\"Not a medical diagnosis.\"}");
    }

    private void symptoms(HttpExchange ex) throws IOException {
        respond(ex, 200, "{\"success\":true,\"count\":3,\"min_symptoms\":2,\"max_symptoms\":20,\"symptoms\":["
                + "{\"name\":\"itching\",\"label\":\"Itching\"},{\"name\":\"skin_rash\",\"label\":\"Skin rash\"},"
                + "{\"name\":\"high_fever\",\"label\":\"High fever\"}]}");
    }

    private static void respond(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
