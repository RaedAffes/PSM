package com.psm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.psm.service.FileStorageService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesAllowedFileWithGeneratedKey() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile("file", "photo.PNG", "image/png", new byte[] { 1, 2, 3 });

        FileStorageService.StoredFile stored = service.store(file);

        assertThat(stored.originalName()).isEqualTo("photo.PNG");
        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.storageKey()).endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(stored.storageKey()))).isTrue();
    }

    @Test
    void stripsPathFromOriginalFileName() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile("file", "../../secret.pdf", "application/pdf", new byte[] { 1 });

        FileStorageService.StoredFile stored = service.store(file);

        assertThat(stored.originalName()).isEqualTo("secret.pdf");
        assertThat(stored.storageKey()).doesNotContain("..");
    }

    @Test
    void rejectsUnsupportedContentType() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile("file", "script.html", "text/html", new byte[] { 1 });

        assertThatThrownBy(() -> service.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported file type");
    }

    @Test
    void rejectsExtensionThatDoesNotMatchContentType() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile("file", "evil.exe", "image/png", new byte[] { 1 });

        assertThatThrownBy(() -> service.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported file type");
    }

    @Test
    void rejectsOversizedFile() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 2);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] { 1, 2, 3 });

        assertThatThrownBy(() -> service.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File too large");
    }

    @Test
    void rejectsEmptyFile() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.store(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File is empty");
    }

    @Test
    void deletesStoredFile() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile(
            "file", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8)
        );

        FileStorageService.StoredFile stored = service.store(file);
        service.delete(stored.storageKey());

        assertThat(Files.exists(tempDir.resolve(stored.storageKey()))).isFalse();
    }

    @Test
    void storeImageAcceptsSupportedImage() {
        FileStorageService service = new FileStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile("file", "cover.webp", "image/webp", new byte[] { 1, 2 });

        FileStorageService.StoredFile stored = service.storeImage(file);

        assertThat(stored.contentType()).isEqualTo("image/webp");
        assertThat(Files.exists(tempDir.resolve(stored.storageKey()))).isTrue();
    }

    @Test
    void storeImageRejectsNonImageAndCleansUp() throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[] { 1 });

        assertThatThrownBy(() -> service.storeImage(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported file type");

        try (var files = Files.walk(tempDir)) {
            assertThat(files.filter(Files::isRegularFile).count()).isZero();
        }
    }
}
