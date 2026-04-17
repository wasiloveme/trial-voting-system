package com.votingsystem.services;

import com.votingsystem.models.VoterWhitelist;
import com.votingsystem.repositories.VoterWhitelistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhitelistService {

    private final VoterWhitelistRepository whitelistRepository;
    private final AuditLogService auditLogService;

    public List<VoterWhitelist> getEntries(Boolean isRegistered) {
        // Stub implementation
        return whitelistRepository.findAll();
    }

    @Transactional
    public void addEntry(String studentId, String adminIdStr, String clientIp) {
        if (whitelistRepository.existsByStudentId(studentId)) {
            throw new RuntimeException("Student ID already exists in whitelist.");
        }

        VoterWhitelist entry = new VoterWhitelist();
        entry.setStudentId(studentId);
        entry.setRegistered(false);

        whitelistRepository.save(entry);

        Long adminId = Long.parseLong(adminIdStr);
        auditLogService.log(
                adminId, "ADMIN", null,
                "WHITELIST_ADD", "VoterWhitelist", entry.getId(),
                "Added student ID " + studentId + " to whitelist", clientIp
        );
    }

    @Transactional
    public void bulkImportFromCsv(MultipartFile file, String adminIdStr, String clientIp) {
        // Stub implementation for CSV parsing
        Long adminId = Long.parseLong(adminIdStr);
        auditLogService.log(
                adminId, "ADMIN", null,
                "WHITELIST_BULK_IMPORT", "VoterWhitelist", null,
                "Bulk imported student IDs to whitelist", clientIp
        );
    }
}
