package com.labelreader.repository;

import com.labelreader.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findBySubmissionIdAndLabelId(Long submissionId, Long labelId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.submissionId = :submissionId")
    Double calculateAverageRating(Long submissionId);

    Integer countBySubmissionId(Long submissionId);
}
