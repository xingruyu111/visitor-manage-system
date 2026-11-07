package com.visitor.common.core;

import java.util.List;

/**
 * 简单分页结果。
 */
public record PageResult<T>(long total, long page, long size, List<T> records) {

    public static <T> PageResult<T> of(long total, long page, long size, List<T> records) {
        return new PageResult<>(total, page, size, records);
    }
}
