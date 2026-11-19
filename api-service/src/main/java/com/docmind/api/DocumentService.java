package com.docmind.api;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public DocumentResponse create(DocumentRequest request) {
        Document document = repository.save(new Document(request.title(), request.body()));
        return DocumentResponse.from(document);
    }

    public List<DocumentResponse> list() {
        return repository.findAll().stream().map(DocumentResponse::from).toList();
    }

    public DocumentResponse get(UUID id) {
        return DocumentResponse.from(find(id));
    }

    @Transactional
    public DocumentResponse update(UUID id, DocumentRequest request) {
        Document document = find(id);
        document.update(request.title(), request.body());
        return DocumentResponse.from(repository.save(document));
    }

    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private Document find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }
}
