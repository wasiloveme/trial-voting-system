package com.votingsystem.services;

import com.votingsystem.models.AuditLog;
import com.votingsystem.models.User;
import com.votingsystem.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * =============================================================================
 * FILE: AuditLogService.java
 * PACKAGE: com.votingsystem.services
 * =============================================================================
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(
        Long       actorId,
        String     actorIdentifier,
        User.Role  actorRole,
        String     action,
        String     targetEntity,
        Long       targetId,
        String     detail,
        String     ipAddress
    ) {
        AuditLog entry = new AuditLog();
        entry.setActorId(actorId);
        entry.setActorIdentifier(actorIdentifier);
        entry.setActorRole(actorRole);
        entry.setAction(action);
        entry.setTargetEntity(targetEntity);
        entry.setTargetId(targetId);
        entry.setDetail(detail);
        entry.setEventTimestamp(LocalDateTime.now());
        entry.setIpAddress(ipAddress);

        auditLogRepository.save(entry);
    }

    public void logSystemAction(
        String action,
        String targetEntity,
        Long   targetId,
        String detail
    ) {
        log(null, "SYSTEM", null, action, targetEntity, targetId, detail, "127.0.0.1");
    }
}
