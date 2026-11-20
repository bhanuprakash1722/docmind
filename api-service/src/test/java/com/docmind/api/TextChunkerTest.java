package com.docmind.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    private static final String S1 = "Alpha beta gamma delta epsilon zeta eta theta iota kappa.";
    private static final String S2 = "Lambda mu nu xi omicron pi rho sigma tau upsilon phi chi.";
    private static final String S3 = "Psi omega bravo charlie delta echo foxtrot golf hotel india.";
    private static final String S4 = "Juliett kilo lima mike november oscar papa quebec romeo sierra.";
    private static final String FOUR_SENTENCES = S1 + " " + S2 + " " + S3 + " " + S4;

    private final TextChunker chunker = new TextChunker(120);

    @Test
    void nullAndBlankReturnEmpty() {
        assertTrue(chunker.chunk(null).isEmpty());
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   \n\t ").isEmpty());
    }

    @Test
    void shortTextIsOneChunk() {
        List<TextChunk> chunks = chunker.chunk("Just one sentence.");

        assertEquals(1, chunks.size());
        assertEquals(new TextChunk(0, "Just one sentence.", 0, 18), chunks.get(0));
    }

    @Test
    void splitsOnSentenceBoundaries() {
        List<TextChunk> chunks = chunker.chunk(FOUR_SENTENCES);

        assertEquals(4, chunks.size());
        assertTrue(chunks.stream().allMatch(c -> c.text().length() <= 120));
        assertTrue(chunks.get(0).text().startsWith("Alpha"));
        assertTrue(chunks.get(0).text().endsWith("chi."));
        assertTrue(chunks.get(3).text().endsWith("sierra."));
    }

    @Test
    void consecutiveChunksOverlapByOneSentence() {
        List<TextChunk> chunks = chunker.chunk(FOUR_SENTENCES);

        assertTrue(chunks.get(1).text().startsWith("Lambda"), "chunk 2 should restart at the previous chunk's last sentence");
        assertTrue(chunks.get(2).text().startsWith("Psi"), "chunk 3 should restart at the previous chunk's last sentence");
        assertTrue(chunks.get(0).text().contains(S2), "overlapping sentence appears in both chunks");
        assertTrue(chunks.get(1).text().contains(S2));
    }

    @Test
    void offsetsPointAtChunkText() {
        List<TextChunk> chunks = chunker.chunk(FOUR_SENTENCES);

        for (TextChunk chunk : chunks) {
            assertEquals(chunk.text(), FOUR_SENTENCES.substring(chunk.startOffset(), chunk.endOffset()));
            assertEquals(chunks.indexOf(chunk), chunk.index());
        }
    }

    @Test
    void longSentenceHardSplitsAtWhitespace() {
        String text = String.join(" ", Collections.nCopies(60, "word")); // 299 chars, no punctuation

        List<TextChunk> chunks = chunker.chunk(text);

        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.stream().allMatch(c -> c.text().length() <= 120));
        for (int i = 1; i < chunks.size(); i++) {
            assertEquals(chunks.get(i - 1).endOffset() + 1, chunks.get(i).startOffset(),
                    "no overlap without sentence boundaries; exactly one space is consumed between chunks");
        }
        String rebuilt = String.join(" ", chunks.stream().map(TextChunk::text).toList());
        assertEquals(text, rebuilt, "no word is lost or split");
    }

    @Test
    void leadingAndTrailingWhitespaceIsStripped() {
        List<TextChunk> chunks = chunker.chunk("   \n " + "Just one sentence." + "  \t ");

        assertEquals(1, chunks.size());
        assertEquals("Just one sentence.", chunks.get(0).text());
    }

    @Test
    void manyChunkTextStaysWithinDefaultLimit() {
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= 200; i++) {
            text.append("Sentence ").append(i).append(" repeats the same fairly ordinary wording again.");
        }
        // no space after the periods, so no sentence boundaries: exercises the hard-split path at scale
        List<TextChunk> chunks = new TextChunker().chunk(text.toString());

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(c -> c.text().length() <= TextChunker.DEFAULT_MAX_CHARS));
    }

    @Test
    void rejectsTooSmallMaxChars() {
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(99));
    }
}
