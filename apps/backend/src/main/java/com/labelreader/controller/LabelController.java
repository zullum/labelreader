package com.labelreader.controller;

import com.labelreader.dto.LabelProfileDto;
import com.labelreader.dto.RatingDto;
import com.labelreader.dto.RatingRequest;
import com.labelreader.dto.SubmissionDto;
import com.labelreader.dto.UpdateLabelProfileRequest;
import com.labelreader.entity.Submission;
import com.labelreader.service.DiscoveryService;
import com.labelreader.service.LabelProfileService;
import com.labelreader.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/label")
@RequiredArgsConstructor
public class LabelController {

    private final LabelProfileService labelProfileService;
    private final DiscoveryService discoveryService;
    private final RatingService ratingService;

    @GetMapping("/profile")
    public ResponseEntity<LabelProfileDto> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(labelProfileService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<LabelProfileDto> updateProfile(
            @Valid @RequestBody UpdateLabelProfileRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(labelProfileService.updateProfile(userId, request));
    }

    @GetMapping("/discover")
    public ResponseEntity<Page<SubmissionDto>> discoverSubmissions(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Submission.SubmissionStatus submissionStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                submissionStatus = Submission.SubmissionStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }

        Page<SubmissionDto> submissions = discoveryService.discoverSubmissions(
                genre, submissionStatus, pageable);

        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<SubmissionDto> getSubmission(@PathVariable Long id) {
        return ResponseEntity.ok(discoveryService.getSubmissionForReview(id));
    }

    @PostMapping("/ratings")
    public ResponseEntity<RatingDto> rateSubmission(
            @Valid @RequestBody RatingRequest request,
            @RequestParam Long submissionId,
            Authentication authentication) {

        Long labelId = (Long) authentication.getPrincipal();
        RatingDto rating = ratingService.rateSubmission(submissionId, labelId, request);
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/ratings")
    public ResponseEntity<Page<RatingDto>> getMyRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long labelId = (Long) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RatingDto> ratings = ratingService.getLabelRatings(labelId, pageable);

        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/submissions/{submissionId}/rating")
    public ResponseEntity<RatingDto> getSubmissionRating(
            @PathVariable Long submissionId,
            Authentication authentication) {

        Long labelId = (Long) authentication.getPrincipal();
        RatingDto rating = ratingService.getSubmissionRating(submissionId, labelId);

        if (rating == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(rating);
    }
}
