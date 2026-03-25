import { useEffect, useMemo, useState } from "react";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import { BackofficeButton } from "@/components/backoffice/BackofficePrimitives";
import {
  downloadAuthenticatedFile,
  fetchAuthenticatedFile,
} from "@/services/utils/apiClient";

type FilePreviewDialogProps = {
  open: boolean;
  title: string;
  description?: string;
  filePath?: string | null;
  suggestedFileName?: string;
  onClose: () => void;
};

type PreviewFileState = {
  objectUrl: string;
  fileName?: string;
  mimeType?: string;
};

export function FilePreviewDialog({
  open,
  title,
  description,
  filePath,
  suggestedFileName,
  onClose,
}: FilePreviewDialogProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [file, setFile] = useState<PreviewFileState | null>(null);

  useEffect(() => {
    if (!open || !filePath) {
      setLoading(false);
      setError("");
      setFile((current) => {
        if (current?.objectUrl) {
          URL.revokeObjectURL(current.objectUrl);
        }
        return null;
      });
      return;
    }

    let active = true;
    let objectUrl = "";
    setLoading(true);
    setError("");
    setFile((current) => {
      if (current?.objectUrl) {
        URL.revokeObjectURL(current.objectUrl);
      }
      return null;
    });

    void fetchAuthenticatedFile(filePath)
      .then((result) => {
        if (!active) {
          return;
        }

        objectUrl = URL.createObjectURL(result.blob);
        setFile({
          objectUrl,
          fileName: suggestedFileName ?? result.fileName,
          mimeType: result.mimeType,
        });
      })
      .catch((serviceError) => {
        if (!active) {
          return;
        }
        setError(serviceError instanceof Error ? serviceError.message : "文件预览加载失败");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [open, filePath, suggestedFileName]);

  const previewType = useMemo(() => {
    if (!filePath) {
      return "none" as const;
    }

    const normalizedPath = filePath.toLowerCase();
    const normalizedFileName = (file?.fileName ?? "").toLowerCase();
    const mimeType = (file?.mimeType ?? "").toLowerCase();

    if (
      mimeType.startsWith("image/") ||
      /\.(png|jpg|jpeg|gif|webp|bmp|svg)$/.test(normalizedPath) ||
      /\.(png|jpg|jpeg|gif|webp|bmp|svg)$/.test(normalizedFileName)
    ) {
      return "image" as const;
    }

    if (
      mimeType.includes("pdf") ||
      normalizedPath.endsWith(".pdf") ||
      normalizedFileName.endsWith(".pdf")
    ) {
      return "pdf" as const;
    }

    return "unsupported" as const;
  }, [file?.fileName, file?.mimeType, filePath]);

  return (
    <Dialog
      open={open}
      title={title}
      description={description}
      onClose={onClose}
      panelClassName="max-w-4xl"
      footer={
        <>
          <BackofficeButton
            variant="secondary"
            disabled={!filePath}
            onClick={() =>
              filePath ? void downloadAuthenticatedFile(filePath, suggestedFileName ?? file?.fileName) : undefined
            }
          >
            下载文件
          </BackofficeButton>
          <BackofficeButton onClick={onClose}>关闭</BackofficeButton>
        </>
      }
    >
      {loading ? (
        <div className="flex h-[26rem] items-center justify-center rounded-[1.5rem] bg-surface-low text-sm text-ink-muted">
          正在加载预览...
        </div>
      ) : error ? (
        <div className="rounded-[1.5rem] border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error}
        </div>
      ) : file && previewType === "image" ? (
        <div className="overflow-hidden rounded-[1.5rem] bg-surface-low">
          <img
            className="max-h-[68vh] w-full object-contain"
            src={file.objectUrl}
            alt={file.fileName ?? title}
          />
        </div>
      ) : file && previewType === "pdf" ? (
        <div className="overflow-hidden rounded-[1.5rem] border border-line bg-surface-low">
          <iframe className="h-[68vh] w-full" src={file.objectUrl} title={title} />
        </div>
      ) : (
        <div className="rounded-[1.5rem] bg-surface-low px-5 py-8 text-sm text-ink-muted">
          <p className="font-semibold text-ink">当前文件暂不支持在线预览</p>
          <p className="mt-2">建议直接下载后在本地查看。</p>
          {(file?.fileName || file?.mimeType) ? (
            <div className="mt-4 space-y-1 text-xs uppercase tracking-[0.12em] text-slate-500">
              {file?.fileName ? <div>文件名：{file.fileName}</div> : null}
              {file?.mimeType ? <div>MIME：{file.mimeType}</div> : null}
            </div>
          ) : null}
        </div>
      )}
    </Dialog>
  );
}
