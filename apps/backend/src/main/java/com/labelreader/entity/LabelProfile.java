package com.labelreader.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "label_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "label_name", nullable = false)
    private String labelName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "genres_interested", columnDefinition = "JSON")
    private String genresInterested;

    @Column(length = 100)
    private String country;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "total_signed")
    private Integer totalSigned = 0;
}
