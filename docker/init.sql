-- LabelReader Database Schema
-- MySQL 8.2+

CREATE DATABASE IF NOT EXISTS labelreader CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE labelreader;

-- Users table (artists and label representatives)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    user_type ENUM('ARTIST', 'LABEL') NOT NULL,
    profile_image_url VARCHAR(500),
    bio TEXT,
    country VARCHAR(100),
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_user_type (user_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;

-- Artist profiles
CREATE TABLE IF NOT EXISTS artist_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    artist_name VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    website_url VARCHAR(500),
    spotify_url VARCHAR(500),
    instagram_handle VARCHAR(100),
    twitter_handle VARCHAR(100),
    soundcloud_url VARCHAR(500),
    total_submissions INT DEFAULT 0,
    total_plays INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_genre (genre),
    INDEX idx_artist_name (artist_name)
) ENGINE=InnoDB;

-- Label profiles
CREATE TABLE IF NOT EXISTS label_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    label_name VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    website_url VARCHAR(500),
    genres_interested JSON,
    country VARCHAR(100),
    total_reviews INT DEFAULT 0,
    total_signed INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_label_name (label_name),
    INDEX idx_country (country)
) ENGINE=InnoDB;

-- Music submissions
CREATE TABLE IF NOT EXISTS submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artist_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    artist_name VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    sub_genre VARCHAR(100),
    bpm INT,
    key_signature VARCHAR(10),
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    duration_seconds INT,
    waveform_data JSON,
    description TEXT,
    lyrics TEXT,
    release_date DATE,
    is_published BOOLEAN DEFAULT FALSE,
    submission_status ENUM('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    play_count INT DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0.00,
    total_ratings INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (artist_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_artist_id (artist_id),
    INDEX idx_genre (genre),
    INDEX idx_status (submission_status),
    INDEX idx_created_at (created_at),
    INDEX idx_average_rating (average_rating),
    FULLTEXT INDEX ft_title_artist (title, artist_name, description)
) ENGINE=InnoDB;

-- Ratings and reviews
CREATE TABLE IF NOT EXISTS ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    is_interested BOOLEAN DEFAULT FALSE,
    listened_duration_seconds INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_rating (submission_id, label_id),
    INDEX idx_submission_id (submission_id),
    INDEX idx_label_id (label_id),
    INDEX idx_rating (rating),
    INDEX idx_is_interested (is_interested)
) ENGINE=InnoDB;

-- Label interest/signing requests
CREATE TABLE IF NOT EXISTS signing_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    artist_id BIGINT NOT NULL,
    request_status ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'WITHDRAWN') DEFAULT 'PENDING',
    message TEXT,
    contract_terms TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (artist_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_submission_id (submission_id),
    INDEX idx_label_id (label_id),
    INDEX idx_artist_id (artist_id),
    INDEX idx_status (request_status)
) ENGINE=InnoDB;

-- Refresh tokens for JWT
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB;

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type ENUM('NEW_RATING', 'SIGNING_REQUEST', 'REQUEST_RESPONSE', 'SYSTEM') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link_url VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;

-- Play history/analytics
CREATE TABLE IF NOT EXISTS play_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    ip_address VARCHAR(45),
    duration_played_seconds INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_submission_id (submission_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;
