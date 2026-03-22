package com.industrial.mdm.modules.category.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.modules.category.domain.CategoryStatus;
import com.industrial.mdm.modules.category.dto.CategoryLeafOptionsResponse;
import com.industrial.mdm.modules.category.dto.CategoryNodeResponse;
import com.industrial.mdm.modules.category.dto.CategorySaveRequest;
import com.industrial.mdm.modules.category.dto.CategoryTreeResponse;
import com.industrial.mdm.modules.category.repository.CategoryEntity;
import com.industrial.mdm.modules.category.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public CategoryTreeResponse getAdminTree() {
        return new CategoryTreeResponse(buildTree(categoryRepository.findAllByOrderBySortOrderAscNameAsc(), false));
    }

    @Transactional(readOnly = true)
    public CategoryTreeResponse getEnterpriseTree() {
        return new CategoryTreeResponse(buildTree(categoryRepository.findAllByOrderBySortOrderAscNameAsc(), true));
    }

    @Transactional(readOnly = true)
    public CategoryLeafOptionsResponse getLeafOptions() {
        return new CategoryLeafOptionsResponse(listEnabledLeafPathNames());
    }

    @Transactional(readOnly = true)
    public List<String> listEnabledLeafPathNames() {
        List<String> leafPaths = new ArrayList<>();
        collectLeafPaths(buildTree(categoryRepository.findAllByOrderBySortOrderAscNameAsc(), true), leafPaths);
        return leafPaths;
    }

    @Transactional
    public CategoryNodeResponse create(CategorySaveRequest request) {
        validateCodeUniqueness(request.code(), null);
        CategoryEntity entity = new CategoryEntity();
        applyRequest(entity, request);
        refreshMetadata(entity);
        return toNode(entity, Map.of());
    }

    @Transactional
    public CategoryNodeResponse update(UUID categoryId, CategorySaveRequest request) {
        CategoryEntity entity = loadCategory(categoryId);
        validateCodeUniqueness(request.code(), categoryId);
        if (request.parentId() != null && request.parentId().equals(categoryId)) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "category cannot use itself as parent");
        }
        if (request.parentId() != null && isDescendant(categoryId, request.parentId())) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "category cannot move under its descendant");
        }
        applyRequest(entity, request);
        refreshMetadata(entity);
        return toNode(entity, Map.of());
    }

    @Transactional
    public void delete(UUID categoryId) {
        CategoryEntity entity = loadCategory(categoryId);
        deleteRecursively(entity.getId());
    }

    private List<CategoryNodeResponse> buildTree(List<CategoryEntity> source, boolean enabledOnly) {
        Map<UUID, List<CategoryEntity>> childrenMap =
                source.stream()
                        .filter(item -> item.getParentId() != null)
                        .filter(item -> !enabledOnly || item.getStatus().isEnabled())
                        .collect(Collectors.groupingBy(CategoryEntity::getParentId));
        List<CategoryEntity> roots =
                source.stream()
                        .filter(item -> item.getParentId() == null)
                        .filter(item -> !enabledOnly || item.getStatus().isEnabled())
                        .sorted(Comparator.comparingInt(CategoryEntity::getSortOrder).thenComparing(CategoryEntity::getName))
                        .toList();
        return roots.stream().map(item -> toNode(item, childrenMap)).toList();
    }

    private CategoryNodeResponse toNode(
            CategoryEntity entity, Map<UUID, List<CategoryEntity>> childrenMap) {
        List<CategoryNodeResponse> children =
                childrenMap.getOrDefault(entity.getId(), List.of()).stream()
                        .sorted(Comparator.comparingInt(CategoryEntity::getSortOrder).thenComparing(CategoryEntity::getName))
                        .map(child -> toNode(child, childrenMap))
                        .toList();
        return new CategoryNodeResponse(
                entity.getId(),
                entity.getParentId(),
                entity.getName(),
                entity.getCode(),
                entity.getStatus().getCode(),
                entity.getSortOrder(),
                entity.getPathName(),
                children);
    }

    private void applyRequest(CategoryEntity entity, CategorySaveRequest request) {
        entity.setParentId(request.parentId());
        entity.setName(request.name().trim());
        entity.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        entity.setSortOrder(request.sortOrder());
        entity.setStatus(parseStatus(request.status()));
    }

    private CategoryStatus parseStatus(String status) {
        if ("enabled".equalsIgnoreCase(status)) {
            return CategoryStatus.ENABLED;
        }
        if ("disabled".equalsIgnoreCase(status)) {
            return CategoryStatus.DISABLED;
        }
        throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported category status");
    }

    private void validateCodeUniqueness(String code, UUID currentId) {
        categoryRepository.findByCodeIgnoreCase(code.trim()).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new BizException(ErrorCode.STATE_CONFLICT, "category code already exists");
            }
        });
    }

    private CategoryEntity loadCategory(UUID categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "category not found"));
    }

    private boolean isDescendant(UUID categoryId, UUID potentialChildId) {
        CategoryEntity current = loadCategory(potentialChildId);
        while (current.getParentId() != null) {
            if (current.getParentId().equals(categoryId)) {
                return true;
            }
            current = loadCategory(current.getParentId());
        }
        return false;
    }

    private void refreshMetadata(CategoryEntity entity) {
        CategoryEntity parent = entity.getParentId() == null ? null : loadCategory(entity.getParentId());
        entity.setLevelNo(parent == null ? 1 : parent.getLevelNo() + 1);
        entity.setPathName(parent == null ? entity.getName() : parent.getPathName() + " / " + entity.getName());
        entity.setPathCode(parent == null ? entity.getCode() : parent.getPathCode() + " / " + entity.getCode());
        categoryRepository.save(entity);

        List<CategoryEntity> children = categoryRepository.findByParentIdOrderBySortOrderAscNameAsc(entity.getId());
        for (CategoryEntity child : children) {
            refreshMetadata(child);
        }
    }

    private void deleteRecursively(UUID categoryId) {
        List<CategoryEntity> children = categoryRepository.findByParentIdOrderBySortOrderAscNameAsc(categoryId);
        for (CategoryEntity child : children) {
            deleteRecursively(child.getId());
        }
        categoryRepository.deleteById(categoryId);
    }

    private void collectLeafPaths(List<CategoryNodeResponse> nodes, List<String> leafPaths) {
        for (CategoryNodeResponse node : nodes) {
            if (node.children() == null || node.children().isEmpty()) {
                leafPaths.add(node.pathName());
                continue;
            }
            collectLeafPaths(node.children(), leafPaths);
        }
    }
}
