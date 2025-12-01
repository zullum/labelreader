package com.labelreader.controller;

import com.labelreader.dto.ArtistProfileDto;
import com.labelreader.dto.ArtistStatsDto;
import com.labelreader.dto.UpdateArtistProfileRequest;
import com.labelreader.service.ArtistProfileService;
import com.labelreader.service.ArtistStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artist")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistProfileService artistProfileService;
    private final ArtistStatsService artistStatsService;

    @GetMapping("/profile")
    public ResponseEntity<ArtistProfileDto> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(artistProfileService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<ArtistProfileDto> updateProfile(
            @Valid @RequestBody UpdateArtistProfileRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(artistProfileService.updateProfile(userId, request));
    }

    @GetMapping("/stats")
    public ResponseEntity<ArtistStatsDto> getStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(artistStatsService.getStats(userId));
    }
}
