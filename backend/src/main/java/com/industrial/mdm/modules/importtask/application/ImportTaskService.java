package com.industrial.mdm.modules.importtask.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.modules.category.application.CategoryService;
import com.industrial.mdm.modules.file.application.FileService;
import com.industrial.mdm.modules.file.repository.StoredFileEntity;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.importtask.domain.ImportMode;
import com.industrial.mdm.modules.importtask.domain.ImportRowResult;
import com.industrial.mdm.modules.importtask.domain.ImportTaskStatus;
import com.industrial.mdm.modules.importtask.dto.ImportTaskCreateRequest;
import com.industrial.mdm.modules.importtask.dto.ImportTaskResponse;
import com.industrial.mdm.modules.importtask.dto.ImportTaskRowResponse;
import com.industrial.mdm.modules.importtask.dto.ImportTemplateResponse;
import com.industrial.mdm.modules.importtask.repository.ImportTaskEntity;
import com.industrial.mdm.modules.importtask.repository.ImportTaskRepository;
import com.industrial.mdm.modules.importtask.repository.ImportTaskRowEntity;
import com.industrial.mdm.modules.importtask.repository.ImportTaskRowRepository;
import com.industrial.mdm.modules.message.application.MessageService;
import com.industrial.mdm.modules.message.domain.MessageType;
import com.industrial.mdm.modules.product.application.ProductService;
import com.industrial.mdm.modules.product.dto.ProductUpsertRequest;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportTaskService {

    private final ImportTaskRepository importTaskRepository;
    private final ImportTaskRowRepository importTaskRowRepository;
    private final FileService fileService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final MessageService messageService;
    private final ImportSheetParser importSheetParser;
    private final ObjectMapper objectMapper;
    private final AuthorizationService authorizationService;

    public ImportTaskService(
            ImportTaskRepository importTaskRepository,
            ImportTaskRowRepository importTaskRowRepository,
            FileService fileService,
            CategoryService categoryService,
            ProductService productService,
            MessageService messageService,
            ImportSheetParser importSheetParser,
            ObjectMapper objectMapper,
            AuthorizationService authorizationService) {
        this.importTaskRepository = importTaskRepository;
        this.importTaskRowRepository = importTaskRowRepository;
        this.fileService = fileService;
        this.categoryService = categoryService;
        this.productService = productService;
        this.messageService = messageService;
        this.importSheetParser = importSheetParser;
        this.objectMapper = objectMapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public ImportTemplateResponse getTemplate() {
        return new ImportTemplateResponse(
                "enterprise-product-import-template.xlsx",
                List.of(
                        "产品名称（中文）",
                        "产品型号",
                        "工业类目",
                        "产品主图",
                        "产品简介（中文）",
                        "HS Code",
                        "原产地",
                        "计量单位"),
                List.of(
                        "产品名称（英文）",
                        "品牌",
                        "产品图册",
                        "产品简介（英文）",
                        "HS 名称",
                        "参考单价",
                        "币种",
                        "包装方式",
                        "MOQ",
                        "材质",
                        "尺寸",
                        "重量",
                        "颜色",
                        "认证资质",
                        "规格参数",
                        "附件资料",
                        "是否公开展示",
                        "排序值"),
                List.of(
                        "当前开发阶段已支持真实 .xlsx / .csv 内容解析，不再依赖文件名演示逻辑。",
                        "规格参数列支持使用“参数名=参数值|单位”格式，多个参数用分号分隔。",
                        "认证资质、图册、附件等多值列可使用英文逗号、分号、顿号或换行分隔。",
                        "工业类目必须填写为当前启用的叶子类目，系统会按导入文件逐行校验。"));
    }

    @Transactional
    public ImportTaskResponse createValidationTask(
            AuthenticatedUser currentUser, ImportTaskCreateRequest request) {
        UUID enterpriseId =
                requireEnterprisePermission(
                        currentUser,
                        PermissionCode.IMPORT_TASK_CREATE,
                        "current account cannot manage import task");
        productService.ensureEnterpriseCanManageProducts(enterpriseId);
        StoredFileEntity sourceFile =
                fileService.loadAuthorizedFile(request.sourceFileId(), currentUser);
        ImportMode mode = parseMode(request.mode());

        List<ImportTaskRowEntity> rows =
                buildRows(
                        sourceFile,
                        importSheetParser.parse(
                                java.nio.file.Path.of(sourceFile.getStoragePath()),
                                sourceFile.getExtension(),
                                categoryService.listEnabledLeafPathNames()));
        int passedRows =
                (int)
                        rows.stream()
                                .filter(item -> item.getValidationResult() == ImportRowResult.PASSED)
                                .count();
        int failedRows = rows.size() - passedRows;

        ImportTaskEntity task = new ImportTaskEntity();
        task.setEnterpriseId(enterpriseId);
        task.setSourceFileId(sourceFile.getId());
        task.setMode(mode);
        task.setStatus(failedRows > 0 ? ImportTaskStatus.FAILED : ImportTaskStatus.READY);
        task.setTotalRows(rows.size());
        task.setPassedRows(passedRows);
        task.setFailedRows(failedRows);
        task.setImportedRows(0);
        task.setReportMessage(
                failedRows > 0
                        ? "文件校验未通过，请根据错误提示修正后重新上传。"
                        : "文件校验通过，可以确认导入。");
        task.setCreatedBy(currentUser.userId());
        task = importTaskRepository.save(task);

        for (ImportTaskRowEntity row : rows) {
            row.setImportTaskId(task.getId());
        }
        importTaskRowRepository.saveAll(rows);
        return toResponse(task, sourceFile.getOriginalFileName(), rows);
    }

    @Transactional(readOnly = true)
    public ImportTaskResponse getTask(AuthenticatedUser currentUser, UUID taskId) {
        UUID enterpriseId =
                requireEnterprisePermission(
                        currentUser,
                        PermissionCode.IMPORT_TASK_READ,
                        "current account cannot manage import task");
        ImportTaskEntity task = loadTaskForEnterprise(enterpriseId, taskId);
        StoredFileEntity sourceFile =
                fileService.loadAuthorizedFile(task.getSourceFileId(), currentUser);
        List<ImportTaskRowEntity> rows =
                importTaskRowRepository.findByImportTaskIdOrderByRowNoAsc(taskId);
        return toResponse(task, sourceFile.getOriginalFileName(), rows);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadErrorReport(
            AuthenticatedUser currentUser, UUID taskId) {
        UUID enterpriseId =
                requireEnterprisePermission(
                        currentUser,
                        PermissionCode.IMPORT_TASK_READ,
                        "current account cannot manage import task");
        ImportTaskEntity task = loadTaskForEnterprise(enterpriseId, taskId);
        List<ImportTaskRowEntity> rows =
                importTaskRowRepository.findByImportTaskIdOrderByRowNoAsc(taskId);
        List<ImportTaskRowEntity> failedRows =
                rows.stream()
                        .filter(item -> item.getValidationResult() == ImportRowResult.FAILED)
                        .toList();
        if (failedRows.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "import task has no error report");
        }

        StoredFileEntity sourceFile =
                fileService.loadAuthorizedFile(task.getSourceFileId(), currentUser);
        byte[] content = buildErrorReportCsv(failedRows);
        String outputFileName = sourceFile.getOriginalFileName() + "-error-report.csv";
        Resource resource = new ByteArrayResource(content);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(outputFileName).build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(content.length)
                .body(resource);
    }

    @Transactional
    public ImportTaskResponse confirmImport(AuthenticatedUser currentUser, UUID taskId) {
        UUID enterpriseId =
                requireEnterprisePermission(
                        currentUser,
                        PermissionCode.IMPORT_TASK_CONFIRM,
                        "current account cannot manage import task");
        ImportTaskEntity task = loadTaskForEnterprise(enterpriseId, taskId);
        productService.ensureEnterpriseCanManageProducts(task.getEnterpriseId());
        if (task.getStatus() != ImportTaskStatus.READY) {
            throw new BizException(
                    ErrorCode.STATE_CONFLICT, "import task is not ready for confirmation");
        }

        List<ImportTaskRowEntity> rows =
                importTaskRowRepository.findByImportTaskIdOrderByRowNoAsc(taskId);
        List<ProductUpsertRequest> payloads =
                rows.stream()
                        .filter(item -> item.getValidationResult() == ImportRowResult.PASSED)
                        .map(this::readPayload)
                        .toList();

        productService.importProducts(
                task.getEnterpriseId(),
                currentUser.userId(),
                payloads,
                task.getMode() == ImportMode.REVIEW);

        task.setStatus(ImportTaskStatus.DONE);
        task.setImportedRows(payloads.size());
        task.setConfirmedBy(currentUser.userId());
        task.setConfirmedAt(OffsetDateTime.now());
        task.setReportMessage("导入完成，产品已按导入模式处理。");
        task = importTaskRepository.save(task);

        messageService.sendToEnterpriseUsers(
                task.getEnterpriseId(),
                MessageType.SYSTEM,
                "批量导入完成",
                "导入任务已处理完成，系统已按所选模式生成产品数据。",
                "你可以前往产品列表查看本次导入结果，并继续编辑或提交审核。",
                "import_task",
                task.getId());

        StoredFileEntity sourceFile =
                fileService.loadAuthorizedFile(task.getSourceFileId(), currentUser);
        return toResponse(task, sourceFile.getOriginalFileName(), rows);
    }

    private List<ImportTaskRowEntity> buildRows(
            StoredFileEntity sourceFile, List<ImportSheetParser.ParsedImportRow> parsedRows) {
        List<ImportTaskRowEntity> rows = new ArrayList<>();
        for (ImportSheetParser.ParsedImportRow parsedRow : parsedRows) {
            ImportTaskRowEntity row = new ImportTaskRowEntity();
            row.setRowNo(parsedRow.rowNo());
            row.setProductName(parsedRow.productName());
            row.setModel(parsedRow.model());
            row.setValidationResult(parsedRow.validationResult());
            row.setReason(parsedRow.reason());
            if (parsedRow.payload() != null) {
                row.setNormalizedPayloadJson(writePayload(parsedRow.payload()));
            }
            rows.add(row);
        }
        if (rows.isEmpty()) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "import file " + sourceFile.getOriginalFileName() + " has no valid rows");
        }
        return rows;
    }

    private ProductUpsertRequest readPayload(ImportTaskRowEntity row) {
        try {
            return objectMapper.readValue(
                    row.getNormalizedPayloadJson(), new TypeReference<ProductUpsertRequest>() {});
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to parse import row payload");
        }
    }

    private String writePayload(ProductUpsertRequest payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException(
                    ErrorCode.INTERNAL_ERROR, "failed to serialize import row payload");
        }
    }

    private ImportTaskResponse toResponse(
            ImportTaskEntity task, String sourceFileName, List<ImportTaskRowEntity> rows) {
        return new ImportTaskResponse(
                task.getId(),
                task.getSourceFileId(),
                sourceFileName,
                task.getMode().getCode(),
                task.getStatus().getCode(),
                task.getTotalRows(),
                task.getPassedRows(),
                task.getFailedRows(),
                task.getImportedRows(),
                task.getReportMessage(),
                task.getCreatedAt(),
                task.getConfirmedAt(),
                rows.stream()
                        .map(
                                item ->
                                        new ImportTaskRowResponse(
                                                item.getId(),
                                                item.getRowNo(),
                                                item.getProductName(),
                                                item.getModel(),
                                                item.getValidationResult().getCode(),
                                                item.getReason()))
                        .toList());
    }

    private ImportTaskEntity loadTaskForEnterprise(UUID enterpriseId, UUID taskId) {
        ImportTaskEntity task =
                importTaskRepository
                        .findById(taskId)
                        .orElseThrow(
                                () -> new BizException(ErrorCode.NOT_FOUND, "import task not found"));
        if (!task.getEnterpriseId().equals(enterpriseId)) {
            throw new BizException(
                    ErrorCode.FORBIDDEN, "import task does not belong to current enterprise");
        }
        return task;
    }

    private UUID requireEnterprisePermission(
            AuthenticatedUser currentUser, PermissionCode permission, String forbiddenMessage) {
        return authorizationService.assertCurrentEnterprisePermission(
                currentUser, permission, forbiddenMessage);
    }

    private ImportMode parseMode(String mode) {
        if ("draft".equalsIgnoreCase(mode)) {
            return ImportMode.DRAFT;
        }
        if ("review".equalsIgnoreCase(mode)) {
            return ImportMode.REVIEW;
        }
        throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported import mode");
    }

    private byte[] buildErrorReportCsv(List<ImportTaskRowEntity> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("rowNo,productName,model,result,reason").append('\n');
        for (ImportTaskRowEntity row : rows) {
            builder.append(row.getRowNo())
                    .append(',')
                    .append(csvValue(row.getProductName()))
                    .append(',')
                    .append(csvValue(row.getModel()))
                    .append(',')
                    .append(csvValue(row.getValidationResult().getCode()))
                    .append(',')
                    .append(csvValue(row.getReason()))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvValue(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
