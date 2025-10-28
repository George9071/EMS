package com._6.ems.utils;

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
import java.util.List;
import java.util.Map;
import java.io.IOException;

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

    /**
     * Uploads a file to Cloudinary after validating and determining its format.
     *
     * @param file   the {@link MultipartFile} to upload
     * @param format the target format for Cloudinary (e.g., "pdf", "jpg", "png")
     * @return the public secure URL of the uploaded file on Cloudinary
     * @throws IOException if file reading or upload fails
     */
    public String uploadToCloudinary(MultipartFile file, String format) throws IOException {
        String extension = getExtension(file.getOriginalFilename());

        // Special handling: convert JFIF images to JPG before upload
        if (extension.equals("jfif")) {
            BufferedImage image = ImageIO.read(file.getInputStream());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", os);
            byte[] jpegBytes = os.toByteArray();

            // Upload converted image bytes to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(jpegBytes, ObjectUtils.asMap(
                    "resource_type", "image", // classify as image
                    "format", "jpg"
            ));
            return (String) uploadResult.get("secure_url");
        }

        // For all other file types, upload directly
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", format.equals("jpg") || format.equals("png") ? "image" : "raw",
                        "format", format
                )
        );

        // Return Cloudinary-hosted file URL
        return (String) uploadResult.get("secure_url");
    }
}
