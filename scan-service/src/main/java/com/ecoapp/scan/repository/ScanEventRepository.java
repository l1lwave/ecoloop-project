package com.ecoapp.scan.repository;

import com.ecoapp.scan.model.ScanEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanEventRepository extends JpaRepository<ScanEvent, Long> {

    /** Newest first; the id tie-breaker keeps paging stable when two scans share a timestamp. */
    Slice<ScanEvent> findByUserIdOrderByScannedAtDescIdDesc(String userId, Pageable pageable);
}
