package org.project.digital_logistics.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    @Test
    void uploadFile_Success_ReturnsS3Url() throws Exception {
        // Given
        String bucketName = "test-bucket";
        ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);

        // When
        String result = s3Service.uploadFile(file);

        // Then
        assertNotNull(result);
        assertTrue(result.startsWith("https://test-bucket.s3.amazonaws.com/uploads/"));
        assertTrue(result.contains("test-image.jpg"));

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_GeneratesUniqueFilename() throws Exception {
        // Given
        String bucketName = "test-bucket";
        ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);

        // When
        String result1 = s3Service.uploadFile(file);
        String result2 = s3Service.uploadFile(file);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1, result2, "Each upload should generate a unique filename");

        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_IOException_ThrowsRuntimeException() throws Exception {
        // Given
        String bucketName = "test-bucket";
        ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);

        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenThrow(new IOException("File read error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> s3Service.uploadFile(file)
        );

        assertEquals("Failed to upload file", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("File read error", exception.getCause().getMessage());

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_S3ClientError_ThrowsRuntimeException() throws Exception {
        // Given
        String bucketName = "test-bucket";
        ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> s3Service.uploadFile(file)
        );

        assertEquals("Failed to upload file", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("S3 connection failed", exception.getCause().getMessage());

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

}