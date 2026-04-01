package com.industrial.mdm.modules.baseDictionary.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.modules.baseDictionary.dto.DictItemSaveRequest;
import com.industrial.mdm.modules.baseDictionary.dto.DictTypeResponse;
import com.industrial.mdm.modules.baseDictionary.repository.DictItemEntity;
import com.industrial.mdm.modules.baseDictionary.repository.DictItemRepository;
import com.industrial.mdm.modules.baseDictionary.repository.DictTypeEntity;
import com.industrial.mdm.modules.baseDictionary.repository.DictTypeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BaseDictionaryService {

    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository dictItemRepository;

    public BaseDictionaryService(
            DictTypeRepository dictTypeRepository, DictItemRepository dictItemRepository) {
        this.dictTypeRepository = dictTypeRepository;
        this.dictItemRepository = dictItemRepository;
    }

    @Transactional(readOnly = true)
    public List<DictTypeResponse> listTypes() {
        return dictTypeRepository.findAllByOrderByCreatedAtAscCodeAsc().stream()
                .map(type -> toResponse(type, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DictTypeResponse getType(String typeCode, boolean enabledOnly) {
        DictTypeEntity type = loadType(typeCode);
        List<DictItemEntity> items =
                enabledOnly
                        ? dictItemRepository
                                .findByDictTypeCodeAndEnabledTrueOrderBySortOrderAscNameAsc(
                                        type.getCode())
                        : dictItemRepository.findByDictTypeCodeOrderBySortOrderAscNameAsc(
                                type.getCode());
        return toResponse(type, items);
    }

    @Transactional
    public DictTypeResponse.DictItemResponse createItem(
            String typeCode, DictItemSaveRequest request) {
        DictTypeEntity type = loadEditableType(typeCode);
        String normalizedCode = normalizeCode(request.code());
        validateCodeUniqueness(type.getCode(), normalizedCode, null);

        DictItemEntity entity = new DictItemEntity();
        entity.setDictTypeCode(type.getCode());
        applyItemRequest(entity, request, normalizedCode);
        return toItemResponse(dictItemRepository.save(entity));
    }

    @Transactional
    public DictTypeResponse.DictItemResponse updateItem(
            String typeCode, UUID itemId, DictItemSaveRequest request) {
        DictTypeEntity type = loadEditableType(typeCode);
        DictItemEntity entity = loadItem(type.getCode(), itemId);
        String normalizedCode = normalizeCode(request.code());
        validateCodeUniqueness(type.getCode(), normalizedCode, entity.getId());

        applyItemRequest(entity, request, normalizedCode);
        return toItemResponse(dictItemRepository.save(entity));
    }

    @Transactional
    public void deleteItem(String typeCode, UUID itemId) {
        DictTypeEntity type = loadEditableType(typeCode);
        DictItemEntity entity = loadItem(type.getCode(), itemId);
        dictItemRepository.delete(entity);
    }

    private DictTypeEntity loadType(String typeCode) {
        return dictTypeRepository
                .findByCode(typeCode == null ? null : typeCode.trim())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "dictionary type not found"));
    }

    private DictTypeEntity loadEditableType(String typeCode) {
        DictTypeEntity type = loadType(typeCode);
        if (!type.isEditable()) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "dictionary type is not editable");
        }
        return type;
    }

    private DictItemEntity loadItem(String typeCode, UUID itemId) {
        return dictItemRepository
                .findByIdAndDictTypeCode(itemId, typeCode)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "dictionary item not found"));
    }

    private void validateCodeUniqueness(String typeCode, String code, UUID currentId) {
        dictItemRepository.findByDictTypeCodeAndCodeIgnoreCase(typeCode, code).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new BizException(
                        ErrorCode.STATE_CONFLICT, "dictionary item code already exists");
            }
        });
    }

    private void applyItemRequest(
            DictItemEntity entity, DictItemSaveRequest request, String normalizedCode) {
        entity.setCode(normalizedCode);
        entity.setName(request.name().trim());
        entity.setSortOrder(request.sortOrder());
        entity.setEnabled(Boolean.TRUE.equals(request.enabled()));
    }

    private DictTypeResponse toResponse(DictTypeEntity type, List<DictItemEntity> items) {
        return new DictTypeResponse(
                type.getCode(),
                type.getName(),
                type.getDescription(),
                type.isEditable(),
                items.stream().map(this::toItemResponse).toList());
    }

    private DictTypeResponse.DictItemResponse toItemResponse(DictItemEntity item) {
        return new DictTypeResponse.DictItemResponse(
                item.getId(), item.getCode(), item.getName(), item.getSortOrder(), item.isEnabled());
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "dictionary item code is required");
        }
        return normalized;
    }
}
