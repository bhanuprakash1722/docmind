package com.docmind.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;

    @InjectMocks
    private DocumentService service;

    @Test
    void createSavesAndReturnsDocument() {
        when(repository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = service.create(new DocumentRequest("Kafka notes", "producers write records"));

        assertNotNull(response.id());
        assertEquals("Kafka notes", response.title());
        assertEquals("producers write records", response.body());
        assertNotNull(response.createdAt());
        verify(repository).save(any(Document.class));
    }

    @Test
    void listReturnsAllDocuments() {
        when(repository.findAll()).thenReturn(List.of(new Document("a", "one"), new Document("b", "two")));

        List<DocumentResponse> documents = service.list();

        assertEquals(2, documents.size());
        assertEquals(List.of("a", "b"), documents.stream().map(DocumentResponse::title).toList());
    }

    @Test
    void getReturnsDocumentWhenPresent() {
        Document stored = new Document("a", "one");
        when(repository.findById(stored.getId())).thenReturn(Optional.of(stored));

        DocumentResponse response = service.get(stored.getId());

        assertEquals(stored.getId(), response.id());
        assertEquals("one", response.body());
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> service.get(id));
    }

    @Test
    void updateChangesTitleAndBody() {
        Document stored = new Document("old", "old body");
        when(repository.findById(stored.getId())).thenReturn(Optional.of(stored));
        when(repository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = service.update(stored.getId(), new DocumentRequest("new", "new body"));

        assertEquals("new", response.title());
        assertEquals("new body", response.body());
    }

    @Test
    void updateThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> service.update(id, new DocumentRequest("t", "b")));
    }

    @Test
    void deleteRemovesExistingDocument() {
        Document stored = new Document("a", "one");
        when(repository.findById(stored.getId())).thenReturn(Optional.of(stored));

        service.delete(stored.getId());

        verify(repository).delete(stored);
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> service.delete(id));
        verify(repository, never()).delete(any(Document.class));
    }
}
