package com.industrial.mdm.modules.enterpriseReview.repository;

import com.industrial.mdm.common.api.PageResponse;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyListQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CompanyListQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<UUID> findReviewCompanyIds(
            String keyword, String industry, String status, int page, int pageSize) {
        return queryCompanyIds(
                keyword,
                industry,
                status,
                page,
                pageSize,
                "e.latest_submission_at DESC NULLS LAST, e.name ASC");
    }

    public PageResponse<UUID> findManagementCompanyIds(
            String keyword, String industry, String status, int page, int pageSize) {
        return queryCompanyIds(
                keyword,
                industry,
                status,
                page,
                pageSize,
                "e.updated_at DESC NULLS LAST, e.name ASC");
    }

    public List<String> findIndustries() {
        return jdbcTemplate.queryForList(
                """
                SELECT DISTINCT p.industry
                FROM enterprises e
                JOIN enterprise_profiles p
                  ON p.id = COALESCE(e.working_profile_id, e.current_profile_id)
                WHERE e.status <> 'UNSUBMITTED'
                  AND NULLIF(TRIM(REPLACE(REPLACE(p.industry, '?', ''), '？', '')), '') IS NOT NULL
                ORDER BY p.industry
                """,
                new MapSqlParameterSource(),
                String.class);
    }

    private PageResponse<UUID> queryCompanyIds(
            String keyword,
            String industry,
            String status,
            int page,
            int pageSize,
            String orderByClause) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(Math.min(pageSize, 100), 1);
        int offset = (safePage - 1) * safeSize;

        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder fromClause =
                new StringBuilder(
                        """
                        FROM enterprises e
                        JOIN enterprise_profiles p
                          ON p.id = COALESCE(e.working_profile_id, e.current_profile_id)
                        WHERE e.status <> 'UNSUBMITTED'
                        """);

        if (keyword != null && !keyword.isBlank()) {
            fromClause.append(" AND (e.name ILIKE :keyword OR p.social_credit_code ILIKE :keyword)");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        if (industry != null && !industry.isBlank() && !"all".equalsIgnoreCase(industry)) {
            fromClause.append(" AND p.industry = :industry");
            params.addValue("industry", industry.trim());
        }
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            fromClause.append(" AND e.status = :status");
            params.addValue("status", EnterpriseStatus.fromCode(status.trim()).name());
        }

        String dataSql =
                "SELECT e.id "
                        + fromClause
                        + " ORDER BY "
                        + orderByClause
                        + " LIMIT :limit OFFSET :offset";
        params.addValue("limit", safeSize);
        params.addValue("offset", offset);

        List<UUID> ids = jdbcTemplate.queryForList(dataSql, params, UUID.class);
        Long total =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) " + fromClause, params, Long.class);
        return new PageResponse<>(ids, total == null ? 0L : total, safePage, safeSize);
    }
}
