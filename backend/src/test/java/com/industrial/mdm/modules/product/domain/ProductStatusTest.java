package com.industrial.mdm.modules.product.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProductStatusTest {

    @Test
    void publishedProductCanBeEditedSubmittedAndTakenOffline() {
        assertTrue(ProductStatus.PUBLISHED.canEdit());
        assertTrue(ProductStatus.PUBLISHED.canSubmit());
        assertTrue(ProductStatus.PUBLISHED.canOffline());
        assertFalse(ProductStatus.PUBLISHED.canDelete());
    }

    @Test
    void draftProductCanBeDeleted() {
        assertTrue(ProductStatus.DRAFT.canDelete());
        assertTrue(ProductStatus.DRAFT.canEdit());
        assertTrue(ProductStatus.DRAFT.canSubmit());
        assertFalse(ProductStatus.DRAFT.canOffline());
    }
}
