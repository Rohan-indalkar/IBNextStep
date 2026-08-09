package com.infobeans.ibnextstep.codingassessment.piston;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobeans.ibnextstep.codingassessment.ProgrammingLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around Piston's synchronous /execute endpoint. Nothing above
 * this layer (services, controllers) talks to Piston directly.
 *
 * Works against either:
 *  - a self-hosted Piston instance (piston.base-url=http://<host>:2000/api/v2, no key needed), or
 *  - the public emkc.org instance (piston.base-url=https://emkc.org/api/v2/piston,
 *    piston.api-key=<key> — emkc.org has required an authorization key since Feb 2026,
 *    see the setup notes at the end of this change).
 *
 * version is always sent as "*" so Piston picks whatever version of that
 * language is currently installed on the target instance — we don't hardcode
 * version numbers, since those depend entirely on what's installed there.
 */
@Component
@Slf4j
public class PistonClient {

    @Value("${piston.base-url}")
    private String baseUrl;

    @Value("${piston.api-key:}")
    private String apiKey;

    @Value("${piston.run-timeout-seconds:5}")
    private int defaultRunTimeoutSeconds;

    @Value("${piston.compile-timeout-seconds:10}")
    private int compileTimeoutSeconds;

    // Deliberately NOT a shared/reused HttpClient. Piston sends "Connection: close" after
    // every response; java.net.http.HttpClient pools connections by default, and reusing one
    // Piston already closed corrupted subsequent requests — this was the actual cause of the
    // sporadic 400s during /submit (one call per test case, back-to-back). A fresh client per
    // call guarantees a fresh connection every time, matching what the server actually does.
    // (Note: HttpRequest deliberately does NOT set a "Connection" header directly — Java's
    // HttpClient treats that as a restricted header and throws IllegalArgumentException if you try.)
   

    private HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PistonResult execute(ProgrammingLanguage language, String sourceCode, String stdin,
                                 int cpuTimeLimitSeconds, int memoryLimitMb) {
        try {
            Map<String, Object> file = new HashMap<>();
            file.put("name", language.fileName());
            file.put("content", sourceCode);

            int runTimeoutSeconds = cpuTimeLimitSeconds > 0 ? cpuTimeLimitSeconds : defaultRunTimeoutSeconds;

            Map<String, Object> body = new HashMap<>();
            body.put("language", language.pistonLanguage());
            body.put("version", "*");
            body.put("files", List.of(file));
            body.put("stdin", stdin == null ? "" : stdin);
            body.put("run_timeout", runTimeoutSeconds * 1000);
            body.put("compile_timeout", compileTimeoutSeconds * 1000);
            // Piston wants bytes; -1 = no limit. Only enforced if the target instance is
            // configured to allow per-request overrides — many self-hosted setups ignore this.
            body.put("run_memory_limit", memoryLimitMb > 0 ? (long) memoryLimitMb * 1024 * 1024 : -1);
            body.put("compile_memory_limit", -1);

            String requestJson = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/execute"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(Math.max(30, runTimeoutSeconds + compileTimeoutSeconds + 10)))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson));

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", apiKey);
            }

            // Temporary diagnostic logging — shows the exact outgoing request so we can
            // compare it byte-for-byte against a manually-run curl that works. Safe to
            // remove once the 400 is root-caused; harmless if left in (INFO level, no secrets here).
            log.info("Piston request -> {}", requestJson);

            HttpResponse<String> response = newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Piston returned {} — full body: [{}] — headers: {}",
                        response.statusCode(), response.body(), response.headers().map());
                throw new RuntimeException("Piston request failed with status " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode compile = root.path("compile");
            JsonNode run = root.path("run");

            boolean hasCompileStage = !compile.isMissingNode() && !compile.isNull();

            return PistonResult.builder()
                    .compileStagePresent(hasCompileStage)
                    .compileCode(hasCompileStage && !compile.path("code").isNull() ? compile.path("code").asInt() : null)
                    .compileOutput(hasCompileStage ? compile.path("output").asText(null) : null)
                    .runCode(run.path("code").isNull() ? null : run.path("code").asInt())
                    .runSignal(run.path("signal").isNull() ? null : run.path("signal").asText())
                    .stdout(run.path("stdout").isMissingNode() || run.path("stdout").isNull() ? null : run.path("stdout").asText())
                    .stderr(run.path("stderr").isMissingNode() || run.path("stderr").isNull() ? null : run.path("stderr").asText())
                    .build();

        } catch (java.io.IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to call Piston", e);
            throw new RuntimeException("Piston execution failed: " + e.getMessage(), e);
        }
    }
}
