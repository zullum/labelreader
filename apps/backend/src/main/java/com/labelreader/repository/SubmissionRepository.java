package com.labelreader.repository;

import com.labelreader.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Page<Submission> findByArtistId(Long artistId, Pageable pageable);

    List<Submission> findByArtistId(Long artistId);

    Page<Submission> findBySubmissionStatus(Submission.SubmissionStatus status, Pageable pageable);

    @Query(value = "SELECT * FROM submissions WHERE " +
           "MATCH(title, artist_name, description) AGAINST (:query IN NATURAL LANGUAGE MODE) " +
           "AND (:genre IS NULL OR genre = :genre) " +
           "AND (:minBpm IS NULL OR bpm >= :minBpm) " +
           "AND (:maxBpm IS NULL OR bpm <= :maxBpm) " +
           "AND (:startDate IS NULL OR created_at >= :startDate) " +
           "AND (:endDate IS NULL OR created_at <= :endDate) " +
           "AND submission_status = 'APPROVED'",
           nativeQuery = true)
    Page<Submission> searchSubmissions(
            @Param("query") String query,
            @Param("genre") String genre,
            @Param("minBpm") Integer minBpm,
            @Param("maxBpm") Integer maxBpm,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE " +
           "(:genres IS NULL OR s.genre IN :genres) " +
           "AND (:minBpm IS NULL OR s.bpm >= :minBpm) " +
           "AND (:maxBpm IS NULL OR s.bpm <= :maxBpm) " +
           "AND (:minRating IS NULL OR s.averageRating >= :minRating) " +
           "AND s.submissionStatus = 'APPROVED'")
    Page<Submission> findByFilters(
            @Param("genres") java.util.List<String> genres,
            @Param("minBpm") Integer minBpm,
            @Param("maxBpm") Integer maxBpm,
            @Param("minRating") Double minRating,
            Pageable pageable);

    @Query("SELECT s.genre, COUNT(s) FROM Submission s WHERE s.genre IS NOT NULL GROUP BY s.genre")
    List<Object[]> countByGenre();

    @Query("SELECT s FROM Submission s WHERE s.submissionStatus = 'APPROVED' ORDER BY s.averageRating DESC, s.totalRatings DESC")
    List<Submission> findTopRatedSubmissions(Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.submissionStatus = 'APPROVED' ORDER BY s.playCount DESC")
    List<Submission> findMostPlayedSubmissions(Pageable pageable);
}
