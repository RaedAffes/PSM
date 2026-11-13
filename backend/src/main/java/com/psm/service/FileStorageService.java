package com.psm.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileStorageService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM");

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.ofEntries(
        Map.entry("image/jpeg", Set.of("jpg", "jpeg")),
        Map.entry("image/jpg", Set.of("jpg", "jpeg")),
        Map.entry("image/png", Set.of("png")),
        Map.entry("image/gif", Set.of("gif")),
        Map.entry("image/webp", Set.of("webp")),
        Map.entry("application/pdf", Set.of("pdf")),
        Map.entry("text/plain", Set.of("txt")),
        Map.entry("text/csv", Set.of("csv")),
        Map.entry("application/zip", Set.of("zip")),
        Map.entry("application/msword", Set.of("doc")),
        Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of("docx")),
        Map.entry("application/vnd.ms-excel", Set.of("xls")),
        Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Set.of("xlsx")),
        Map.entry("application/vnd.ms-powerpoint", Set.of("ppt")),
        Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", Set.of("pptx"))
    );

    private final Path root;
    private final long maxBytes;

    public FileStorageService(
        @Value("${psm.storage.root:./uploads}") String rootPath,
        @Value("${psm.storage.max-file-size-bytes:10485760}") long maxBytes
    ) {
        this.root = Paths.get(rootPath).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create upload directory: " + this.root, exception);
        }
        log.info("File storage root: {} (max {} bytes per file)", this.root, maxBytes);
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File too large");
        }

        String contentType = Optional.ofNullable(file.getContentType())
            .map(value -> value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
            .orElse("application/octet-stream");
        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        Set<String> allowedExtensions = ALLOWED_TYPES.get(contentType);
        if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        String storageKey = LocalDate.now().format(DATE_PATH) + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store uploaded file", exception);
        }

        return new StoredFile(storageKey, originalName, contentType, file.getSize());
    }

    public StoredFile storeImage(MultipartFile file) {
        StoredFile stored = store(file);
        if (!stored.contentType().startsWith("image/") || !IMAGE_EXTENSIONS.contains(extensionOf(stored.originalName()))) {
            delete(stored.storageKey());
            throw new IllegalArgumentException("Unsupported file type");
        }
        return stored;
    }

    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Stored file is missing");
        }
        return new FileSystemResource(path);
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            log.warn("Unable to delete stored file {}: {}", storageKey, exception.getMessage());
        }
    }

    private Path resolve(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return path;
    }

    private String sanitizeFileName(String original) {
        String name = Optional.ofNullable(original).orElse("file");
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            name = "file";
        }
        if (name.length() > 180) {
            String suffix = "";
            int dot = name.lastIndexOf('.');
            if (dot > -1 && dot < name.length() - 1) {
                suffix = name.substring(dot);
            }
            name = name.substring(0, Math.max(0, 180 - suffix.length())) + suffix;
        }
        return name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String storageKey, String originalName, String contentType, long size) {
    }
}
