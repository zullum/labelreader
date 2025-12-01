package com.labelreader.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateLabelProfileRequest {
    @NotBlank(message = "Label name is required")
    private String labelName;

    private String companyName;
    private String websiteUrl;
    private List<String> genresInterested;
    private String bio;
    private String country;
}
