package com.labelreader.repository;

import com.labelreader.entity.Rating;
import com.labelreader.entity.Submission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findBySubmissionIdAndLabelId(Long submissionId, Long labelId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.submissionId = :submissionId")
    Double calculateAverageRating(Long submissionId);

    Integer countBySubmissionId(Long submissionId);

    Long countByLabelId(Long labelId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.labelId = :labelId")
    Double findAverageRatingByLabelId(@Param("labelId") Long labelId);

    @Query("SELECT s.genre, COUNT(r) FROM Rating r JOIN Submission s ON r.submissionId = s.id WHERE r.labelId = :labelId GROUP BY s.genre")
    List<Object[]> countRatingsByGenreForLabel(@Param("labelId") Long labelId);

    @Query("SELECT s FROM Rating r JOIN Submission s ON r.submissionId = s.id WHERE r.labelId = :labelId ORDER BY r.createdAt DESC")
    List<Submission> findRecentlyRatedSubmissionsByLabel(@Param("labelId") Long labelId, Pageable pageable);
}
