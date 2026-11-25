package mx.hecagus.luma;
public final class GalleryLogicTest {
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static boolean media(String query, int width, int height) {
        return GalleryLogic.matchesMedia(GalleryLogic.normalize(query).split("\\s+"),
            "img_005.jpg camara julio", "IMG_005.JPG", "image/jpeg", width, height);
    }
    public static void main(String[] args) {
        check(GalleryLogic.matches("camara JULIO", "IMG_005.jpg", "Cámara", "17 de julio de 2026"), "Search ignores accents and case");
        check(!GalleryLogic.matches("julio captura", "IMG.jpg", "Cámara", "17 de julio de 2026"), "Every search word is required");
        check(GalleryLogic.matches("", "x", "y", "z"), "Empty search includes all");
        check(GalleryLogic.timestamp(1700000000000L, 100) == 1700000000000L, "Capture date wins");
        check(GalleryLogic.timestamp(0, 1700000000L) == 1700000000000L, "Added date converts seconds once");
        check(GalleryLogic.timestamp(-1, -1) == 0, "Invalid dates remain unknown");
        check(GalleryLogic.sampleSize(12000, 8000, 3000) == 4, "Large image decode is bounded");
        check(GalleryLogic.sampleSize(800, 12000, 3000) == 4, "Portrait decode is bounded");
        check(GalleryLogic.sampleSize(100, 50, 3000) == 1, "Small images are not oversampled");
        check(GalleryLogic.fileSize(1572864).equals("1.5 MB"), "File size formatting");
        check(GalleryLogic.normalize(null).isEmpty(), "Missing metadata does not crash search");
        check(media("camara formato:jpg mp:4 orientacion:horizontal", 4000, 3000), "Combined metadata filters");
        check(media("formato:jpeg", 4000, 3000), "JPEG alias");
        check(!media("formato:png", 4000, 3000), "Format mismatch");
        check(!media("mp:13", 4000, 3000), "Minimum megapixels");
        check(media("mp:12,0", 4000, 3000), "Decimal comma");
        check(!media("mp:abc", 4000, 3000), "Invalid resolution");
        check(!media("mp:0", 4000, 3000), "Zero resolution rejected");
        check(!media("mp:4", 0, 0), "Unknown dimensions");
        check(media("orientacion:vertical", 3000, 4000), "Vertical");
        check(media("orientacion:cuadrada", 3000, 3000), "Square");
        check(!media("orientacion:horizontal", 3000, 3000), "Square not horizontal");
        check(!media("orientacion:", 3000, 3000), "Invalid orientation");
        check(!media("formato:", 3000, 3000), "Empty format");
        check(media("camara julio", 0, 0), "Plain text remains compatible");
        check(GalleryLogic.matchesMedia(new String[]{"formato:mp4"}, "video", "VID.mp4", "video/*", 0, 0), "Format from extension");
        System.out.println(checks + " logic checks passed.");
    }
}
