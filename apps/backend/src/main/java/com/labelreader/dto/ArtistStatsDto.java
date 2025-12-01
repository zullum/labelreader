package com.labelreader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistStatsDto {
    private Integer totalSubmissions;
    private Integer totalPlays;
    private BigDecimal averageRating;
    private Integer pendingSubmissions;
    private Integer approvedSubmissions;
    private Integer rejectedSubmissions;
    private Integer signingRequests;
}
