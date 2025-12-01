package com.labelreader.service;

import com.labelreader.dto.ArtistStatsDto;
import com.labelreader.entity.ArtistProfile;
import com.labelreader.entity.Submission;
import com.labelreader.repository.ArtistProfileRepository;
import com.labelreader.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistStatsService {

    private final ArtistProfileRepository artistProfileRepository;
    private final SubmissionRepository submissionRepository;

    public ArtistStatsDto getStats(Long userId) {
        ArtistProfile profile = artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        List<Submission> submissions = submissionRepository.findByArtistId(userId, null).getContent();

        int pendingCount = (int) submissions.stream()
                .filter(s -> s.getSubmissionStatus() == Submission.SubmissionStatus.PENDING)
                .count();

        int approvedCount = (int) submissions.stream()
                .filter(s -> s.getSubmissionStatus() == Submission.SubmissionStatus.APPROVED)
                .count();

        int rejectedCount = (int) submissions.stream()
                .filter(s -> s.getSubmissionStatus() == Submission.SubmissionStatus.REJECTED)
                .count();

        BigDecimal avgRating = submissions.stream()
                .map(Submission::getAverageRating)
                .filter(rating -> rating != null && rating.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, submissions.size())), 2, RoundingMode.HALF_UP);

        return ArtistStatsDto.builder()
                .totalSubmissions(profile.getTotalSubmissions())
                .totalPlays(profile.getTotalPlays())
                .averageRating(avgRating)
                .pendingSubmissions(pendingCount)
                .approvedSubmissions(approvedCount)
                .rejectedSubmissions(rejectedCount)
                .signingRequests(0) // TODO: Implement when signing requests are ready
                .build();
    }
}
