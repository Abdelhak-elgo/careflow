package com.elgourmat.careflow;

import com.elgourmat.careflow.adapter.out.persistence.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CareflowE2ETest extends AbstractPostgresIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Test
    void small_dental_claim_is_approved_and_retrievable_by_id() throws Exception {
        String body = """
                {
                  "patientId": "patient-42",
                  "careType": "DENTAL",
                  "amount": 50.00,
                  "currency": "EUR",
                  "careDate": "2026-08-15"
                }
                """;

        ResponseEntity<String> created = rest.exchange(
                "/api/claims", HttpMethod.POST, jsonRequest(body), String.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdJson = JSON.readTree(created.getBody());
        assertThat(createdJson.get("status").asText()).isEqualTo("APPROVED");
        assertThat(createdJson.get("decisionReason").asText())
                .isEqualTo("Auto-approval: amount below 100 EUR");
        String id = createdJson.get("id").asText();

        ResponseEntity<String> loaded = rest.exchange(
                "/api/claims/" + id, HttpMethod.GET, empty(), String.class);

        assertThat(loaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode loadedJson = JSON.readTree(loaded.getBody());
        assertThat(loadedJson.get("id").asText()).isEqualTo(id);
        assertThat(loadedJson.get("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    void expensive_optical_claim_is_rejected() throws Exception {
        String body = """
                {
                  "patientId": "patient-42",
                  "careType": "OPTICAL",
                  "amount": 800.00,
                  "currency": "EUR",
                  "careDate": "2026-08-15"
                }
                """;

        ResponseEntity<String> created = rest.exchange(
                "/api/claims", HttpMethod.POST, jsonRequest(body), String.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdJson = JSON.readTree(created.getBody());
        assertThat(createdJson.get("status").asText()).isEqualTo("REJECTED");
        assertThat(createdJson.get("decisionReason").asText())
                .contains("Optical claim above 500 EUR");
    }

    @Test
    void medium_dental_claim_is_pending_and_appears_in_filter() throws Exception {
        String body = """
                {
                  "patientId": "patient-42",
                  "careType": "DENTAL",
                  "amount": 300.00,
                  "currency": "EUR",
                  "careDate": "2026-08-15"
                }
                """;

        ResponseEntity<String> created = rest.exchange(
                "/api/claims", HttpMethod.POST, jsonRequest(body), String.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdJson = JSON.readTree(created.getBody());
        assertThat(createdJson.get("status").asText()).isEqualTo("PENDING");
        String id = createdJson.get("id").asText();

        ResponseEntity<String> pending = rest.exchange(
                "/api/claims?status=PENDING", HttpMethod.GET, empty(), String.class);

        assertThat(pending.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode pendingJson = JSON.readTree(pending.getBody());
        assertThat(pendingJson.isArray()).isTrue();
        boolean containsCreated = false;
        for (JsonNode node : pendingJson) {
            if (node.get("id").asText().equals(id)) {
                containsCreated = true;
                assertThat(node.get("status").asText()).isEqualTo("PENDING");
            }
        }
        assertThat(containsCreated).isTrue();
    }

    @Test
    void invalid_negative_amount_returns_problem_detail_400() throws Exception {
        String body = """
                {
                  "patientId": "patient-42",
                  "careType": "DENTAL",
                  "amount": -10.00,
                  "currency": "EUR",
                  "careDate": "2026-08-15"
                }
                """;

        ResponseEntity<String> response = rest.exchange(
                "/api/claims", HttpMethod.POST, jsonRequest(body), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        JsonNode problem = JSON.readTree(response.getBody());
        assertThat(problem.get("title").asText()).isEqualTo("Validation failed");
        assertThat(problem.get("violations").get(0).get("field").asText()).isEqualTo("amount");
    }

    private HttpEntity<String> jsonRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-User-Id", "patient-42");
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> empty() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "patient-42");
        return new HttpEntity<>(headers);
    }
}
