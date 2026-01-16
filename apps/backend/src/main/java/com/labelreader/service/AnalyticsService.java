package com.labelreader.service;

import com.labelreader.dto.AnalyticsDto;
import com.labelreader.entity.Submission;
import com.labelreader.entity.User;
import com.labelreader.repository.PlayHistoryRepository;
import com.labelreader.repository.RatingRepository;
import com.labelreader.repository.SubmissionRepository;
import com.labelreader.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PlayHistoryRepository playHistoryRepository;
    private final SubmissionRepository submissionRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    public AnalyticsDto.ArtistAnalytics getArtistAnalytics(User artist, Integer days) {
        if (days == null) days = 30;
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<Submission> submissions = submissionRepository.findByArtistId(artist.getId());
        Long totalSubmissions = (long) submissions.size();

        Long totalPlays = playHistoryRepository.countTotalPlaysByArtist(artist);

        Double averageRating = submissions.stream()
                .map(s -> s.getAverageRating() != null ? s.getAverageRating().doubleValue() : 0.0)
                .filter(r -> r > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Integer totalRatings = submissions.stream()
                .mapToInt(s -> s.getTotalRatings() != null ? s.getTotalRatings() : 0)
                .sum();

        // For now, we don't have a SigningRequest entity, so we'll return 0
        Integer signingRequests = 0;

        List<Object[]> playsByDateData = playHistoryRepository.countPlaysByDateForArtist(artist, startDate);
        List<AnalyticsDto.PlayCountByDate> playsByDate = playsByDateData.stream()
                .map(row -> AnalyticsDto.PlayCountByDate.builder()
                        .date((LocalDate) row[0])
                        .playCount((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        List<Object[]> topSubmissionsData = playHistoryRepository.findTopSubmissionsByArtist(artist, PageRequest.of(0, 5));
        List<AnalyticsDto.TopSubmission> topSubmissions = topSubmissionsData.stream()
                .map(row -> {
                    Submission sub = (Submission) row[0];
                    Long playCount = (Long) row[1];
                    return AnalyticsDto.TopSubmission.builder()
                            .id(sub.getId())
                            .title(sub.getTitle())
                            .artistName(sub.getArtistName())
                            .playCount(playCount)
                            .averageRating(sub.getAverageRating() != null ? sub.getAverageRating().doubleValue() : null)
                            .totalRatings(sub.getTotalRatings())
                            .build();
                })
                .collect(Collectors.toList());

        return AnalyticsDto.ArtistAnalytics.builder()
                .totalSubmissions(totalSubmissions)
                .totalPlays(totalPlays)
                .averageRating(averageRating)
                .totalRatings(totalRatings)
                .signingRequests(signingRequests)
                .playsByDate(playsByDate)
                .topSubmissions(topSubmissions)
                .build();
    }

    public AnalyticsDto.LabelAnalytics getLabelAnalytics(User label) {
        Long totalReviews = ratingRepository.countByLabelId(label.getId());
        Long totalSigningRequests = 0L; // No SigningRequest entity yet

        Double averageRatingGiven = ratingRepository.findAverageRatingByLabelId(label.getId());
        if (averageRatingGiven == null) averageRatingGiven = 0.0;

        Map<String, Long> reviewsByGenre = ratingRepository.countRatingsByGenreForLabel(label.getId())
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        List<Submission> recentlyReviewedSubmissions = ratingRepository.findRecentlyRatedSubmissionsByLabel(label.getId(), PageRequest.of(0, 5));
        List<AnalyticsDto.TopSubmission> recentlyReviewed = recentlyReviewedSubmissions.stream()
                .map(sub -> AnalyticsDto.TopSubmission.builder()
                        .id(sub.getId())
                        .title(sub.getTitle())
                        .artistName(sub.getArtistName())
                        .playCount(sub.getPlayCount() != null ? sub.getPlayCount().longValue() : 0L)
                        .averageRating(sub.getAverageRating() != null ? sub.getAverageRating().doubleValue() : null)
                        .totalRatings(sub.getTotalRatings())
                        .build())
                .collect(Collectors.toList());

        return AnalyticsDto.LabelAnalytics.builder()
                .totalReviews(totalReviews)
                .totalSigningRequests(totalSigningRequests)
                .averageRatingGiven(averageRatingGiven)
                .reviewsByGenre(reviewsByGenre)
                .recentlyReviewed(recentlyReviewed)
                .build();
    }

    public AnalyticsDto.PlatformAnalytics getPlatformAnalytics() {
        Long totalSubmissions = submissionRepository.count();
        Long totalArtists = userRepository.countByUserType(User.UserType.ARTIST);
        Long totalLabels = userRepository.countByUserType(User.UserType.LABEL);
        Long totalPlays = playHistoryRepository.count();

        Map<String, Long> genreCounts = submissionRepository.countByGenre()
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        double total = genreCounts.values().stream().mapToLong(Long::longValue).sum();
        List<AnalyticsDto.GenreDistribution> genreDistribution = genreCounts.entrySet().stream()
                .map(entry -> AnalyticsDto.GenreDistribution.builder()
                        .genre(entry.getKey())
                        .count(entry.getValue())
                        .percentage(total > 0 ? (entry.getValue() / total) * 100.0 : 0.0)
                        .build())
                .collect(Collectors.toList());

        List<Submission> topRated = submissionRepository.findTopRatedSubmissions(PageRequest.of(0, 10));
        List<AnalyticsDto.TopSubmission> topRatedSubmissions = topRated.stream()
                .map(sub -> AnalyticsDto.TopSubmission.builder()
                        .id(sub.getId())
                        .title(sub.getTitle())
                        .artistName(sub.getArtistName())
                        .playCount(sub.getPlayCount() != null ? sub.getPlayCount().longValue() : 0L)
                        .averageRating(sub.getAverageRating() != null ? sub.getAverageRating().doubleValue() : null)
                        .totalRatings(sub.getTotalRatings())
                        .build())
                .collect(Collectors.toList());

        List<Submission> mostPlayed = submissionRepository.findMostPlayedSubmissions(PageRequest.of(0, 10));
        List<AnalyticsDto.TopSubmission> mostPlayedSubmissions = mostPlayed.stream()
                .map(sub -> AnalyticsDto.TopSubmission.builder()
                        .id(sub.getId())
                        .title(sub.getTitle())
                        .artistName(sub.getArtistName())
                        .playCount(sub.getPlayCount() != null ? sub.getPlayCount().longValue() : 0L)
                        .averageRating(sub.getAverageRating() != null ? sub.getAverageRating().doubleValue() : null)
                        .totalRatings(sub.getTotalRatings())
                        .build())
                .collect(Collectors.toList());

        return AnalyticsDto.PlatformAnalytics.builder()
                .totalSubmissions(totalSubmissions)
                .totalArtists(totalArtists)
                .totalLabels(totalLabels)
                .totalPlays(totalPlays)
                .genreDistribution(genreDistribution)
                .topRatedSubmissions(topRatedSubmissions)
                .mostPlayedSubmissions(mostPlayedSubmissions)
                .build();
    }
}
