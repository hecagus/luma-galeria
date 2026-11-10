package mx.hecagus.luma;
public final class GalleryLogicTest {
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
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
        System.out.println(checks + " logic checks passed.");
    }
}
