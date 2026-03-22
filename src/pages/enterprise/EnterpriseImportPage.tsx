import { useEffect, useState } from "react";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  SectionCard,
  TableCard,
} from "@/components/backoffice/BackofficePrimitives";
import { enterpriseService } from "@/services/enterpriseService";
import type {
  ImportTaskResponse,
  ImportTemplateResponse,
} from "@/services/contracts/backoffice";

type ImportStage = "idle" | "validating" | "failed" | "ready" | "done";

export function EnterpriseImportPage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [mode, setMode] = useState<"draft" | "review">("draft");
  const [stage, setStage] = useState<ImportStage>("idle");
  const [task, setTask] = useState<ImportTaskResponse | null>(null);
  const [template, setTemplate] = useState<ImportTemplateResponse | null>(null);
  const [loadingTemplate, setLoadingTemplate] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    enterpriseService
      .getImportTemplate()
      .then((result) => {
        if (mounted) {
          setTemplate(result.data);
        }
      })
      .catch((serviceError) => {
        if (mounted) {
          setError(serviceError instanceof Error ? serviceError.message : "加载导入模板失败");
        }
      })
      .finally(() => {
        if (mounted) {
          setLoadingTemplate(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const startValidation = async () => {
    if (!selectedFile || working) {
      return;
    }

    setWorking(true);
    setError("");
    setStage("validating");

    try {
      const uploadResult = await enterpriseService.uploadFile(
        selectedFile,
        "import-sheet",
        "private",
      );
      const taskResult = await enterpriseService.createImportTask({
        sourceFileId: uploadResult.data.id,
        mode,
      });
      setTask(taskResult.data);
      setStage(taskResult.data.status);
    } catch (serviceError) {
      setTask(null);
      setStage("idle");
      setError(serviceError instanceof Error ? serviceError.message : "导入校验失败");
    } finally {
      setWorking(false);
    }
  };

  const confirmImport = async () => {
    if (!task || working) {
      return;
    }

    setWorking(true);
    setError("");

    try {
      const result = await enterpriseService.confirmImportTask(task.id);
      setTask(result.data);
      setStage("done");
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "确认导入失败");
    } finally {
      setWorking(false);
    }
  };

  const handleDownloadErrorReport = async () => {
    if (!task || working) {
      return;
    }

    setWorking(true);
    setError("");
    try {
      await enterpriseService.downloadImportErrorReport(task.id, task.sourceFileName);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "下载错误报告失败");
    } finally {
      setWorking(false);
    }
  };

  const resetImport = () => {
    setSelectedFile(null);
    setTask(null);
    setStage("idle");
    setError("");
  };

  return (
    <div className="space-y-8" data-testid="enterprise-import-page">
      <BackofficePageHeader
        eyebrow="E10"
        title="批量导入"
        description="通过固定的 Excel 或 CSV 模板快速导入产品，先校验，再确认导入为草稿或直接提交审核。"
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      <SectionCard title="导入说明">
        <div className="grid gap-4 lg:grid-cols-3">
          <HintCard text="下载模板后按字段说明填写，重点关注主图、类目、HS Code、原产地和计量单位。" />
          <HintCard text="当前开发环境已支持真实的 .xlsx / .csv 内容解析，校验结果会精确到具体行。" />
          <HintCard text="校验通过后，可选择“导入为草稿”或“导入后直接提交审核”两种模式。" />
        </div>

        {loadingTemplate ? (
          <div className="mt-6 text-sm text-ink-muted">正在加载模板说明...</div>
        ) : template ? (
          <div className="mt-6 grid gap-4 lg:grid-cols-3">
            <TemplateList title="必填字段" items={template.requiredColumns} />
            <TemplateList title="选填字段" items={template.optionalColumns} />
            <TemplateList title="填写说明" items={template.notes} />
          </div>
        ) : null}

        <div className="mt-6">
          <BackofficeButton variant="secondary" onClick={() => downloadTemplateCsv(template)}>
            下载导入模板
          </BackofficeButton>
        </div>
      </SectionCard>

      <SectionCard title="上传区域">
        <div className="grid gap-5 lg:grid-cols-[1fr_auto]">
          <div className="rounded-[1.75rem] border border-dashed border-[#dbe5f1] bg-[#f7f9fc] px-5 py-8 text-sm text-ink-muted">
            <input
              type="file"
              accept=".xlsx,.csv"
              onChange={(event) => {
                setSelectedFile(event.target.files?.[0] ?? null);
                setTask(null);
                setStage("idle");
                setError("");
              }}
            />
            <div className="mt-3">当前文件：{selectedFile?.name ?? "未选择文件"}</div>
          </div>
          <div className="space-y-3">
            <label className="flex items-center gap-2 rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm">
              <input type="radio" checked={mode === "draft"} onChange={() => setMode("draft")} />
              导入为草稿
            </label>
            <label className="flex items-center gap-2 rounded-2xl bg-[#f7f9fc] px-4 py-3 text-sm">
              <input type="radio" checked={mode === "review"} onChange={() => setMode("review")} />
              导入后直接提交审核
            </label>
            <BackofficeButton disabled={!selectedFile || working} onClick={startValidation}>
              {working && stage === "validating" ? "校验中..." : "开始校验"}
            </BackofficeButton>
          </div>
        </div>
      </SectionCard>

      {stage === "idle" ? (
        <EmptyState
          title="还没有开始导入"
          description="选择 Excel 或 CSV 文件后点击“开始校验”，系统会先检查字段完整性和格式。"
          icon="upload_file"
        />
      ) : null}

      {stage === "validating" ? (
        <SectionCard title="校验中">
          <div className="rounded-2xl bg-[#f7f9fc] px-5 py-6 text-sm text-ink-muted">
            文件正在校验，请稍候...
          </div>
        </SectionCard>
      ) : null}

      {stage === "failed" ? (
        <div className="space-y-6">
          <EmptyState
            icon="error"
            title="导入校验失败"
            description={task?.reportMessage ?? "文件校验未通过，请根据错误提示修正后重新上传。"}
            actions={
              <>
                <BackofficeButton
                  variant="secondary"
                  onClick={() => void handleDownloadErrorReport()}
                  disabled={!task || working}
                >
                  下载错误报告
                </BackofficeButton>
                <BackofficeButton onClick={resetImport}>重新上传</BackofficeButton>
              </>
            }
          />
          <ImportResultTable rows={task?.rows ?? []} />
        </div>
      ) : null}

      {stage === "ready" ? (
        <TableCard
          title="校验结果"
          actions={
            <div className="flex gap-3">
              <BackofficeButton variant="secondary" onClick={resetImport}>
                重新上传
              </BackofficeButton>
              <BackofficeButton disabled={working} onClick={() => void confirmImport()}>
                {working ? "导入中..." : "确认导入"}
              </BackofficeButton>
            </div>
          }
        >
          <div className="border-b border-[#eef3f9] px-6 py-4 text-sm text-ink-muted">
            {task?.reportMessage}
          </div>
          <ImportResultTable rows={task?.rows ?? []} />
        </TableCard>
      ) : null}

      {stage === "done" ? (
        <EmptyState
          title="导入完成"
          description={
            task
              ? `${task.sourceFileName} 已按“${
                  task.mode === "draft" ? "导入为草稿" : "导入后直接提交审核"
                }”模式处理完成，共导入 ${task.importedRows} 条产品。`
              : "导入任务已完成。"
          }
          icon="task_alt"
          actions={
            <>
              <BackofficeButton to="/enterprise/products">返回产品列表</BackofficeButton>
              <BackofficeButton onClick={resetImport} variant="secondary">
                继续导入
              </BackofficeButton>
            </>
          }
        />
      ) : null}
    </div>
  );
}

function ImportResultTable({ rows }: { rows: ImportTaskResponse["rows"] }) {
  return (
    <table className="min-w-full text-left text-sm">
      <thead className="border-b border-[#eef3f9] text-xs uppercase tracking-[0.18em] text-slate-400">
        <tr>
          <th className="px-6 py-4">行号</th>
          <th className="px-6 py-4">产品名称</th>
          <th className="px-6 py-4">型号</th>
          <th className="px-6 py-4">校验结果</th>
          <th className="px-6 py-4">错误原因</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.id} className="border-b border-[#eef3f9] last:border-b-0">
            <td className="px-6 py-4">{row.rowNo}</td>
            <td className="px-6 py-4">{row.productName}</td>
            <td className="px-6 py-4">{row.model}</td>
            <td className="px-6 py-4">{row.result === "passed" ? "通过" : "失败"}</td>
            <td
              className={
                row.result === "failed"
                  ? "px-6 py-4 text-rose-700"
                  : "px-6 py-4 text-ink-muted"
              }
            >
              {row.reason}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TemplateList({ title, items }: { title: string; items: string[] }) {
  return (
    <div className="rounded-2xl bg-[#f7f9fc] p-5">
      <div className="text-sm font-semibold text-primary-strong">{title}</div>
      <ul className="mt-3 space-y-2 text-sm leading-7 text-ink-muted">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </div>
  );
}

function HintCard({ text }: { text: string }) {
  return <div className="rounded-2xl bg-[#f7f9fc] p-5 text-sm leading-7 text-ink-muted">{text}</div>;
}

function downloadTemplateCsv(template: ImportTemplateResponse | null) {
  if (!template) {
    return;
  }

  const headers = [...template.requiredColumns, ...template.optionalColumns];
  const csv = `${headers.join(",")}\n`;
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = template.templateName.replace(/\.xlsx$/i, ".csv");
  link.click();
  URL.revokeObjectURL(url);
}
