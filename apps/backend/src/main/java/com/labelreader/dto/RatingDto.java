package com.labelreader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDto {
    private Long id;
    private Long submissionId;
    private Long labelId;
    private Integer rating;
    private String reviewText;
    private Boolean isInterested;
    private Integer listenedDurationSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
