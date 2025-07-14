package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

@Getter
public final class Avatar extends ValueObject {

    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024;
    private static final int MIN_DIMENSION = 50;
    private static final int MAX_DIMENSION = 1024;

    private final String fileName;
    private final String mimeType;
    private final long sizeBytes;
    private final int width;
    private final int height;
    private final String storageUrl;

    private Avatar(String fileName, String mimeType, long sizeBytes, int width, int height, String storageUrl) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.storageUrl = storageUrl;
        validate();
    }

    public static Avatar of(String fileName, String mimeType, long sizeBytes, int width, int height, String storageUrl) {
        return new Avatar(fileName, mimeType, sizeBytes, width, height, storageUrl);
    }

    public static Avatar defaultAvatar() {
        return new Avatar("default.png", "image/png", 1024, 100, 100, "/assets/default-avatar.png");
    }

    @Override
    protected void validate() {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new InvalidAvatarException("File name cannot be null or empty");
        }

        if (!isValidMimeType(mimeType)) {
            throw new InvalidAvatarException("Unsupported image format: " + mimeType);
        }

        if (sizeBytes > MAX_SIZE_BYTES) {
            throw new InvalidAvatarException("Image size exceeds maximum: " + sizeBytes + " bytes");
        }

        if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
            throw new InvalidAvatarException(
                    String.format("Image dimensions too small: %dx%d (minimum: %dx%d)",
                            width, height, MIN_DIMENSION, MIN_DIMENSION));
        }

        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new InvalidAvatarException(
                    String.format("Image dimensions too large: %dx%d (maximum: %dx%d)",
                            width, height, MAX_DIMENSION, MAX_DIMENSION));
        }

        if (storageUrl == null || storageUrl.trim().isEmpty()) {
            throw new InvalidAvatarException("Storage URL cannot be null or empty");
        }
    }

    private boolean isValidMimeType(String mimeType) {
        return mimeType != null && (
                mimeType.equals("image/jpeg") ||
                        mimeType.equals("image/png") ||
                        mimeType.equals("image/webp")
        );
    }

    public boolean isSquare() {
        return width == height;
    }

    public String getFileExtension() {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    public boolean isHighResolution() {
        return width >= 512 && height >= 512;
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{fileName, mimeType, sizeBytes, width, height, storageUrl};
    }

    @Override
    public String toString() {
        return String.format("Avatar{%s, %s, %dx%d, %.1fKB}",
                fileName, mimeType, width, height, sizeBytes / 1024.0);
    }
}