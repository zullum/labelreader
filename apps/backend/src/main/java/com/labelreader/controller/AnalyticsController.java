package com.labelreader.controller;

import com.labelreader.dto.AnalyticsDto;
import com.labelreader.entity.User;
import com.labelreader.repository.UserRepository;
import com.labelreader.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @GetMapping("/artist")
    public ResponseEntity<AnalyticsDto.ArtistAnalytics> getCurrentArtistAnalytics(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        if (user.getUserType() != User.UserType.ARTIST) {
            return ResponseEntity.badRequest().build();
        }

        AnalyticsDto.ArtistAnalytics analytics = analyticsService.getArtistAnalytics(user, days);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<AnalyticsDto.ArtistAnalytics> getArtistAnalytics(
            @PathVariable Long artistId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("Artist not found"));

        if (artist.getUserType() != User.UserType.ARTIST) {
            return ResponseEntity.badRequest().build();
        }

        AnalyticsDto.ArtistAnalytics analytics = analyticsService.getArtistAnalytics(artist, days);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/label")
    public ResponseEntity<AnalyticsDto.LabelAnalytics> getCurrentLabelAnalytics(
            @AuthenticationPrincipal User user) {

        if (user.getUserType() != User.UserType.LABEL) {
            return ResponseEntity.badRequest().build();
        }

        AnalyticsDto.LabelAnalytics analytics = analyticsService.getLabelAnalytics(user);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/label/{labelId}")
    public ResponseEntity<AnalyticsDto.LabelAnalytics> getLabelAnalytics(
            @PathVariable Long labelId) {

        User label = userRepository.findById(labelId)
                .orElseThrow(() -> new RuntimeException("Label not found"));

        if (label.getUserType() != User.UserType.LABEL) {
            return ResponseEntity.badRequest().build();
        }

        AnalyticsDto.LabelAnalytics analytics = analyticsService.getLabelAnalytics(label);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/platform")
    public ResponseEntity<AnalyticsDto.PlatformAnalytics> getPlatformAnalytics() {
        AnalyticsDto.PlatformAnalytics analytics = analyticsService.getPlatformAnalytics();
        return ResponseEntity.ok(analytics);
    }
}
