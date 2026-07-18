package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {
    List<InAppNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<InAppNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
