package mx.hecagus.luma;

import java.text.Normalizer;
import java.util.Locale;

/** Pure logic shared by the gallery and the JVM verification suite. */
public final class GalleryLogic {
    private GalleryLogic() {}
    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
    public static boolean matches(String query, String name, String album, String date) {
        String haystack = normalize(name + " " + album + " " + date);
        for (String word : normalize(query).split("\\s+")) if (!haystack.contains(word)) return false;
        return true;
    }
    public static long timestamp(long takenMillis, long addedSeconds) {
        return takenMillis > 0 ? takenMillis : Math.max(0, addedSeconds) * 1000L;
    }
    public static int sampleSize(int width, int height, int maximum) {
        int factor = 1;
        while (Math.max(width / factor, height / factor) > maximum) factor *= 2;
        return factor;
    }
    public static String fileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
    }
}
