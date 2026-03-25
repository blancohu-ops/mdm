package com.industrial.mdm.modules.product.repository;

import com.industrial.mdm.common.api.PageResponse;
import com.industrial.mdm.modules.product.domain.ProductStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductListQueryRepository {

    private static final String ENTERPRISE_PROFILE_JOIN =
            """
            JOIN product_profiles pf
              ON pf.id = COALESCE(p.working_profile_id, p.current_profile_id)
            """;

    private static final String ADMIN_PROFILE_JOIN =
            """
            JOIN product_profiles pf
              ON pf.id = CASE
                    WHEN p.status IN ('PUBLISHED', 'OFFLINE')
                      THEN COALESCE(p.current_profile_id, p.working_profile_id)
                    ELSE COALESCE(p.working_profile_id, p.current_profile_id)
                  END
            JOIN enterprises e
              ON e.id = p.enterprise_id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProductListQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<UUID> findEnterpriseProductIds(
            UUID enterpriseId, String keyword, String status, String category, int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("enterpriseId", enterpriseId);
        StringBuilder fromClause =
                new StringBuilder(
                        """
                        FROM products p
                        """
                                + ENTERPRISE_PROFILE_JOIN
                                + """
                        WHERE p.enterprise_id = :enterpriseId
                        """);
        appendProductFilters(fromClause, params, keyword, status, category);
        return executePagedUuidQuery(
                fromClause, params, page, pageSize, "p.updated_at DESC NULLS LAST, p.id");
    }

    public List<String> findEnterpriseCategories(UUID enterpriseId) {
        return jdbcTemplate.queryForList(
                """
                SELECT DISTINCT pf.category_path
                FROM products p
                JOIN product_profiles pf
                  ON pf.id = COALESCE(p.working_profile_id, p.current_profile_id)
                WHERE p.enterprise_id = :enterpriseId
                  AND NULLIF(TRIM(REPLACE(pf.category_path, '?', '')), '') IS NOT NULL
                ORDER BY pf.category_path
                """,
                new MapSqlParameterSource().addValue("enterpriseId", enterpriseId),
                String.class);
    }

    public PageResponse<UUID> findReviewProductIds(
            String keyword,
            String enterpriseName,
            String category,
            String status,
            String hsFilled,
            List<UUID> enterpriseIds,
            int page,
            int pageSize) {
        return queryAdminProductIds(
                keyword,
                enterpriseName,
                category,
                status,
                hsFilled,
                enterpriseIds,
                page,
                pageSize,
                "p.status IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED')",
                "p.latest_submission_at DESC NULLS LAST, p.id");
    }

    public PageResponse<UUID> findManagementProductIds(
            String keyword,
            String enterpriseName,
            String category,
            String status,
            List<UUID> enterpriseIds,
            int page,
            int pageSize) {
        return queryAdminProductIds(
                keyword,
                enterpriseName,
                category,
                status,
                null,
                enterpriseIds,
                page,
                pageSize,
                "1 = 1",
                "p.updated_at DESC NULLS LAST, p.id");
    }

    public List<String> findReviewEnterpriseNames(List<UUID> enterpriseIds) {
        return queryDistinctStrings(
                """
                SELECT DISTINCT e.name
                FROM products p
                JOIN enterprises e
                  ON e.id = p.enterprise_id
                WHERE p.status IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED')
                """,
                "e.name",
                enterpriseIds);
    }

    public List<String> findReviewCategories(List<UUID> enterpriseIds) {
        return queryDistinctStrings(
                """
                SELECT DISTINCT pf.category_path
                FROM products p
                """
                        + ADMIN_PROFILE_JOIN
                        + """
                WHERE p.status IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED')
                  AND NULLIF(TRIM(REPLACE(pf.category_path, '?', '')), '') IS NOT NULL
                """,
                "pf.category_path",
                enterpriseIds);
    }

    public List<String> findManagementEnterpriseNames(List<UUID> enterpriseIds) {
        return queryDistinctStrings(
                """
                SELECT DISTINCT e.name
                FROM products p
                JOIN enterprises e
                  ON e.id = p.enterprise_id
                WHERE 1 = 1
                """,
                "e.name",
                enterpriseIds);
    }

    public List<String> findManagementCategories(List<UUID> enterpriseIds) {
        return queryDistinctStrings(
                """
                SELECT DISTINCT pf.category_path
                FROM products p
                """
                        + ADMIN_PROFILE_JOIN
                        + """
                WHERE NULLIF(TRIM(REPLACE(pf.category_path, '?', '')), '') IS NOT NULL
                """,
                "pf.category_path",
                enterpriseIds);
    }

    private PageResponse<UUID> queryAdminProductIds(
            String keyword,
            String enterpriseName,
            String category,
            String status,
            String hsFilled,
            List<UUID> enterpriseIds,
            int page,
            int pageSize,
            String basePredicate,
            String orderByClause) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(Math.min(pageSize, 100), 1);
        if (enterpriseIds != null && enterpriseIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0L, safePage, safeSize);
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder fromClause =
                new StringBuilder(
                        "FROM products p\n"
                                + ADMIN_PROFILE_JOIN
                                + "\nWHERE "
                                + basePredicate
                                + "\n");
        if (enterpriseIds != null) {
            fromClause.append(" AND p.enterprise_id IN (:enterpriseIds)");
            params.addValue("enterpriseIds", enterpriseIds);
        }
        appendAdminProductFilters(fromClause, params, keyword, enterpriseName, category, status, hsFilled);
        return executePagedUuidQuery(fromClause, params, page, pageSize, orderByClause);
    }

    private List<String> queryDistinctStrings(
            String baseSql, String orderByColumn, List<UUID> enterpriseIds) {
        if (enterpriseIds != null && enterpriseIds.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder(baseSql);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (enterpriseIds != null) {
            sql.append(" AND p.enterprise_id IN (:enterpriseIds)");
            params.addValue("enterpriseIds", enterpriseIds);
        }
        sql.append(" ORDER BY ").append(orderByColumn);
        return jdbcTemplate.queryForList(sql.toString(), params, String.class);
    }

    private void appendProductFilters(
            StringBuilder fromClause,
            MapSqlParameterSource params,
            String keyword,
            String status,
            String category) {
        if (keyword != null && !keyword.isBlank()) {
            fromClause.append(
                    " AND (pf.name_zh ILIKE :keyword OR pf.model ILIKE :keyword OR pf.category_path ILIKE :keyword)");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        appendProductStatusAndCategory(fromClause, params, status, category);
    }

    private void appendAdminProductFilters(
            StringBuilder fromClause,
            MapSqlParameterSource params,
            String keyword,
            String enterpriseName,
            String category,
            String status,
            String hsFilled) {
        if (keyword != null && !keyword.isBlank()) {
            fromClause.append(
                    " AND (pf.name_zh ILIKE :keyword OR pf.model ILIKE :keyword OR e.name ILIKE :keyword)");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        if (enterpriseName != null
                && !enterpriseName.isBlank()
                && !"all".equalsIgnoreCase(enterpriseName)) {
            fromClause.append(" AND e.name = :enterpriseName");
            params.addValue("enterpriseName", enterpriseName.trim());
        }
        appendProductStatusAndCategory(fromClause, params, status, category);
        if (hsFilled != null && !hsFilled.isBlank() && !"all".equalsIgnoreCase(hsFilled)) {
            if ("filled".equalsIgnoreCase(hsFilled)) {
                fromClause.append(" AND NULLIF(TRIM(pf.hs_code), '') IS NOT NULL");
            } else if ("empty".equalsIgnoreCase(hsFilled)) {
                fromClause.append(" AND NULLIF(TRIM(pf.hs_code), '') IS NULL");
            }
        }
    }

    private void appendProductStatusAndCategory(
            StringBuilder fromClause,
            MapSqlParameterSource params,
            String status,
            String category) {
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            fromClause.append(" AND p.status = :status");
            params.addValue("status", ProductStatus.fromCode(status.trim()).name());
        }
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            fromClause.append(" AND pf.category_path = :category");
            params.addValue("category", category.trim());
        }
    }

    private PageResponse<UUID> executePagedUuidQuery(
            StringBuilder fromClause,
            MapSqlParameterSource params,
            int page,
            int pageSize,
            String orderByClause) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(Math.min(pageSize, 100), 1);
        int offset = (safePage - 1) * safeSize;

        params.addValue("limit", safeSize);
        params.addValue("offset", offset);

        String dataSql =
                "SELECT p.id "
                        + fromClause
                        + " ORDER BY "
                        + orderByClause
                        + " LIMIT :limit OFFSET :offset";

        List<UUID> ids = jdbcTemplate.queryForList(dataSql, params, UUID.class);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + fromClause, params, Long.class);
        return new PageResponse<>(ids, total == null ? 0L : total, safePage, safeSize);
    }
}
