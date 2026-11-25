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
    /**
     * Combined local metadata filters. Unknown prefixes retain ordinary text search.
     * No file reads, network requests or additional permissions.
     */
    public static boolean matchesMedia(String[] words, String searchText, String name,
                                       String mime, int width, int height) {
        for (String word : words) {
            if (word.startsWith("formato:")) {
                String format = word.substring(8);
                if (format.equals("jpg")) format = "jpeg";
                String kind = normalize(mime);
                String extension = normalize(name);
                int dot = extension.lastIndexOf('.');
                extension = dot < 0 ? "" : extension.substring(dot + 1);
                if (extension.equals("jpg")) extension = "jpeg";
                if (format.isEmpty() || !(kind.endsWith("/" + format) || extension.equals(format))) return false;
            } else if (word.startsWith("orientacion:")) {
                String orientation = word.substring(12);
                if (width <= 0 || height <= 0) return false;
                if ("horizontal".equals(orientation)) { if (width <= height) return false; }
                else if ("vertical".equals(orientation)) { if (height <= width) return false; }
                else if ("cuadrada".equals(orientation)) { if (width != height) return false; }
                else return false;
            } else if (word.startsWith("mp:")) {
                String value = word.substring(3).replace(',', '.');
                if (width <= 0 || height <= 0 || !value.matches("[0-9]+(?:\\.[0-9]+)?")) return false;
                double minimum;
                try { minimum = Double.parseDouble(value); } catch (NumberFormatException invalid) { return false; }
                if (Double.isInfinite(minimum) || minimum <= 0 || (long) width * height / 1000000.0 < minimum) return false;
            } else if (!searchText.contains(word)) return false;
        }
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
