package com.labelreader.service;

import com.labelreader.dto.SubmissionDto;
import com.labelreader.dto.SubmissionRequest;
import com.labelreader.entity.ArtistProfile;
import com.labelreader.entity.Submission;
import com.labelreader.repository.ArtistProfileRepository;
import com.labelreader.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ArtistProfileRepository artistProfileRepository;

    @InjectMocks
    private SubmissionService submissionService;

    private Submission testSubmission;
    private SubmissionRequest submissionRequest;
    private ArtistProfile testArtistProfile;

    @BeforeEach
    void setUp() {
        testSubmission = Submission.builder()
                .id(1L)
                .artistId(1L)
                .title("Test Song")
                .artistName("Test Artist")
                .genre("Electronic")
                .subGenre("House")
                .bpm(128)
                .keySignature("Am")
                .filePath("test-file.mp3")
                .fileSizeBytes(5000000L)
                .durationSeconds(240)
                .description("Test description")
                .isPublished(true)
                .submissionStatus(Submission.SubmissionStatus.PENDING)
                .playCount(0)
                .averageRating(BigDecimal.ZERO)
                .totalRatings(0)
                .releaseDate(LocalDate.now())
                .build();

        submissionRequest = new SubmissionRequest();
        submissionRequest.setTitle("Test Song");
        submissionRequest.setArtistName("Test Artist");
        submissionRequest.setGenre("Electronic");
        submissionRequest.setSubGenre("House");
        submissionRequest.setBpm(128);
        submissionRequest.setKeySignature("Am");
        submissionRequest.setDescription("Test description");

        testArtistProfile = ArtistProfile.builder()
                .id(1L)
                .userId(1L)
                .artistName("Test Artist")
                .totalSubmissions(0)
                .totalPlays(0)
                .build();

        ReflectionTestUtils.setField(submissionService, "uploadPath", "./test-uploads");
    }

    @Test
    void createSubmission_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp3",
                "audio/mpeg",
                "test content".getBytes()
        );

        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        when(artistProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(testArtistProfile));

        SubmissionDto result = submissionService.createSubmission(1L, file, submissionRequest);

        assertNotNull(result);
        assertEquals("Test Song", result.getTitle());
        assertEquals("Electronic", result.getGenre());
        verify(submissionRepository, times(1)).save(any(Submission.class));
        verify(artistProfileRepository, times(1)).findByUserId(anyLong());
    }

    @Test
    void createSubmission_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp3",
                "audio/mpeg",
                new byte[0]
        );

        assertThrows(RuntimeException.class, () -> {
            submissionService.createSubmission(1L, file, submissionRequest);
        });

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void createSubmission_InvalidFileType_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        assertThrows(RuntimeException.class, () -> {
            submissionService.createSubmission(1L, file, submissionRequest);
        });

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void createSubmission_FileTooLarge_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp3",
                "audio/mpeg",
                new byte[51 * 1024 * 1024]
        );

        assertThrows(RuntimeException.class, () -> {
            submissionService.createSubmission(1L, file, submissionRequest);
        });

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void getArtistSubmissions_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Submission> page = new PageImpl<>(List.of(testSubmission));

        when(submissionRepository.findByArtistId(anyLong(), any(Pageable.class))).thenReturn(page);

        Page<SubmissionDto> result = submissionService.getArtistSubmissions(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Song", result.getContent().get(0).getTitle());
    }

    @Test
    void getSubmission_Success() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));

        SubmissionDto result = submissionService.getSubmission(1L, 1L);

        assertNotNull(result);
        assertEquals("Test Song", result.getTitle());
    }

    @Test
    void getSubmission_NotFound_ThrowsException() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            submissionService.getSubmission(999L, 1L);
        });
    }

    @Test
    void getSubmission_Unauthorized_ThrowsException() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));

        assertThrows(RuntimeException.class, () -> {
            submissionService.getSubmission(1L, 999L);
        });
    }

    @Test
    void deleteSubmission_Success() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));
        when(artistProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(testArtistProfile));
        doNothing().when(submissionRepository).delete(any(Submission.class));

        assertDoesNotThrow(() -> {
            submissionService.deleteSubmission(1L, 1L);
        });

        verify(submissionRepository, times(1)).delete(any(Submission.class));
        verify(artistProfileRepository, times(1)).findByUserId(anyLong());
    }

    @Test
    void deleteSubmission_Unauthorized_ThrowsException() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));

        assertThrows(RuntimeException.class, () -> {
            submissionService.deleteSubmission(1L, 999L);
        });

        verify(submissionRepository, never()).delete(any(Submission.class));
    }

    @Test
    void deleteSubmission_NotFound_ThrowsException() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            submissionService.deleteSubmission(1L, 1L);
        });

        verify(submissionRepository, never()).delete(any(Submission.class));
    }
}
