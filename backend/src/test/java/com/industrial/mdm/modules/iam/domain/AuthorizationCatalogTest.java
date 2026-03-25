package com.industrial.mdm.modules.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.context.ReviewContextType;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AuthorizationCatalogTest {

    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile("^[a-z_]+:[a-z_]+$");

    @Test
    void permissionCodesAreUniqueAndFormatted() {
        var codes = Arrays.stream(PermissionCode.values()).map(PermissionCode::getCode).toList();

        assertThat(codes).doesNotHaveDuplicates();
        assertThat(codes).allMatch(code -> PERMISSION_CODE_PATTERN.matcher(code).matches());
    }

    @Test
    void reviewContextTypesResolveByCode() {
        assertThat(ReviewContextType.fromCode("enterprise_review"))
                .isEqualTo(ReviewContextType.ENTERPRISE_REVIEW);
        assertThat(ReviewContextType.fromCode("product_review"))
                .isEqualTo(ReviewContextType.PRODUCT_REVIEW);
    }

    @Test
    void unknownCatalogValuesAreRejected() {
        assertThatThrownBy(() -> PermissionCode.fromCode("unknown:permission"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DataScopeCode.fromCode("unknown_scope"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CapabilityCode.fromCode("unknown_capability"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReviewContextType.fromCode("unknown_review_context"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
