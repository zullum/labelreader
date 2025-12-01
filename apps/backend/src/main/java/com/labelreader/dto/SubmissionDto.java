package com.labelreader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDto {
    private Long id;
    private Long artistId;
    private String title;
    private String artistName;
    private String genre;
    private String subGenre;
    private Integer bpm;
    private String keySignature;
    private String filePath;
    private Long fileSizeBytes;
    private Integer durationSeconds;
    private String description;
    private String lyrics;
    private Boolean isPublished;
    private String submissionStatus;
    private Integer playCount;
    private BigDecimal averageRating;
    private Integer totalRatings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
