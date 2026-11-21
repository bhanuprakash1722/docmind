package com.docmind.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-11-19T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService service;

    @Test
    void createReturns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.create(any(DocumentRequest.class)))
                .thenReturn(new DocumentResponse(id, "Kafka notes", "body", FIXED_TIME));

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Kafka notes\",\"body\":\"body\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/documents/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Kafka notes"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-19T00:00:00Z"));
    }

    @Test
    void listReturnsDocuments() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.list()).thenReturn(List.of(new DocumentResponse(id, "a", "one", FIXED_TIME)));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].title").value("a"));
    }

    @Test
    void getReturnsDocument() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenReturn(new DocumentResponse(id, "a", "one", FIXED_TIME));

        mockMvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void updateReturnsUpdatedDocument() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.update(eq(id), any(DocumentRequest.class)))
                .thenReturn(new DocumentResponse(id, "new", "new body", FIXED_TIME));

        mockMvc.perform(put("/api/documents/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"new\",\"body\":\"new body\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("new"))
                .andExpect(jsonPath("$.body").value("new body"));
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/documents/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    @Test
    void missingDocumentReturns404WithErrorBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new DocumentNotFoundException(id));

        mockMvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("document_not_found"))
                .andExpect(jsonPath("$.message").value("Document not found: " + id));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"body\":\"body\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void createRejectsMissingBody() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"title\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.body").exists());
    }

    @Test
    void createRejectsOversizedTitle() throws Exception {
        String oversized = "x".repeat(201);

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + oversized + "\",\"body\":\"body\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void updateRejectsBlankBody() throws Exception {
        mockMvc.perform(put("/api/documents/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"title\",\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.body").exists());
    }
}
