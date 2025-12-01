package com.labelreader.service;

import com.labelreader.dto.SubmissionDto;
import com.labelreader.entity.Submission;
import com.labelreader.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final SubmissionRepository submissionRepository;

    public Page<SubmissionDto> discoverSubmissions(
            String genre,
            Submission.SubmissionStatus status,
            Pageable pageable) {

        Page<Submission> submissions;

        if (genre != null && !genre.isEmpty()) {
            // Filter by genre (you can enhance this with custom query)
            submissions = submissionRepository.findAll(pageable);
        } else if (status != null) {
            submissions = submissionRepository.findBySubmissionStatus(status, pageable);
        } else {
            submissions = submissionRepository.findAll(pageable);
        }

        return submissions.map(this::mapToDto);
    }

    public SubmissionDto getSubmissionForReview(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        return mapToDto(submission);
    }

    private SubmissionDto mapToDto(Submission submission) {
        return SubmissionDto.builder()
                .id(submission.getId())
                .artistId(submission.getArtistId())
                .title(submission.getTitle())
                .artistName(submission.getArtistName())
                .genre(submission.getGenre())
                .subGenre(submission.getSubGenre())
                .bpm(submission.getBpm())
                .keySignature(submission.getKeySignature())
                .filePath(submission.getFilePath())
                .fileSizeBytes(submission.getFileSizeBytes())
                .durationSeconds(submission.getDurationSeconds())
                .description(submission.getDescription())
                .lyrics(submission.getLyrics())
                .isPublished(submission.getIsPublished())
                .submissionStatus(submission.getSubmissionStatus().name())
                .playCount(submission.getPlayCount())
                .averageRating(submission.getAverageRating())
                .totalRatings(submission.getTotalRatings())
                .createdAt(submission.getCreatedAt())
                .updatedAt(submission.getUpdatedAt())
                .build();
    }
}
