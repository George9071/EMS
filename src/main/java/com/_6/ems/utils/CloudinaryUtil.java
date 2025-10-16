package com._6.ems.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.io.IOException;

@Component
@AllArgsConstructor
public class CloudinaryUtil {

    private final Cloudinary cloudinary;

    private final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "docx", "txt", "png", "csv", "jfif", "jpeg", "jpg"
    );

    private final List<String> ALLOWED_CONTENT_TYPES = List.of(
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
            "image/pjpeg",   // progressive JPEG
            "image/jpg",
            "image/jfif",

            // (optional) Fallback for unknown binary uploads
            "application/octet-stream"
    );

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
            throw new IllegalArgumentException("Only PDF, DOCX, and TXT files are allowed");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file content type: " + contentType);
        }
    }

    public String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public String getUploadFormat(String contentType, String filename) {
        if ("application/octet-stream".equals(contentType)) {
            String ext = this.getExtension(filename);
            if (ext.equals("jfif")) {
                return "jpg";
            }
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

    public String uploadToCloudinary(MultipartFile file, String format) throws IOException {
        String extension = getExtension(file.getOriginalFilename());

        if (extension.equals("jfif")) {
            BufferedImage image = ImageIO.read(file.getInputStream());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", os);
            byte[] jpegBytes = os.toByteArray();

            Map uploadResult = cloudinary.uploader().upload(jpegBytes, ObjectUtils.asMap(
                    "resource_type", "image", // for images
                    "format", "jpg"
            ));
            return (String) uploadResult.get("secure_url");
        }

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", format.equals("jpg") || format.equals("png") ? "image" : "raw",
                        "format", format
                )
        );

        return (String) uploadResult.get("secure_url");
    }
}
