package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import java.util.Objects;

public record Avatar(String fileName, String contentType, long sizeBytes) {

    public Avatar(String fileName, String contentType, long sizeBytes) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Avatar filename cannot be empty");
        }
        if (!isValidImageType(contentType)) {
            throw new IllegalArgumentException("Invalid image type: " + contentType);
        }
        if (sizeBytes <= 0 || sizeBytes > 5_000_000) { // 5MB max
            throw new IllegalArgumentException("Invalid avatar size: " + sizeBytes);
        }

        this.fileName = fileName.trim();
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public String getFileExtension() {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    private static boolean isValidImageType(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif") ||
                        contentType.equals("image/webp")
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Avatar(String name, String type, long bytes))) return false;
        return Objects.equals(this.fileName, name) &&
                Objects.equals(this.contentType, type) &&
                this.sizeBytes == bytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, contentType, sizeBytes);
    }

    @Override
    public String toString() {
        return "Avatar(" + fileName + ", " + contentType + ", " + sizeBytes + " bytes)";
    }
}