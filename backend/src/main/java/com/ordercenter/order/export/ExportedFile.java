package com.ordercenter.order.export;

/**
 * 导出文件的内存表示，由 Controller 写入 HTTP 响应。
 */
public record ExportedFile(String fileName, byte[] content) {
}
