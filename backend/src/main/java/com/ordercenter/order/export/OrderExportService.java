package com.ordercenter.order.export;

import com.ordercenter.order.Order;
import com.ordercenter.order.OrderRepository;
import com.ordercenter.shipping.ShippingMethodCatalog;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量导出订单。
 * Excel 文件在内存中生成，再由 Controller 以二进制响应返回。
 */
@Service
@Transactional(readOnly = true)
public class OrderExportService {

    public static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String[] HEADERS = {
            "序号", "订单号", "运输方式", "收件国家代码", "客户姓名", "联系电话",
            "商品名称", "数量", "单价", "总金额", "订单状态", "备注", "创建时间", "更新时间"
    };

    private static final int[] COLUMN_WIDTHS = {
            7, 23, 32, 14, 16, 16, 30, 9, 13, 15, 11, 30, 21, 21
    };

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final DateTimeFormatter CELL_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderRepository orderRepository;
    private final ShippingMethodCatalog shippingMethodCatalog;

    public OrderExportService(
            OrderRepository orderRepository,
            ShippingMethodCatalog shippingMethodCatalog
    ) {
        this.orderRepository = orderRepository;
        this.shippingMethodCatalog = shippingMethodCatalog;
    }

    public ExportedFile export(List<Long> ids) {
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<Order> found = orderRepository.findAllById(distinctIds);

        if (found.size() != distinctIds.size()) {
            throw new IllegalArgumentException("部分订单不存在，请刷新后重试");
        }

        Map<Long, Order> byId = found.stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        List<Order> orders = distinctIds.stream()
                .map(byId::get)
                .toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createBodyStyle(workbook, null);
            CellStyle moneyStyle = createBodyStyle(workbook, "#,##0.00");

            writeHeader(sheet, headerStyle);
            writeRows(sheet, orders, textStyle, moneyStyle);

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, orders.size(), 0, HEADERS.length - 1));
            for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            String fileName = "order-export-" + LocalDateTime.now().format(FILE_TIME) + ".xlsx";
            return new ExportedFile(fileName, output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("生成 Excel 文件失败", exception);
        }
    }

    private void writeHeader(Sheet sheet, CellStyle style) {
        Row header = sheet.createRow(0);
        header.setHeightInPoints(24);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeRows(
            Sheet sheet,
            List<Order> orders,
            CellStyle textStyle,
            CellStyle moneyStyle
    ) {
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            Row row = sheet.createRow(i + 1);
            row.setHeightInPoints(21);

            int column = 0;
            writeNumber(row, column++, textStyle, i + 1);
            writeText(row, column++, textStyle, order.getOrderNo());
            writeText(row, column++, textStyle,
                    shippingMethodCatalog.labelOf(order.getShippingMethod()));
            writeText(row, column++, textStyle, order.getRecipientCountry());
            writeText(row, column++, textStyle, order.getCustomerName());
            writeText(row, column++, textStyle, order.getCustomerPhone());
            writeText(row, column++, textStyle, order.getProductName());
            writeNumber(row, column++, textStyle, order.getQuantity());
            writeNumber(row, column++, moneyStyle, order.getUnitPrice());
            writeNumber(row, column++, moneyStyle, order.getTotalAmount());
            writeText(row, column++, textStyle, order.getStatus().getLabel());
            writeText(row, column++, textStyle, order.getRemark());
            writeText(row, column++, textStyle, formatTime(order.getCreatedAt()));
            writeText(row, column, textStyle, formatTime(order.getUpdatedAt()));
        }
    }

    private static void writeText(Row row, int column, CellStyle style, String value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void writeNumber(Row row, int column, CellStyle style, Number value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private static String formatTime(LocalDateTime value) {
        return value == null ? "" : value.format(CELL_TIME);
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorder(style);
        return style;
    }

    private static CellStyle createBodyStyle(Workbook workbook, String numberFormat) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (numberFormat != null) {
            style.setDataFormat(workbook.createDataFormat().getFormat(numberFormat));
        }
        applyBorder(style);
        return style;
    }

    private static void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
