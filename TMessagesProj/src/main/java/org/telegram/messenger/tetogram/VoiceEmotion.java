/* Tetogram, 2026. GNU GPL v2 or later. Offline voice message helpers. */
package org.telegram.messenger.tetogram;

/** Removes only explicitly supported inline emotion-control emoji; other emoji remain speech text. */
public final class VoiceEmotion {
    public static final class Parsed {
        public final String text;
        public final String emotion;
        Parsed(String text, String emotion) { this.text = text; this.emotion = emotion; }
    }
    private static final String[][] TOKENS = {
        {"😊", "happy"}, {"😄", "happy"}, {"😡", "angry"}, {"😢", "sad"},
        {"🤩", "excited"}, {"🤫", "whisper"}, {"📢", "shout"}, {"😂", "laugh"},
        {"😮‍💨", "sigh"}, {"😏", "sarcastic"}, {"😱", "scared"}
    };
    private VoiceEmotion() {}
    public static Parsed parse(String input) {
        if (input == null) return new Parsed("", "neutral");
        String mood = "neutral";
        int earliest = Integer.MAX_VALUE;
        String result = input;
        for (String[] entry : TOKENS) {
            int where = input.indexOf(entry[0]);
            if (where >= 0 && where < earliest) { earliest = where; mood = entry[1]; }
            result = result.replace(entry[0], " ");
        }
        result = result.replaceAll("[\\t ]{2,}", " ").trim();
        return new Parsed(result, mood);
    }
    public static float pitch(String emotion) {
        switch (emotion) {
            case "happy": return 1.06f;
            case "excited": return 1.12f;
            case "angry": return 0.96f;
            case "sad": return 0.92f;
            case "whisper": return 0.97f;
            case "shout": return 1.07f;
            case "laugh": return 1.08f;
            case "scared": return 1.11f;
            default: return 1f;
        }
    }
    public static float speed(String emotion) {
        switch (emotion) {
            case "excited": return 1.12f;
            case "angry": return 1.08f;
            case "sad": return .84f;
            case "whisper": return .82f;
            case "shout": return 1.04f;
            case "scared": return 1.08f;
            default: return 1f;
        }
    }
}
