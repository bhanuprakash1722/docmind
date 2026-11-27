package com.docmind.indexer;

/**
 * One chunk of document text, ready for embedding. Offsets refer to the
 * whitespace-stripped input text.
 */
public record TextChunk(int index, String text, int startOffset, int endOffset) {
}
