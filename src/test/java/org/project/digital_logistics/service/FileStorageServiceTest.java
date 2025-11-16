package org.project.digital_logistics.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private String uploadDir;

    @BeforeEach
    void setUp() {
        uploadDir = tempDir.resolve("test-uploads").toString();
        fileStorageService = new FileStorageService(uploadDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up: delete all files in upload directory
        Path uploadPath = Paths.get(uploadDir);
        if (Files.exists(uploadPath)) {
            Files.walk(uploadPath)
                    .sorted((a, b) -> -a.compareTo(b)) // Reverse order (files before directories)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CONSTRUCTOR TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void constructor_CreatesUploadDirectory() {
        // Given & When
        Path uploadPath = Paths.get(uploadDir);

        // Then
        assertTrue(Files.exists(uploadPath));
        assertTrue(Files.isDirectory(uploadPath));
    }

    @Test
    void constructor_InvalidPath_ThrowsException() {
        // Given
        String invalidPath = "\0invalid/path"; // Null character in path

        // When & Then
        assertThrows(RuntimeException.class, () -> new FileStorageService(invalidPath));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // STORE FILE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void storeFile_ValidImageFile_Success() throws IOException {
        // Given
        byte[] content = "test image content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                content
        );

        // When
        String filename = fileStorageService.storeFile(file);

        // Then
        assertNotNull(filename);
        assertTrue(filename.endsWith(".jpg"));
        assertTrue(filename.length() > 4); // UUID + extension

        // Verify file exists
        Path filePath = fileStorageService.getFilePath(filename);
        assertTrue(Files.exists(filePath));

        // Verify content
        byte[] storedContent = Files.readAllBytes(filePath);
        assertArrayEquals(content, storedContent);
    }

    @Test
    void storeFile_PngImage_Success() {
        // Given
        byte[] content = "png image".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.png",
                "image/png",
                content
        );

        // When
        String filename = fileStorageService.storeFile(file);

        // Then
        assertNotNull(filename);
        assertTrue(filename.endsWith(".png"));
    }

    @Test
    void storeFile_EmptyFile_ThrowsException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.storeFile(emptyFile)
        );

        assertEquals("Cannot store empty file", exception.getMessage());
    }

    @Test
    void storeFile_NonImageFile_ThrowsException() {
        // Given
        byte[] content = "pdf content".getBytes();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                content
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.storeFile(pdfFile)
        );

        assertEquals("Only image files are allowed", exception.getMessage());
    }

    @Test
    void storeFile_NullContentType_ThrowsException() {
        // Given
        byte[] content = "some content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                null, // Null content type
                content
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.storeFile(file)
        );

        assertEquals("Only image files are allowed", exception.getMessage());
    }

    @Test
    void storeFile_FileWithoutExtension_Success() {
        // Given
        byte[] content = "image content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "imagefile", // No extension
                "image/jpeg",
                content
        );

        // When
        String filename = fileStorageService.storeFile(file);

        // Then
        assertNotNull(filename);
        // Should still work, just no extension
        assertTrue(filename.length() > 0);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE FILE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteFile_ExistingFile_Success() throws IOException {
        // Given
        byte[] content = "test content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                content
        );
        String filename = fileStorageService.storeFile(file);

        // Verify file exists
        Path filePath = fileStorageService.getFilePath(filename);
        assertTrue(Files.exists(filePath));

        // When
        fileStorageService.deleteFile(filename);

        // Then
        assertFalse(Files.exists(filePath));
    }

    @Test
    void deleteFile_NonExistentFile_DoesNotThrow() {
        // Given
        String nonExistentFilename = "non-existent-file.jpg";

        // When & Then
        assertDoesNotThrow(() -> fileStorageService.deleteFile(nonExistentFilename));
    }

    @Test
    void deleteFile_NullFilename_DoesNothing() {
        // When & Then
        assertDoesNotThrow(() -> fileStorageService.deleteFile(null));
    }

    @Test
    void deleteFile_EmptyFilename_DoesNothing() {
        // When & Then
        assertDoesNotThrow(() -> fileStorageService.deleteFile(""));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET FILE PATH TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getFilePath_ReturnsCorrectPath() {
        // Given
        String filename = "test-file.jpg";

        // When
        Path filePath = fileStorageService.getFilePath(filename);

        // Then
        assertNotNull(filePath);
        assertTrue(filePath.toString().contains(filename));
        assertTrue(filePath.isAbsolute());
    }
}