package com.industrial.mdm.modules.importtask.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.modules.importtask.domain.ImportRowResult;
import com.industrial.mdm.modules.product.dto.ProductSpecItemPayload;
import com.industrial.mdm.modules.product.dto.ProductUpsertRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

@Service
public class ImportSheetParser {

    private static final Set<String> REQUIRED_FIELDS =
            Set.of("nameZh", "model", "category", "mainImage", "summaryZh", "hsCode", "origin", "unit");

    private static final Map<String, String> HEADER_ALIASES = createHeaderAliases();

    private static final Map<String, String> UNIT_ALIASES =
            Map.ofEntries(
                    Map.entry("件", "piece"),
                    Map.entry("piece", "piece"),
                    Map.entry("pcs", "piece"),
                    Map.entry("台", "unit"),
                    Map.entry("unit", "unit"),
                    Map.entry("套", "set"),
                    Map.entry("set", "set"),
                    Map.entry("kg", "kg"),
                    Map.entry("千克", "kg"),
                    Map.entry("m", "m"),
                    Map.entry("米", "m"),
                    Map.entry("m2", "m2"),
                    Map.entry("㎡", "m2"),
                    Map.entry("m²", "m2"),
                    Map.entry("平方米", "m2"),
                    Map.entry("m3", "m3"),
                    Map.entry("m³", "m3"),
                    Map.entry("立方米", "m3"));

    private final DataFormatter dataFormatter = new DataFormatter();

