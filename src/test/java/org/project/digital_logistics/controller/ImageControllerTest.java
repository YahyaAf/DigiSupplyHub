package org.project.digital_logistics.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.web.servlet.MockMvc;
import org.project.digital_logistics.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    private Path testImagePath;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary test image file
        testImagePath = tempDir.resolve("test-image.jpg");
        Files.write(testImagePath, "fake image content".getBytes());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // SERVE IMAGE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void serveImage_ExistingJpegFile_ReturnsOk() throws Exception {
        // Given
        when(fileStorageService.getFilePath("test-image.jpg")).thenReturn(testImagePath);

        // When & Then
        mockMvc.perform(get("/api/images/test-image.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"test-image.jpg\""));

        verify(fileStorageService).getFilePath("test-image.jpg");
    }

    @Test
    void serveImage_ExistingPngFile_ReturnsOk() throws Exception {
        // Given
        Path pngImagePath = tempDir.resolve("test-image.png");
        Files.write(pngImagePath, "fake png content".getBytes());

        when(fileStorageService.getFilePath("test-image.png")).thenReturn(pngImagePath);

        // When & Then
        mockMvc.perform(get("/api/images/test-image.png"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"test-image.png\""));

        verify(fileStorageService).getFilePath("test-image.png");
    }

    @Test
    void serveImage_FileNotFound_ReturnsNotFound() throws Exception {
        // Given
        Path nonExistentPath = tempDir.resolve("non-existent.jpg");
        when(fileStorageService.getFilePath("non-existent.jpg")).thenReturn(nonExistentPath);

        // When & Then
        mockMvc.perform(get("/api/images/non-existent.jpg"))
                .andExpect(status().isNotFound());

        verify(fileStorageService).getFilePath("non-existent.jpg");
    }

    @Test
    void serveImage_ExceptionThrown_ReturnsNotFound() throws Exception {
        // Given
        when(fileStorageService.getFilePath(anyString()))
                .thenThrow(new RuntimeException("File access error"));

        // When & Then
        mockMvc.perform(get("/api/images/error-file.jpg"))
                .andExpect(status().isNotFound());

        verify(fileStorageService).getFilePath("error-file.jpg");
    }
}