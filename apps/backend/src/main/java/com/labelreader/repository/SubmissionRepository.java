package com.labelreader.repository;

import com.labelreader.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Page<Submission> findByArtistId(Long artistId, Pageable pageable);

    Page<Submission> findBySubmissionStatus(Submission.SubmissionStatus status, Pageable pageable);
}
