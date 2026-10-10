package com.waste.management.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;

@Component
public class ImageValidator {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/jpg");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB limit
    public static final int MAX_IMAGES_PER_COMPLAINT = 5;

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded image file cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size '" + file.getOriginalFilename() + "' exceeds maximum allowed limit of 10MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file MIME type '" + contentType + "'. Allowed image formats: JPG, PNG, WEBP.");
        }

        // Deep Security Check: Inspect binary magic bytes to prevent file extension spoofing
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                throw new IllegalArgumentException("File header for '" + file.getOriginalFilename() + "' is corrupted or incomplete.");
            }

            if (!isJpeg(header) && !isPng(header) && !isWebp(header)) {
                throw new IllegalArgumentException("File content header for '" + file.getOriginalFilename() + "' does not match valid JPEG, PNG, or WEBP magic bytes. File signature verification failed.");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read file content header for security validation: " + ex.getMessage());
        }
    }

    private boolean isJpeg(byte[] header) {
        // JPEG signature: 0xFF, 0xD8, 0xFF
        return header.length >= 3 &&
                (header[0] & 0xFF) == 0xFF &&
                (header[1] & 0xFF) == 0xD8 &&
                (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        // PNG signature: 0x89, 0x50, 0x4E, 0x47
        return header.length >= 4 &&
                (header[0] & 0xFF) == 0x89 &&
                (header[1] & 0xFF) == 0x50 &&
                (header[2] & 0xFF) == 0x4E &&
                (header[3] & 0xFF) == 0x47;
    }

    private boolean isWebp(byte[] header) {
        // WEBP signature: 'R', 'I', 'F', 'F' ... 'W', 'E', 'B', 'P'
        return header.length >= 12 &&
                (header[0] & 0xFF) == 'R' &&
                (header[1] & 0xFF) == 'I' &&
                (header[2] & 0xFF) == 'F' &&
                (header[3] & 0xFF) == 'F' &&
                (header[8] & 0xFF) == 'W' &&
                (header[9] & 0xFF) == 'E' &&
                (header[10] & 0xFF) == 'B' &&
                (header[11] & 0xFF) == 'P';
    }
}
