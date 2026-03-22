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
                  AND NULLIF(TRIM(REPLACE(REPLACE(pf.category_path, '?', ''), '？', '')), '') IS NOT NULL
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
            int page,
            int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder fromClause =
                new StringBuilder(
                        """
                        FROM products p
                        """
                                + ADMIN_PROFILE_JOIN
                                + """
                        WHERE p.status IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED')
                        """);
        appendAdminProductFilters(fromClause, params, keyword, enterpriseName, category, status, hsFilled);
        return executePagedUuidQuery(
                fromClause, params, page, pageSize, "p.latest_submission_at DESC NULLS LAST, p.id");
    }

    public PageResponse<UUID> findManagementProductIds(
            String keyword,
            String enterpriseName,
            String category,
            String status,
            int page,
            int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder fromClause =
                new StringBuilder(
                        """
                        FROM products p
                        """
                                + ADMIN_PROFILE_JOIN
                                + """
                        WHERE 1 = 1
                        """);
        appendAdminProductFilters(fromClause, params, keyword, enterpriseName, category, status, null);
        return executePagedUuidQuery(
                fromClause, params, page, pageSize, "p.updated_at DESC NULLS LAST, p.id");
    }

    public List<String> findReviewEnterpriseNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT DISTINCT e.name
                FROM products p
                JOIN enterprises e
                  ON e.id = p.enterprise_id
                WHERE p.status IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED')
                ORDER BY e.name
                """,
                new MapSqlParameterSource(),
                String.class);
    }

    public List<String> findReviewCategories() {
        return jdbcTemplate.queryForList(
                """
                SELECT DISTINCT pf.category_path
                FROM products p
                """
                        + ADMIN_PROFILE_JOIN
                        + """
                WHERE p.status IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED')
                  AND NULLIF(TRIM(REPLACE(REPLACE(pf.category_path, '?', ''), '？', '')), '') IS NOT NULL
                ORDER BY pf.category_path
                """,
                new MapSqlParameterSource(),
                String.class);
    }

    public List<String> findManagementEnterpriseNames() {
        return jdbcTemplate.queryForList(
                """
                SELECT DISTINCT e.name
                FROM products p
                JOIN enterprises e
                  ON e.id = p.enterprise_id
                ORDER BY e.name
                """,
                new MapSqlParameterSource(),
                String.class);
    }

    public List<String> findManagementCategories() {
        return jdbcTemplate.queryForList(
                """
                SELECT DISTINCT pf.category_path
                FROM products p
                """
                        + ADMIN_PROFILE_JOIN
                        + """
                WHERE NULLIF(TRIM(REPLACE(REPLACE(pf.category_path, '?', ''), '？', '')), '') IS NOT NULL
                ORDER BY pf.category_path
                """,
                new MapSqlParameterSource(),
                String.class);
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
        Long total =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) " + fromClause, params, Long.class);
        return new PageResponse<>(ids, total == null ? 0L : total, safePage, safeSize);
    }
}
