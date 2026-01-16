package com.labelreader.service;

import com.labelreader.dto.RatingDto;
import com.labelreader.dto.RatingRequest;
import com.labelreader.entity.LabelProfile;
import com.labelreader.entity.Rating;
import com.labelreader.entity.Submission;
import com.labelreader.repository.LabelProfileRepository;
import com.labelreader.repository.RatingRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private LabelProfileRepository labelProfileRepository;

    @InjectMocks
    private RatingService ratingService;

    private Rating testRating;
    private Submission testSubmission;
    private LabelProfile testLabelProfile;
    private RatingRequest ratingRequest;

    @BeforeEach
    void setUp() {
        testSubmission = Submission.builder()
                .id(1L)
                .artistId(1L)
                .title("Test Song")
                .artistName("Test Artist")
                .genre("Electronic")
                .averageRating(BigDecimal.ZERO)
                .totalRatings(0)
                .build();

        testRating = Rating.builder()
                .id(1L)
                .submissionId(1L)
                .labelId(1L)
                .rating(4)
                .reviewText("Great track!")
                .isInterested(true)
                .listenedDurationSeconds(180)
                .build();

        testLabelProfile = LabelProfile.builder()
                .id(1L)
                .userId(1L)
                .labelName("Test Label")
                .totalReviews(0)
                .totalSigned(0)
                .build();

        ratingRequest = new RatingRequest();
        ratingRequest.setRating(4);
        ratingRequest.setReviewText("Great track!");
        ratingRequest.setIsInterested(true);
        ratingRequest.setListenedDurationSeconds(180);
    }

    @Test
    void rateSubmission_NewRating_Success() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));
        when(ratingRepository.findBySubmissionIdAndLabelId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingRepository.calculateAverageRating(anyLong())).thenReturn(4.0);
        when(ratingRepository.countBySubmissionId(anyLong())).thenReturn(1);
        when(labelProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(testLabelProfile));

        RatingDto result = ratingService.rateSubmission(1L, 1L, ratingRequest);

        assertNotNull(result);
        assertEquals(4, result.getRating());
        assertEquals("Great track!", result.getReviewText());
        assertTrue(result.getIsInterested());
        verify(ratingRepository, times(1)).save(any(Rating.class));
        verify(submissionRepository, times(2)).findById(anyLong());
        verify(labelProfileRepository, times(1)).findByUserId(anyLong());
    }

    @Test
    void rateSubmission_UpdateExisting_Success() {
        testRating.setId(1L);
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));
        when(ratingRepository.findBySubmissionIdAndLabelId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testRating));
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingRepository.calculateAverageRating(anyLong())).thenReturn(4.5);
        when(ratingRepository.countBySubmissionId(anyLong())).thenReturn(2);

        RatingDto result = ratingService.rateSubmission(1L, 1L, ratingRequest);

        assertNotNull(result);
        assertEquals(4, result.getRating());
        verify(ratingRepository, times(1)).save(any(Rating.class));
        verify(labelProfileRepository, never()).findByUserId(anyLong());
    }

    @Test
    void rateSubmission_SubmissionNotFound_ThrowsException() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            ratingService.rateSubmission(999L, 1L, ratingRequest);
        });

        verify(ratingRepository, never()).save(any(Rating.class));
    }

    @Test
    void rateSubmission_UpdatesSubmissionAverageRating() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));
        when(ratingRepository.findBySubmissionIdAndLabelId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingRepository.calculateAverageRating(anyLong())).thenReturn(4.25);
        when(ratingRepository.countBySubmissionId(anyLong())).thenReturn(4);
        when(labelProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(testLabelProfile));

        ratingService.rateSubmission(1L, 1L, ratingRequest);

        verify(ratingRepository, times(1)).calculateAverageRating(anyLong());
        verify(ratingRepository, times(1)).countBySubmissionId(anyLong());
        verify(submissionRepository, times(2)).save(any(Submission.class));
    }

    @Test
    void rateSubmission_UpdatesLabelProfileReviewCount() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));
        when(ratingRepository.findBySubmissionIdAndLabelId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingRepository.calculateAverageRating(anyLong())).thenReturn(4.0);
        when(ratingRepository.countBySubmissionId(anyLong())).thenReturn(1);
        when(labelProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(testLabelProfile));

        ratingService.rateSubmission(1L, 1L, ratingRequest);

        verify(labelProfileRepository, times(1)).findByUserId(anyLong());
        verify(labelProfileRepository, times(1)).save(any(LabelProfile.class));
    }

    @Test
    void getLabelRatings_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Rating> page = new PageImpl<>(List.of(testRating));

        when(ratingRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<RatingDto> result = ratingService.getLabelRatings(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(4, result.getContent().get(0).getRating());
    }

    @Test
    void getSubmissionRating_Found() {
        when(ratingRepository.findBySubmissionIdAndLabelId(anyLong(), anyLong()))
                .thenReturn(Optional.of(testRating));

        RatingDto result = ratingService.getSubmissionRating(1L, 1L);

        assertNotNull(result);
        assertEquals(4, result.getRating());
        assertEquals("Great track!", result.getReviewText());
    }

    @Test
    void getSubmissionRating_NotFound() {
        when(ratingRepository.findBySubmissionIdAndLabelId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        RatingDto result = ratingService.getSubmissionRating(1L, 1L);

        assertNull(result);
    }

    @Test
    void rateSubmission_WithNoReviewText() {
        ratingRequest.setReviewText(null);
        testRating.setReviewText(null);

        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));
        when(ratingRepository.findBySubmissionIdAndLabelId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingRepository.calculateAverageRating(anyLong())).thenReturn(4.0);
        when(ratingRepository.countBySubmissionId(anyLong())).thenReturn(1);
        when(labelProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(testLabelProfile));

        RatingDto result = ratingService.rateSubmission(1L, 1L, ratingRequest);

        assertNotNull(result);
        assertNull(result.getReviewText());
    }

    @Test
    void rateSubmission_NotInterested() {
        ratingRequest.setIsInterested(false);
        testRating.setIsInterested(false);

        when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(testSubmission));
        when(ratingRepository.findBySubmissionIdAndLabelId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingRepository.calculateAverageRating(anyLong())).thenReturn(3.0);
        when(ratingRepository.countBySubmissionId(anyLong())).thenReturn(1);
        when(labelProfileRepository.findByUserId(anyLong())).thenReturn(Optional.of(testLabelProfile));

        RatingDto result = ratingService.rateSubmission(1L, 1L, ratingRequest);

        assertNotNull(result);
        assertFalse(result.getIsInterested());
    }
}
