package com.labelreader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelProfileDto {
    private Long id;
    private Long userId;
    private String labelName;
    private String companyName;
    private String websiteUrl;
    private List<String> genresInterested;
    private String country;
    private Integer totalReviews;
    private Integer totalSigned;
}
