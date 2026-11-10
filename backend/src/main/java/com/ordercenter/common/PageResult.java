package com.ordercenter.common;

import java.util.List;

/**
 * 与前端约定好的分页结构，page 从 1 开始。
 */
public record PageResult<T>(List<T> records, long total, int page, int size) {
}
