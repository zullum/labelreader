package com.labelreader.repository;

import com.labelreader.entity.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {
    Optional<ArtistProfile> findByUserId(Long userId);
}
