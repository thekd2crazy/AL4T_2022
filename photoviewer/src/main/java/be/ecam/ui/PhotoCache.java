package be.ecam.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * LRU cache of thumbnails, keyed by file path.
 * <p>
 * Failed loads (unreadable image or I/O error) are cached too, as an empty
 * {@link Optional}, so a broken file is not read again on every repaint.
 * <p>
 * Not thread-safe: an access-ordered {@link LinkedHashMap} is modified even by
 * {@code get()}. It must only be used from the Event Dispatch Thread.
 */
public class PhotoCache {

    private final int capacity;
    private final int thumbnailSize;
    private final Map<Path, Optional<ImageIcon>> cache;

    /**
     * @param capacity      maximum number of thumbnails kept in memory
     * @param thumbnailSize maximum width and height of a thumbnail, in pixels
     */
    public PhotoCache(int capacity, int thumbnailSize) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Cache capacity must be positive");
        }
        if (thumbnailSize <= 0) {
            throw new IllegalArgumentException("Thumbnail size must be positive");
        }

        this.capacity = capacity;
        this.thumbnailSize = thumbnailSize;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Path, Optional<ImageIcon>> eldest) {
                return size() > PhotoCache.this.capacity;
            }
        };
    }

    /**
     * Returns the thumbnail for the given path, loading it from disk on a cache miss.
     *
     * @return the thumbnail, or an empty Optional if the file cannot be read as an image
     */
    public Optional<ImageIcon> get(Path path) {

        // containsKey() does not update the access order, so use get() for the hit.
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        Optional<ImageIcon> thumbnail = load(path);
        cache.put(path, thumbnail);
        return thumbnail;
    }

    public int size() {
        return cache.size();
    }

    private Optional<ImageIcon> load(Path path) {
        try {
            BufferedImage original = ImageIO.read(path.toFile());
            if (original == null) {
                return Optional.empty();
            }
            // Keep the aspect ratio of the original image.
            return Optional.of(new ImageIcon(UiUtils.scale(original, thumbnailSize, thumbnailSize)));
        } catch (IOException e) {
            System.err.println("Cannot read " + path + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
