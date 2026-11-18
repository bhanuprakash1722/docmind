package com.docmind.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    private UUID id;
    private String title;
    private String body;
    private Instant createdAt;

    protected Document() {
    }

    public Document(String title, String body) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
