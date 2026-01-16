package com.labelreader.repository;

import com.labelreader.entity.PlayHistory;
import com.labelreader.entity.Submission;
import com.labelreader.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {

    @Query("SELECT DATE(ph.playedAt) as date, COUNT(ph) as playCount " +
           "FROM PlayHistory ph " +
           "WHERE ph.submission.user = :artist AND ph.playedAt >= :startDate " +
           "GROUP BY DATE(ph.playedAt) " +
           "ORDER BY DATE(ph.playedAt)")
    List<Object[]> countPlaysByDateForArtist(User artist, LocalDateTime startDate);

    @Query("SELECT s, COUNT(ph) as playCount " +
           "FROM PlayHistory ph " +
           "JOIN ph.submission s " +
           "WHERE s.user = :artist " +
           "GROUP BY s " +
           "ORDER BY COUNT(ph) DESC")
    List<Object[]> findTopSubmissionsByArtist(User artist, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(ph) FROM PlayHistory ph WHERE ph.submission = :submission")
    Long countBySubmission(Submission submission);

    @Query("SELECT COUNT(ph) FROM PlayHistory ph WHERE ph.submission.user = :artist")
    Long countTotalPlaysByArtist(User artist);
}
