package com.labelreader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistProfileDto {
    private Long id;
    private Long userId;
    private String artistName;
    private String genre;
    private String websiteUrl;
    private String spotifyUrl;
    private String instagramHandle;
    private String twitterHandle;
    private String soundcloudUrl;
    private Integer totalSubmissions;
    private Integer totalPlays;
}
