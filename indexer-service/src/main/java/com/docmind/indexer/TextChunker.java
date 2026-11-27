package com.docmind.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits document text into sentence-aware chunks of at most {@code maxChars} characters.
 * Consecutive chunks overlap by one sentence where possible so context is not lost at
 * chunk boundaries. A sentence longer than {@code maxChars} is hard-split, preferring
 * whitespace so words stay whole.
 */
public class TextChunker {

    public static final int DEFAULT_MAX_CHARS = 500;

    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]+[\"')\\]]*\\s+");

    private final int maxChars;

    public TextChunker() {
        this(DEFAULT_MAX_CHARS);
    }

    public TextChunker(int maxChars) {
        if (maxChars < 100) {
            throw new IllegalArgumentException("maxChars must be at least 100, got " + maxChars);
        }
        this.maxChars = maxChars;
    }

    public List<TextChunk> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.strip();
        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = chunkEnd(normalized, start);
            int s = start;
            while (s < end && Character.isWhitespace(normalized.charAt(s))) {
                s++;
            }
            int e = end;
            while (e > s && Character.isWhitespace(normalized.charAt(e - 1))) {
                e--;
            }
            if (s < e) {
                chunks.add(new TextChunk(chunks.size(), normalized.substring(s, e), s, e));
            }
            if (end >= normalized.length()) {
                break;
            }
            int overlap = overlapStart(normalized, start, end);
            start = overlap > start ? overlap : end;
        }
        return chunks;
    }

    private int chunkEnd(String text, int start) {
        if (text.length() - start <= maxChars) {
            return text.length();
        }
        int limit = start + maxChars;
        int boundary = lastSentenceBoundary(text, start, limit);
        if (boundary > start) {
            return boundary;
        }
        for (int i = limit; i > start; i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                return i;
            }
        }
        return limit;
    }

    /** End of the last sentence boundary in the region, or -1 when there is none. */
    private int lastSentenceBoundary(String text, int start, int limit) {
        Matcher matcher = SENTENCE_END.matcher(text).region(start, limit);
        int last = -1;
        while (matcher.find()) {
            last = matcher.end();
        }
        return last;
    }

    /** Start of the chunk's last sentence (boundary ending strictly before chunkEnd), or -1. */
    private int overlapStart(String text, int chunkStart, int chunkEnd) {
        Matcher matcher = SENTENCE_END.matcher(text).region(chunkStart, chunkEnd);
        int last = -1;
        while (matcher.find()) {
            if (matcher.end() < chunkEnd) {
                last = matcher.end();
            }
        }
        return last;
    }
}
