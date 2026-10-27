package com.servicehub.service;

import com.servicehub.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PagingTest {
    private static final Set<String> PUBLIC_SORTS = Set.of("id", "title", "price");

    @Test
    void preservesAllowedPageAndOrdering() {
        var requested = PageRequest.of(2, 100, Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id")));
        assertEquals(requested, Paging.safe(requested, PUBLIC_SORTS));
    }

    @Test
    void rejectsPrivateAndNestedFieldsBeforeTheyReachJpa() {
        for (String field : new String[]{"passwordHash", "provider.email", "provider.passwordHash", "missing"}) {
            assertThrows(BadRequestException.class,
                () -> Paging.safe(PageRequest.of(0, 20, Sort.by(field)), PUBLIC_SORTS));
        }
    }

    @Test
    void rejectsUnboundedSizeAndUnsupportedSortModifiers() {
        assertThrows(BadRequestException.class, () -> Paging.safe(PageRequest.of(0, 101), PUBLIC_SORTS));
        assertThrows(BadRequestException.class,
            () -> Paging.safe(PageRequest.of(0, 20, Sort.by(Sort.Order.asc("price").ignoreCase())), PUBLIC_SORTS));
    }

    @Test
    void turnsUnpagedIntoABoundedFirstPage() {
        var page = Paging.safe(Pageable.unpaged(), PUBLIC_SORTS);
        assertTrue(page.isPaged());
        assertEquals(0, page.getPageNumber());
        assertEquals(20, page.getPageSize());
    }
}
