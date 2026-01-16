package com.labelreader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalyticsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayCountByDate {
        private LocalDate date;
        private Long playCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSubmission {
        private Long id;
        private String title;
        private String artistName;
        private Long playCount;
        private Double averageRating;
        private Integer totalRatings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenreDistribution {
        private String genre;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtistAnalytics {
        private Long totalSubmissions;
        private Long totalPlays;
        private Double averageRating;
        private Integer totalRatings;
        private Integer signingRequests;
        private List<PlayCountByDate> playsByDate;
        private List<TopSubmission> topSubmissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelAnalytics {
        private Long totalReviews;
        private Long totalSigningRequests;
        private Double averageRatingGiven;
        private Map<String, Long> reviewsByGenre;
        private List<TopSubmission> recentlyReviewed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformAnalytics {
        private Long totalSubmissions;
        private Long totalArtists;
        private Long totalLabels;
        private Long totalPlays;
        private List<GenreDistribution> genreDistribution;
        private List<TopSubmission> topRatedSubmissions;
        private List<TopSubmission> mostPlayedSubmissions;
    }
}
