package com.labelreader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmissionRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Artist name is required")
    private String artistName;

    private String genre;
    private String subGenre;
    private Integer bpm;
    private String keySignature;
    private String description;
    private String lyrics;
}
