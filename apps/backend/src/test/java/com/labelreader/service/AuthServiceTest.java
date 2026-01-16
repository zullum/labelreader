package com.labelreader.service;

import com.labelreader.dto.AuthResponse;
import com.labelreader.dto.LoginRequest;
import com.labelreader.dto.RegisterRequest;
import com.labelreader.entity.ArtistProfile;
import com.labelreader.entity.User;
import com.labelreader.repository.ArtistProfileRepository;
import com.labelreader.repository.LabelProfileRepository;
import com.labelreader.repository.UserRepository;
import com.labelreader.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArtistProfileRepository artistProfileRepository;

    @Mock
    private LabelProfileRepository labelProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .userType(User.UserType.ARTIST)
                .isActive(true)
                .isVerified(false)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        registerRequest.setUserType("ARTIST");
        registerRequest.setArtistName("Test Artist");
        registerRequest.setGenre("Electronic");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenReturn(new ArtistProfile());
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refreshToken");

        AuthResponse result = authService.register(registerRequest);

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        assertEquals("test@example.com", result.getUser().getEmail());
        assertEquals("Test", result.getUser().getFirstName());
        verify(userRepository, times(1)).save(any(User.class));
        verify(artistProfileRepository, times(1)).save(any(ArtistProfile.class));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refreshToken");

        AuthResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        assertEquals("test@example.com", result.getUser().getEmail());
        verify(userRepository, times(1)).findByEmail(anyString());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void login_InactiveUser_ThrowsException() {
        testUser.setIsActive(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void register_LabelUser_Success() {
        registerRequest.setUserType("LABEL");
        registerRequest.setLabelName("Test Label");
        registerRequest.setCompanyName("Test Company");
        testUser.setUserType(User.UserType.LABEL);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(labelProfileRepository.save(any())).thenReturn(null);
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refreshToken");

        AuthResponse result = authService.register(registerRequest);

        assertNotNull(result);
        verify(labelProfileRepository, times(1)).save(any());
    }
}
