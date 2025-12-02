package com._6.ems.utils;

import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.UUID;

/**
 * Utility class that provides helper methods for validating and uploading files to Cloudinary.
 */
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryUtil {

    Cloudinary cloudinary;

    List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "docx", "txt", "png", "csv", "jfif", "jpeg", "jpg"
    );

    List<String> ALLOWED_CONTENT_TYPES = List.of(
            // PDF
            "application/pdf",

            // Microsoft Word (.docx)
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",

            // Plain text
            "text/plain",

            // CSV
            "text/csv",
            "application/csv",

            // Images
            "image/png",
            "image/jpeg",
            "image/pjpeg",
            "image/jpg",
            "image/jfif",

            // Fallback for unknown binary uploads
            "application/octet-stream"
    );

    /**
     * Validates the provided file to ensure it meets allowed type and format constraints.
     *
     * @param file the {@link MultipartFile} to validate
     * @throws IllegalArgumentException if the file is empty, has an invalid name,
     *                                  unsupported extension, or invalid MIME type
     */
    public void validateFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (filename.isBlank()) {
            throw new IllegalArgumentException("File name is missing");
        }

        String extension = getExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only PDF, DOCX, TXT, CSV, and image files are allowed");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file content type: " + contentType);
        }
    }

    public String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Determines the appropriate upload format based on content type and filename.
     * Used to ensure correct format mapping for Cloudinary upload.
     *
     * @param contentType the MIME type of the file
     * @param filename    the original filename
     * @return the normalized format string for Cloudinary upload (e.g., "pdf", "jpg", "png")
     * @throws IllegalArgumentException if the content type is unsupported
     */
    public String getUploadFormat(String contentType, String filename) {
        if ("application/octet-stream".equals(contentType)) {
            String extension = this.getExtension(filename);
            if (extension.equals("jfif")) return "jpg";
        }

        return switch (contentType) {
            case "application/pdf" -> "pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "text/plain" -> "txt";
            case "text/csv" -> "csv";
            case "image/jpeg", "image/pipeg" -> "jpg";
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    public String uploadToCloudinary(MultipartFile file, String folder) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh");
        }

        String folderPath = folder + "/";

        String publicId = UUID.randomUUID().toString();

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("folder",  folderPath);
        options.put("public_id", publicId);
        options.put("overwrite", true);

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            return uploadResult.get("url").toString();
        } catch (IOException e) {
            throw new AppException(ErrorCode.IO_EXCEPTION);
        }
    }
}
