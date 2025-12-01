package com.labelreader.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateArtistProfileRequest {
    @NotBlank(message = "Artist name is required")
    private String artistName;

    private String genre;
    private String websiteUrl;
    private String spotifyUrl;
    private String instagramHandle;
    private String twitterHandle;
    private String soundcloudUrl;
    private String bio;
    private String country;
}
