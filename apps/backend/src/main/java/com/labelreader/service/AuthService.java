package com.labelreader.service;

import com.labelreader.dto.AuthResponse;
import com.labelreader.dto.LoginRequest;
import com.labelreader.dto.RegisterRequest;
import com.labelreader.entity.ArtistProfile;
import com.labelreader.entity.LabelProfile;
import com.labelreader.entity.User;
import com.labelreader.repository.ArtistProfileRepository;
import com.labelreader.repository.LabelProfileRepository;
import com.labelreader.repository.UserRepository;
import com.labelreader.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final LabelProfileRepository labelProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .userType(User.UserType.valueOf(request.getUserType()))
                .isVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Create profile based on user type
        if (user.getUserType() == User.UserType.ARTIST) {
            ArtistProfile profile = ArtistProfile.builder()
                    .userId(user.getId())
                    .artistName(request.getArtistName() != null ? request.getArtistName()
                            : user.getFirstName() + " " + user.getLastName())
                    .genre(request.getGenre())
                    .totalSubmissions(0)
                    .totalPlays(0)
                    .build();
            artistProfileRepository.save(profile);
        } else {
            LabelProfile profile = LabelProfile.builder()
                    .userId(user.getId())
                    .labelName(request.getLabelName() != null ? request.getLabelName() : request.getCompanyName())
                    .companyName(request.getCompanyName())
                    .totalReviews(0)
                    .totalSigned(0)
                    .build();
            labelProfileRepository.save(profile);
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getUserType().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .userType(user.getUserType().name())
                        .build())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getUserType().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .userType(user.getUserType().name())
                        .build())
                .build();
    }
}
