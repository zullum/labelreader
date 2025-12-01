package com.labelreader.repository;

import com.labelreader.entity.LabelProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabelProfileRepository extends JpaRepository<LabelProfile, Long> {
    Optional<LabelProfile> findByUserId(Long userId);
}
