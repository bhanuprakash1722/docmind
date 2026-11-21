package com.docmind.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 50_000) String body) {
}
