package com.labelreader.controller;

import com.labelreader.dto.SubmissionDto;
import com.labelreader.dto.SubmissionRequest;
import com.labelreader.service.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/artist/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionDto> uploadSubmission(
            @RequestParam("file") MultipartFile file,
            @RequestParam("metadata") String metadataJson,
            Authentication authentication) throws IOException {

        Long artistId = (Long) authentication.getPrincipal();
        SubmissionRequest request = objectMapper.readValue(metadataJson, SubmissionRequest.class);

        SubmissionDto submission = submissionService.createSubmission(artistId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(submission);
    }

    @GetMapping
    public ResponseEntity<Page<SubmissionDto>> getSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) {

        Long artistId = (Long) authentication.getPrincipal();
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SubmissionDto> submissions = submissionService.getArtistSubmissions(artistId, pageable);

        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> getSubmission(
            @PathVariable Long id,
            Authentication authentication) {

        Long artistId = (Long) authentication.getPrincipal();
        SubmissionDto submission = submissionService.getSubmission(id, artistId);
        return ResponseEntity.ok(submission);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(
            @PathVariable Long id,
            Authentication authentication) {

        Long artistId = (Long) authentication.getPrincipal();
        submissionService.deleteSubmission(id, artistId);
        return ResponseEntity.noContent().build();
    }
}
