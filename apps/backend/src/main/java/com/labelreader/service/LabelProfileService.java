package com.labelreader.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelreader.dto.LabelProfileDto;
import com.labelreader.dto.UpdateLabelProfileRequest;
import com.labelreader.entity.LabelProfile;
import com.labelreader.entity.User;
import com.labelreader.repository.LabelProfileRepository;
import com.labelreader.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelProfileService {

    private final LabelProfileRepository labelProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public LabelProfileDto getProfile(Long userId) {
        LabelProfile profile = labelProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Label profile not found"));

        return mapToDto(profile);
    }

    @Transactional
    public LabelProfileDto updateProfile(Long userId, UpdateLabelProfileRequest request) {
        LabelProfile profile = labelProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Label profile not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update profile
        profile.setLabelName(request.getLabelName());
        profile.setCompanyName(request.getCompanyName());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setCountry(request.getCountry());

        // Convert genres list to JSON string
        if (request.getGenresInterested() != null) {
            try {
                String genresJson = objectMapper.writeValueAsString(request.getGenresInterested());
                profile.setGenresInterested(genresJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing genres", e);
            }
        }

        // Update user info
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }

        labelProfileRepository.save(profile);
        userRepository.save(user);

        return mapToDto(profile);
    }

    private LabelProfileDto mapToDto(LabelProfile profile) {
        List<String> genres = new ArrayList<>();
        if (profile.getGenresInterested() != null && !profile.getGenresInterested().isEmpty()) {
            try {
                genres = objectMapper.readValue(
                        profile.getGenresInterested(),
                        new TypeReference<List<String>>() {
                        });
            } catch (JsonProcessingException e) {
                // Return empty list if parsing fails
            }
        }

        return LabelProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .labelName(profile.getLabelName())
                .companyName(profile.getCompanyName())
                .websiteUrl(profile.getWebsiteUrl())
                .genresInterested(genres)
                .country(profile.getCountry())
                .totalReviews(profile.getTotalReviews())
                .totalSigned(profile.getTotalSigned())
                .build();
    }
}
