package com.example.candidateregistration.service;

import com.example.candidateregistration.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    private Path storageLocation;

    @PostConstruct
    public void init() {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create the resume upload directory: " + storageLocation, e);
        }
    }

    /**
     * Validates and stores the uploaded resume, returning the generated,
     * collision-proof filename it was stored under (not the original name).
     */
    public String storeResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Resume file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidRequestException("Resume file must be 5MB or smaller");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());

        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidRequestException("Only PDF, DOC, or DOCX files are allowed for the resume");
        }

        String storedFilename = UUID.randomUUID() + "." + extension.toLowerCase();

        try {
            Path target = storageLocation.resolve(storedFilename).normalize();
            Files.copy(file.getInputStream(), target);
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store resume file", e);
        }
    }

    public Resource loadAsResource(String storedFilename) {
        try {
            Path filePath = storageLocation.resolve(storedFilename).normalize();
            System.out.println(filePath.getFileName());
            Resource resource = new UrlResource(filePath.toUri());
            System.out.println("resource:"+resource);
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Resume file not found on disk: " + storedFilename);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid resume file path: " + storedFilename, e);
        }
    }

    /** Best-effort delete, used when a resume needs to be replaced. */
    public void deleteIfExists(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) return;
        try {
            Files.deleteIfExists(storageLocation.resolve(storedFilename).normalize());
        } catch (IOException ignored) {
            // Non-fatal: an orphaned file on disk is not worth failing the request over.
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }
}
