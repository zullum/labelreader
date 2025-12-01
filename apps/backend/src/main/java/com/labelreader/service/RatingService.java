package com.labelreader.service;

import com.labelreader.dto.RatingDto;
import com.labelreader.dto.RatingRequest;
import com.labelreader.entity.LabelProfile;
import com.labelreader.entity.Rating;
import com.labelreader.entity.Submission;
import com.labelreader.repository.LabelProfileRepository;
import com.labelreader.repository.RatingRepository;
import com.labelreader.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final SubmissionRepository submissionRepository;
    private final LabelProfileRepository labelProfileRepository;

    @Transactional
    public RatingDto rateSubmission(Long submissionId, Long labelId, RatingRequest request) {
        // Verify submission exists
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        // Create or update rating
        Rating rating = ratingRepository
                .findBySubmissionIdAndLabelId(submissionId, labelId)
                .orElse(new Rating());

        boolean isNewRating = rating.getId() == null;

        rating.setSubmissionId(submissionId);
        rating.setLabelId(labelId);
        rating.setRating(request.getRating());
        rating.setReviewText(request.getReviewText());
        rating.setIsInterested(request.getIsInterested());
        rating.setListenedDurationSeconds(request.getListenedDurationSeconds());

        rating = ratingRepository.save(rating);

        // Update submission average rating
        updateSubmissionAverageRating(submissionId);

        // Update label profile review count
        if (isNewRating) {
            labelProfileRepository.findByUserId(labelId).ifPresent(profile -> {
                profile.setTotalReviews(profile.getTotalReviews() + 1);
                labelProfileRepository.save(profile);
            });
        }

        return mapToDto(rating);
    }

    public Page<RatingDto> getLabelRatings(Long labelId, Pageable pageable) {
        // Note: You'll need to add this method to RatingRepository
        return ratingRepository.findAll(pageable).map(this::mapToDto);
    }

    public RatingDto getSubmissionRating(Long submissionId, Long labelId) {
        Rating rating = ratingRepository.findBySubmissionIdAndLabelId(submissionId, labelId)
                .orElse(null);

        return rating != null ? mapToDto(rating) : null;
    }

    private void updateSubmissionAverageRating(Long submissionId) {
        Double avgRating = ratingRepository.calculateAverageRating(submissionId);
        Integer totalRatings = ratingRepository.countBySubmissionId(submissionId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        submission.setAverageRating(
                avgRating != null
                        ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
        submission.setTotalRatings(totalRatings);

        submissionRepository.save(submission);
    }

    private RatingDto mapToDto(Rating rating) {
        return RatingDto.builder()
                .id(rating.getId())
                .submissionId(rating.getSubmissionId())
                .labelId(rating.getLabelId())
                .rating(rating.getRating())
                .reviewText(rating.getReviewText())
                .isInterested(rating.getIsInterested())
                .listenedDurationSeconds(rating.getListenedDurationSeconds())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}
