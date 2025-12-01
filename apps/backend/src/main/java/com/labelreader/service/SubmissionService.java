package com.labelreader.service;

import com.labelreader.dto.SubmissionDto;
import com.labelreader.dto.SubmissionRequest;
import com.labelreader.entity.ArtistProfile;
import com.labelreader.entity.Submission;
import com.labelreader.repository.ArtistProfileRepository;
import com.labelreader.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ArtistProfileRepository artistProfileRepository;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Transactional
    public SubmissionDto createSubmission(
            Long artistId,
            MultipartFile file,
            SubmissionRequest request) throws IOException {

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isValidAudioFile(contentType)) {
            throw new RuntimeException("Invalid file type. Only MP3, WAV, and FLAC are allowed.");
        }

        // Check file size (50MB limit)
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("File size exceeds 50MB limit");
        }

        // Save file
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadDir = Paths.get(uploadPath);

        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create submission
        Submission submission = Submission.builder()
                .artistId(artistId)
                .title(request.getTitle())
                .artistName(request.getArtistName())
                .genre(request.getGenre())
                .subGenre(request.getSubGenre())
                .bpm(request.getBpm())
                .keySignature(request.getKeySignature())
                .filePath(fileName)
                .fileSizeBytes(file.getSize())
                .description(request.getDescription())
                .lyrics(request.getLyrics())
                .isPublished(false)
                .submissionStatus(Submission.SubmissionStatus.PENDING)
                .playCount(0)
                .totalRatings(0)
                .build();

        submission = submissionRepository.save(submission);

        // Update artist profile submission count
        artistProfileRepository.findByUserId(artistId).ifPresent(profile -> {
            profile.setTotalSubmissions(profile.getTotalSubmissions() + 1);
            artistProfileRepository.save(profile);
        });

        return mapToDto(submission);
    }

    public Page<SubmissionDto> getArtistSubmissions(Long artistId, Pageable pageable) {
        return submissionRepository.findByArtistId(artistId, pageable)
                .map(this::mapToDto);
    }

    public SubmissionDto getSubmission(Long submissionId, Long artistId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (!submission.getArtistId().equals(artistId)) {
            throw new RuntimeException("Unauthorized access to submission");
        }

        return mapToDto(submission);
    }

    @Transactional
    public void deleteSubmission(Long submissionId, Long artistId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (!submission.getArtistId().equals(artistId)) {
            throw new RuntimeException("Unauthorized access to submission");
        }

        // Delete file
        try {
            Path filePath = Paths.get(uploadPath).resolve(submission.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but continue with deletion
        }

        submissionRepository.delete(submission);

        // Update artist profile submission count
        artistProfileRepository.findByUserId(artistId).ifPresent(profile -> {
            profile.setTotalSubmissions(Math.max(0, profile.getTotalSubmissions() - 1));
            artistProfileRepository.save(profile);
        });
    }

    private boolean isValidAudioFile(String contentType) {
        return contentType.equals("audio/mpeg") ||
                contentType.equals("audio/wav") ||
                contentType.equals("audio/x-wav") ||
                contentType.equals("audio/flac") ||
                contentType.equals("audio/x-flac");
    }

    private SubmissionDto mapToDto(Submission submission) {
        return SubmissionDto.builder()
                .id(submission.getId())
                .artistId(submission.getArtistId())
                .title(submission.getTitle())
                .artistName(submission.getArtistName())
                .genre(submission.getGenre())
                .subGenre(submission.getSubGenre())
                .bpm(submission.getBpm())
                .keySignature(submission.getKeySignature())
                .filePath(submission.getFilePath())
                .fileSizeBytes(submission.getFileSizeBytes())
                .durationSeconds(submission.getDurationSeconds())
                .description(submission.getDescription())
                .lyrics(submission.getLyrics())
                .isPublished(submission.getIsPublished())
                .submissionStatus(submission.getSubmissionStatus().name())
                .playCount(submission.getPlayCount())
                .averageRating(submission.getAverageRating())
                .totalRatings(submission.getTotalRatings())
                .createdAt(submission.getCreatedAt())
                .updatedAt(submission.getUpdatedAt())
                .build();
    }
}
