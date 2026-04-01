package com.industrial.mdm.modules.baseDictionary.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.modules.baseDictionary.dto.RegionNodeResponse;
import com.industrial.mdm.modules.baseDictionary.dto.RegionSaveRequest;
import com.industrial.mdm.modules.baseDictionary.dto.RegionUpdateRequest;
import com.industrial.mdm.modules.baseDictionary.repository.AdministrativeRegionEntity;
import com.industrial.mdm.modules.baseDictionary.repository.AdministrativeRegionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministrativeRegionService {

    private final AdministrativeRegionRepository administrativeRegionRepository;

    public AdministrativeRegionService(AdministrativeRegionRepository administrativeRegionRepository) {
        this.administrativeRegionRepository = administrativeRegionRepository;
    }

    @Transactional(readOnly = true)
    public List<RegionNodeResponse> listPublicRegions(Integer level, String parentCode) {
        if (parentCode != null && !parentCode.isBlank() && level != null) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "region query accepts either level or parentCode, not both");
        }

        if (parentCode != null && !parentCode.isBlank()) {
            return administrativeRegionRepository
                    .findByParentCodeAndEnabledTrueOrderBySortOrderAscNameAsc(parentCode.trim())
                    .stream()
                    .map(region -> toNode(region, Map.of()))
                    .toList();
        }

        int resolvedLevel = level == null ? 1 : level;
        return administrativeRegionRepository
                .findByLevelAndEnabledTrueOrderBySortOrderAscNameAsc(resolvedLevel)
                .stream()
                .map(region -> toNode(region, Map.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegionNodeResponse> getAdminTree() {
        return buildTree(administrativeRegionRepository.findAllByOrderByLevelAscSortOrderAscNameAsc());
    }

    @Transactional(readOnly = true)
    public List<RegionNodeResponse> getAdminChildren(String parentCode) {
        return administrativeRegionRepository.findByParentCodeOrderBySortOrderAscNameAsc(parentCode).stream()
                .map(region -> toNode(region, Map.of()))
                .toList();
    }

    @Transactional
    public RegionNodeResponse create(RegionSaveRequest request) {
        String code = normalizeCode(request.code());
        validateCodeUniqueness(code, null);
        validateCreateRequest(code, request);

        AdministrativeRegionEntity entity = new AdministrativeRegionEntity();
        entity.setCode(code);
        entity.setName(request.name().trim());
        entity.setLevel(request.level());
        entity.setParentCode(normalizeParentCode(request.parentCode()));
        entity.setSortOrder(request.sortOrder());
        entity.setEnabled(true);
        return toNode(administrativeRegionRepository.save(entity), Map.of());
    }

    @Transactional
    public RegionNodeResponse update(UUID regionId, RegionUpdateRequest request) {
        AdministrativeRegionEntity entity = loadRegion(regionId);
        entity.setName(request.name().trim());
        entity.setSortOrder(request.sortOrder());
        entity.setEnabled(Boolean.TRUE.equals(request.enabled()));
        return toNode(administrativeRegionRepository.save(entity), Map.of());
    }

    @Transactional
    public void delete(UUID regionId) {
        AdministrativeRegionEntity entity = loadRegion(regionId);
        deleteRecursively(entity.getCode());
    }

    private List<RegionNodeResponse> buildTree(List<AdministrativeRegionEntity> source) {
        Map<String, List<AdministrativeRegionEntity>> childrenMap =
                source.stream()
                        .filter(item -> item.getParentCode() != null)
                        .collect(Collectors.groupingBy(AdministrativeRegionEntity::getParentCode));
        return source.stream()
                .filter(item -> item.getParentCode() == null)
                .sorted(regionComparator())
                .map(region -> toNode(region, childrenMap))
                .toList();
    }

    private RegionNodeResponse toNode(
            AdministrativeRegionEntity entity,
            Map<String, List<AdministrativeRegionEntity>> childrenMap) {
        List<RegionNodeResponse> children =
                childrenMap.getOrDefault(entity.getCode(), List.of()).stream()
                        .sorted(regionComparator())
                        .map(child -> toNode(child, childrenMap))
                        .toList();
        return new RegionNodeResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getLevel(),
                entity.getParentCode(),
                entity.getSortOrder(),
                entity.isEnabled(),
                children);
    }

    private AdministrativeRegionEntity loadRegion(UUID regionId) {
        return administrativeRegionRepository
                .findById(regionId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "administrative region not found"));
    }

    private AdministrativeRegionEntity loadRegionByCode(String code) {
        return administrativeRegionRepository
                .findByCode(code)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "parent administrative region not found"));
    }

    private void validateCreateRequest(String code, RegionSaveRequest request) {
        String parentCode = normalizeParentCode(request.parentCode());
        if (request.level() == 1) {
            if (parentCode != null) {
                throw new BizException(
                        ErrorCode.INVALID_REQUEST, "province-level region cannot have parentCode");
            }
            return;
        }
        if (parentCode == null) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "non-root administrative region requires parentCode");
        }
        if (code.equals(parentCode)) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "administrative region cannot use itself as parent");
        }
        AdministrativeRegionEntity parent = loadRegionByCode(parentCode);
        if (parent.getLevel() + 1 != request.level()) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "administrative region level does not match parent");
        }
    }

    private void validateCodeUniqueness(String code, UUID currentId) {
        administrativeRegionRepository.findByCode(code).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new BizException(
                        ErrorCode.STATE_CONFLICT, "administrative region code already exists");
            }
        });
    }

    private void deleteRecursively(String code) {
        List<AdministrativeRegionEntity> children =
                administrativeRegionRepository.findByParentCodeOrderBySortOrderAscNameAsc(code);
        for (AdministrativeRegionEntity child : children) {
            deleteRecursively(child.getCode());
        }
        administrativeRegionRepository.findByCode(code).ifPresent(administrativeRegionRepository::delete);
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim();
        if (!normalized.matches("\\d{6}")) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "administrative region code must be 6 digits");
        }
        return normalized;
    }

    private String normalizeParentCode(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return null;
        }
        String normalized = parentCode.trim();
        if (!normalized.matches("\\d{6}")) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "administrative region parentCode must be 6 digits");
        }
        return normalized;
    }

    private Comparator<AdministrativeRegionEntity> regionComparator() {
        return Comparator.comparingInt(AdministrativeRegionEntity::getSortOrder)
                .thenComparing(AdministrativeRegionEntity::getName);
    }
}
