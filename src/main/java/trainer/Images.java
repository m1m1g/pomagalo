package trainer;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public final class Images {

    private static final Logger LOG = Logger.getLogger(Images.class.getName());

    private static final String ZIP    = "images.zip";
    private static final String FOLDER = "data";

    private static ZipFile archive;
    private static boolean tried;

    private static final Map<String, Image> cache = new HashMap<>();

    private Images() { }

    public static synchronized Image get(String path) {
        if (path == null || path.isBlank()) return null;
        if (cache.containsKey(path)) return cache.get(path);

        Image image = fromArchive(path);
        if (image == null) image = fromFolder(path);

        cache.put(path, image);
        if (image == null) LOG.info("[Images] Липсва изображение: " + path);
        return image;
    }

    public static synchronized boolean has(String path) {
        return get(path) != null;
    }

    public static synchronized void close() {
        try { if (archive != null) archive.close(); }
        catch (Exception ignored) { }
        archive = null;
        cache.clear();
    }

    private static Image fromArchive(String path) {
        ZipFile zip = archive();
        if (zip == null) return null;
        try {
            var entry = zip.getEntry(path);
            if (entry == null) return null;
            try (InputStream in = zip.getInputStream(entry)) {
                return new Image(in);
            }
        } catch (Exception e) {
            LOG.warning("[Images] Грешка при четене от архива: " + e.getMessage());
            return null;
        }
    }

    private static Image fromFolder(String path) {
        try {
            Path p = Pathove.osnova().resolve(FOLDER).resolve(path);
            if (!Files.isReadable(p)) return null;
            try (InputStream in = Files.newInputStream(p)) {
                return new Image(in);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static ZipFile archive() {
        if (archive != null || tried) return archive;
        tried = true;
        try {
            Path p = Pathove.danni(ZIP);
            if (!Files.isReadable(p)) {
                LOG.info("[Images] Няма архив " + p + "; ползва се папката "
                       + Pathove.osnova().resolve(FOLDER).resolve("images") + ".");
                return null;
            }
            archive = new ZipFile(p.toFile());
            LOG.info("[Images] Архивът е отворен: " + archive.size() + " записа.");
        } catch (Exception e) {
            LOG.warning("[Images] Архивът не се отвори: " + e.getMessage());
            archive = null;
        }
        return archive;
    }
}
