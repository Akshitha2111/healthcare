package com.healthcare.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtil {
    public static final int MAX_PAGE_SIZE = 50;

    private PageUtil() {}

    /** Clamps client supplied paging values so a caller can never request an unbounded result set. */
    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }
}
