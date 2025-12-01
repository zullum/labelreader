package com.labelreader.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "artist_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    @Column(length = 100)
    private String genre;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "spotify_url", length = 500)
    private String spotifyUrl;

    @Column(name = "instagram_handle", length = 100)
    private String instagramHandle;

    @Column(name = "twitter_handle", length = 100)
    private String twitterHandle;

    @Column(name = "soundcloud_url", length = 500)
    private String soundcloudUrl;

    @Column(name = "total_submissions")
    private Integer totalSubmissions = 0;

    @Column(name = "total_plays")
    private Integer totalPlays = 0;
}