    public List<ParsedImportRow> parse(Path filePath, String extension, List<String> allowedCategories) {
        Set<String> categorySet =
                allowedCategories.stream()
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        try {
            List<ParsedImportRow> rows =
                    switch (normalizeExtension(extension)) {
                        case ".xlsx", ".xls" -> parseWorkbook(filePath, categorySet);
                        case ".csv" -> parseCsv(filePath, categorySet);
                        default -> throw new BizException(
                                ErrorCode.INVALID_REQUEST,
                                "unsupported import file extension");
                    };
            if (rows.isEmpty()) {
                throw new BizException(ErrorCode.INVALID_REQUEST, "import file has no data rows");
            }
            return rows;
        } catch (IOException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "failed to parse import file");
        }
    }

    private List<ParsedImportRow> parseWorkbook(Path filePath, Set<String> allowedCategories)
            throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath);
                Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BizException(ErrorCode.INVALID_REQUEST, "import workbook is empty");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new BizException(ErrorCode.INVALID_REQUEST, "import workbook header is missing");
            }

            Map<Integer, String> headerMap = readHeaderMap(headerRow);
            List<ParsedImportRow> rows = new ArrayList<>();
            for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
                    Cell cell = row.getCell(entry.getKey(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.put(entry.getValue(), formatCell(cell));
                }
                rows.add(buildParsedRow(index + 1, values, allowedCategories));
            }
            return rows;
        }
    }

    private List<ParsedImportRow> parseCsv(Path filePath, Set<String> allowedCategories)
            throws IOException {
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "import csv is empty");
        }
        Map<Integer, String> headerMap = readHeaderMap(parseCsvLine(lines.getFirst()));
        List<ParsedImportRow> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            List<String> columns = parseCsvLine(line);
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
                String value = entry.getKey() < columns.size() ? columns.get(entry.getKey()) : "";
                values.put(entry.getValue(), value.trim());
            }
            rows.add(buildParsedRow(index + 1, values, allowedCategories));
        }
        return rows;
    }

    private Map<Integer, String> readHeaderMap(Row headerRow) {
        Map<Integer, String> headerMap = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String header = canonicalizeHeader(formatCell(cell));
            String field = HEADER_ALIASES.get(header);
            if (field != null && !headerMap.containsValue(field)) {
                headerMap.put(cell.getColumnIndex(), field);
            }
        }
        if (!headerMap.values().containsAll(REQUIRED_FIELDS)) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "import header is incomplete");
        }
        return headerMap;
    }

    private Map<Integer, String> readHeaderMap(List<String> headerColumns) {
        Map<Integer, String> headerMap = new LinkedHashMap<>();
        for (int index = 0; index < headerColumns.size(); index++) {
            String header = canonicalizeHeader(headerColumns.get(index));
            String field = HEADER_ALIASES.get(header);
            if (field != null && !headerMap.containsValue(field)) {
                headerMap.put(index, field);
            }
        }
        if (!headerMap.values().containsAll(REQUIRED_FIELDS)) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "import header is incomplete");
        }
        return headerMap;
    }

    private ParsedImportRow buildParsedRow(
            int rowNo, Map<String, String> values, Set<String> allowedCategories) {
        String nameZh = valueOf(values, "nameZh");
        String model = valueOf(values, "model");
        String category = resolveCategory(valueOf(values, "category"), allowedCategories);
        String mainImage = valueOf(values, "mainImage");
        String summaryZh = valueOf(values, "summaryZh");
        String hsCode = valueOf(values, "hsCode");
        String origin = valueOf(values, "origin");
        String unit = normalizeUnit(valueOf(values, "unit"));

        List<String> reasons = new ArrayList<>();
        validateRequired(nameZh, "产品名称（中文）", reasons);
        validateRequired(model, "产品型号", reasons);
        validateRequired(category, "工业类目", reasons);
        validateRequired(mainImage, "产品主图", reasons);
        validateRequired(summaryZh, "产品简介（中文）", reasons);
        validateRequired(hsCode, "HS Code", reasons);
        validateRequired(origin, "原产地", reasons);
        validateRequired(unit, "计量单位", reasons);
        validateCategory(values.get("category"), category, allowedCategories, reasons);

        String price = blankToNull(valueOf(values, "price"));
        if (price != null) {
            try {
                new java.math.BigDecimal(price);
            } catch (NumberFormatException exception) {
                reasons.add("参考单价格式不正确");
            }
        }

        Integer sortOrder = parseOptionalInteger(valueOf(values, "sortOrder"), reasons, "排序值");

        ProductUpsertRequest payload = null;
        if (reasons.isEmpty()) {
            payload =
                    new ProductUpsertRequest(
                            nameZh,
                            blankToNull(valueOf(values, "nameEn")),
                            model,
                            blankToNull(valueOf(values, "brand")),
                            category,
                            mainImage,
                            splitMultiValue(valueOf(values, "gallery")),
                            summaryZh,
                            blankToNull(valueOf(values, "summaryEn")),
                            hsCode,
                            blankToNull(valueOf(values, "hsName")),
                            origin,
                            unit,
                            price,
                            blankToNull(valueOf(values, "currency")),
                            blankToNull(valueOf(values, "packaging")),
                            blankToNull(valueOf(values, "moq")),
                            blankToNull(valueOf(values, "material")),
                            blankToNull(valueOf(values, "size")),
                            blankToNull(valueOf(values, "weight")),
                            blankToNull(valueOf(values, "color")),
                            parseSpecs(valueOf(values, "specs")),
                            splitMultiValue(valueOf(values, "certifications")),
                            splitMultiValue(valueOf(values, "attachments")),
                            parseDisplayPublic(valueOf(values, "displayPublic")),
                            sortOrder == null ? rowNo * 10 : sortOrder);
        }

        return new ParsedImportRow(
                rowNo,
                blankToNull(nameZh) == null ? "未填写产品名称" : nameZh,
                blankToNull(model) == null ? "--" : model,
                reasons.isEmpty() ? ImportRowResult.PASSED : ImportRowResult.FAILED,
                reasons.isEmpty() ? "校验通过" : String.join("；", reasons),
                payload);
    }

    private void validateRequired(String value, String label, List<String> reasons) {
        if (blankToNull(value) == null) {
            reasons.add(label + "不能为空");
        }
    }

    private void validateCategory(
            String rawCategory, String resolvedCategory, Set<String> allowedCategories, List<String> reasons) {
        String normalized = blankToNull(rawCategory);
        if (normalized == null) {
            return;
        }
        if (blankToNull(resolvedCategory) == null || !allowedCategories.contains(resolvedCategory)) {
            reasons.add("工业类目不在当前启用类目中");
        }
    }

    private String resolveCategory(String rawCategory, Set<String> allowedCategories) {
        String normalized = blankToNull(rawCategory);
        if (normalized == null) {
            return "";
        }
        if (allowedCategories.contains(normalized)) {
            return normalized;
        }
        List<String> matched =
                allowedCategories.stream()
                        .filter(
                                item ->
                                        item.equalsIgnoreCase(normalized)
                                                || item.endsWith(" / " + normalized))
                        .toList();
        if (matched.size() == 1) {
            return matched.getFirst();
        }
        return normalized;
    }

    private String normalizeUnit(String rawUnit) {
        String normalized = blankToNull(rawUnit);
        if (normalized == null) {
            return "";
        }
        return UNIT_ALIASES.getOrDefault(normalized.toLowerCase(Locale.ROOT), normalized);
    }

    private Integer parseOptionalInteger(String rawValue, List<String> reasons, String label) {
        String normalized = blankToNull(rawValue);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            reasons.add(label + "必须为整数");
            return null;
        }
    }

    private List<ProductSpecItemPayload> parseSpecs(String rawValue) {
        List<String> items = splitMultiValue(rawValue);
        List<ProductSpecItemPayload> specs = new ArrayList<>();
        for (String item : items) {
            String normalized = item.trim();
            if (normalized.isBlank()) {
                continue;
            }
            String[] nameAndRest = normalized.split("[:=]", 2);
            if (nameAndRest.length < 2) {
                specs.add(new ProductSpecItemPayload(UUID.randomUUID().toString(), normalized, "", ""));
                continue;
            }
            String[] valueAndUnit = nameAndRest[1].trim().split("\\|", 2);
            specs.add(
                    new ProductSpecItemPayload(
                            UUID.randomUUID().toString(),
                            nameAndRest[0].trim(),
                            valueAndUnit[0].trim(),
                            valueAndUnit.length > 1 ? valueAndUnit[1].trim() : ""));
        }
        return specs;
    }

    private boolean parseDisplayPublic(String rawValue) {
        String normalized = blankToNull(rawValue);
        if (normalized == null) {
            return true;
        }
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "false", "0", "no", "n", "否", "关闭" -> false;
            default -> true;
        };
    }

    private List<String> splitMultiValue(String rawValue) {
        String normalized = blankToNull(rawValue);
        if (normalized == null) {
            return List.of();
        }
        return List.of(normalized.split("[;,；，、\\n\\r]+")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (!formatCell(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String formatCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (character == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        values.add(current.toString().trim());
        return values;
    }

    private String valueOf(Map<String, String> values, String key) {
        return values.getOrDefault(key, "").trim();
    }

    private String normalizeExtension(String extension) {
        return extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String canonicalizeHeader(String header) {
        String normalized =
                header == null
                        ? ""
                        : header.replace("\uFEFF", "")
                                .replace("（", "(")
                                .replace("）", ")")
                                .replace("㎡", "m2")
                                .replace("m²", "m2")
                                .replace("m³", "m3")
                                .toLowerCase(Locale.ROOT)
                                .trim();
        return normalized.replaceAll("[\\s_\\-/]+", "");
    }

    private static Map<String, String> createHeaderAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        registerAliases(aliases, "nameZh", "产品名称(中文)", "产品名称中文", "namezh", "productnamezh");
        registerAliases(aliases, "nameEn", "产品名称(英文)", "产品名称英文", "nameen", "productnameen");
        registerAliases(aliases, "model", "产品型号", "型号", "model");
        registerAliases(aliases, "brand", "品牌", "brand");
        registerAliases(aliases, "category", "工业类目", "产品类目", "类目", "category");
        registerAliases(aliases, "mainImage", "产品主图", "主图", "mainimage", "mainimageurl");
        registerAliases(aliases, "gallery", "产品图册", "图册", "gallery", "images");
        registerAliases(aliases, "summaryZh", "产品简介(中文)", "中文简介", "summaryzh", "descriptionzh");
        registerAliases(aliases, "summaryEn", "产品简介(英文)", "英文简介", "summaryen", "descriptionen");
        registerAliases(aliases, "hsCode", "hscode", "hscode编码", "商品编码", "海关编码");
        registerAliases(aliases, "hsName", "hs名称", "hscode名称", "hsname");
        registerAliases(aliases, "origin", "原产地", "origin");
        registerAliases(aliases, "unit", "计量单位", "单位", "unit");
        registerAliases(aliases, "price", "参考单价", "单价", "price");
        registerAliases(aliases, "currency", "币种", "currency");
        registerAliases(aliases, "packaging", "包装方式", "packaging");
        registerAliases(aliases, "moq", "最小起订量moq", "最小起订量", "moq");
        registerAliases(aliases, "material", "材质", "material");
        registerAliases(aliases, "size", "尺寸", "size");
        registerAliases(aliases, "weight", "重量", "weight");
        registerAliases(aliases, "color", "颜色", "color");
        registerAliases(aliases, "certifications", "认证资质", "认证", "certifications");
        registerAliases(aliases, "attachments", "附件资料", "产品资料", "附件", "attachments");
        registerAliases(aliases, "displayPublic", "是否公开展示", "公开展示", "displaypublic");
        registerAliases(aliases, "sortOrder", "排序值", "排序", "sortorder");
        registerAliases(aliases, "specs", "规格参数", "specifications", "specs");
        return aliases;
    }

    private static void registerAliases(
            Map<String, String> aliases, String target, String... sourceHeaders) {
        for (String sourceHeader : sourceHeaders) {
            String normalized =
                    sourceHeader.replace("（", "(")
                            .replace("）", ")")
                            .replace("㎡", "m2")
                            .replace("m²", "m2")
                            .replace("m³", "m3")
                            .toLowerCase(Locale.ROOT)
                            .replaceAll("[\\s_\\-/]+", "");
            aliases.put(normalized, target);
        }
    }

    public record ParsedImportRow(
            int rowNo,
            String productName,
            String model,
            ImportRowResult validationResult,
            String reason,
            ProductUpsertRequest payload) {}
}
