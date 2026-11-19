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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-11-19T00:00:00Z");

    @Mock
    private DocumentService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

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
    void getReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new DocumentNotFoundException(id));

        mockMvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isNotFound());
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
}
