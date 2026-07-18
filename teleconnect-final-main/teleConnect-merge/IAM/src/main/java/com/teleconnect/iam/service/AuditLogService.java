package com.teleconnect.iam.service;

import com.teleconnect.iam.dto.request.AuditLogFilterDTO;
import com.teleconnect.iam.dto.response.AuditLogResponseDTO;
import com.teleconnect.iam.entity.AuditLog;
import com.teleconnect.iam.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository repo;

    public AuditLogService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(Long userId, String action, String module, String ip) {
        log.info("Audit entry: userId={} action={} module={} ip={}", userId, action, module, ip);
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setIpAddress(ip);
        repo.save(log);
    }

    // -- GET ALL LOGS (with filter and pagination) ------------
    public Page<AuditLogResponseDTO> getAllLogs(AuditLogFilterDTO filter) {
        log.debug("Fetching all audit logs with filter {}", filter);
        List<AuditLogResponseDTO> filtered = applyFilter(repo.findAll(), filter);
        return buildPage(filtered, filter);
    }

    // -- GET LOGS BY USER (with filter and pagination) ---------
    public Page<AuditLogResponseDTO> getLogsByUser(Long userId, AuditLogFilterDTO filter) {
        log.debug("Fetching audit logs for userId={} with filter {}", userId, filter);
        List<AuditLog> userLogs = repo.findAll().stream()
            .filter(l -> userId.equals(l.getUserId()))
            .collect(Collectors.toList());
        List<AuditLogResponseDTO> filtered = applyFilter(userLogs, filter);
        return buildPage(filtered, filter);
    }

    private Page<AuditLogResponseDTO> buildPage(List<AuditLogResponseDTO> logs, AuditLogFilterDTO filter) {
        int page = filter.getPage() == null || filter.getPage() < 0 ? 0 : filter.getPage();
        int size = filter.getSize() == null || filter.getSize() <= 0 ? 20 : filter.getSize();
        int start = page * size;
        if (start >= logs.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), logs.size());
        }
        int end = Math.min(start + size, logs.size());
        return new PageImpl<>(logs.subList(start, end), PageRequest.of(page, size), logs.size());
    }

    private List<AuditLogResponseDTO> applyFilter(List<AuditLog> logs, AuditLogFilterDTO f) {
        return logs.stream()
            .filter(l -> f.getFrom() == null || !l.getTimestamp().isBefore(f.getFrom()))
            .filter(l -> f.getTo() == null || !l.getTimestamp().isAfter(f.getTo()))
            .filter(l -> f.getAction() == null || l.getAction().equalsIgnoreCase(f.getAction()))
            .filter(l -> f.getModule() == null || l.getModule().equalsIgnoreCase(f.getModule()))
            .map(this::toDTO)
            .sorted(Comparator.comparing(AuditLogResponseDTO::getTimestamp).reversed())
            .collect(Collectors.toList());
    }

    private AuditLogResponseDTO toDTO(AuditLog l) {
        AuditLogResponseDTO dto = new AuditLogResponseDTO();
        dto.setAuditId(l.getAuditId());
        dto.setUserId(l.getUserId());
        dto.setAction(l.getAction());
        dto.setModule(l.getModule());
        dto.setIpAddress(l.getIpAddress());
        dto.setTimestamp(l.getTimestamp());
        return dto;
    }
}
