import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BackofficeButton,
  BackofficePageHeader,
  EmptyState,
  SectionCard,
  StatusBadge,
} from "@/components/backoffice/BackofficePrimitives";
import { Dialog } from "@/components/backoffice/BackofficeOverlays";
import { enterpriseService } from "@/services/enterpriseService";
import type { EnterpriseMessagesResponse } from "@/services/contracts/backoffice";
import type { MessageRecord } from "@/types/backoffice";

export function EnterpriseMessagesPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<"system" | "review">("system");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [payload, setPayload] = useState<EnterpriseMessagesResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [selectedMessage, setSelectedMessage] = useState<MessageRecord | null>(null);
  const [error, setError] = useState("");

  const loadMessages = async (nextTab = tab, nextUnreadOnly = unreadOnly) => {
    setLoading(true);
    setError("");
    try {
      const result = await enterpriseService.getMessages({
        type: nextTab,
        status: nextUnreadOnly ? "unread" : undefined,
      });
      setPayload(result.data);
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "加载消息失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadMessages();
  }, [tab, unreadOnly]);

  const messages = payload?.items ?? [];

  const markOneRead = async (messageId: string) => {
    try {
      setWorking(true);
      await enterpriseService.markMessageRead(messageId);
      await loadMessages();
    } catch (serviceError) {
      setError(serviceError instanceof Error ? serviceError.message : "标记已读失败");
    } finally {
      setWorking(false);
    }
  };

  const openMessage = async (message: MessageRecord) => {
    if (message.status === "unread") {
      await markOneRead(message.id);
    }

    setSelectedMessage({
      ...message,
      status: "read",
    });
  };

  return (
    <div className="space-y-8" data-testid="enterprise-messages-page">
      <BackofficePageHeader
        eyebrow="E11"
        title="消息中心"
        description="查看系统通知和审核通知，支持消息详情、标记已读，以及一键跳转到相关业务页面。"
        actions={
          <BackofficeButton
            variant="secondary"
            disabled={working}
            onClick={async () => {
              try {
                setWorking(true);
                await enterpriseService.markAllMessagesRead();
                await loadMessages();
              } catch (serviceError) {
                setError(serviceError instanceof Error ? serviceError.message : "全部已读失败");
              } finally {
                setWorking(false);
              }
            }}
          >
            全部已读
          </BackofficeButton>
        }
      />

      {error ? (
        <div className="rounded-3xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      <SectionCard title="通知列表">
        <div className="mb-6 flex flex-wrap items-center gap-3">
          {[
            ["system", "系统通知"],
            ["review", "审核通知"],
          ].map(([value, label]) => (
            <button
              key={value}
              type="button"
              onClick={() => setTab(value as "system" | "review")}
              className={[
                "rounded-full px-4 py-2 text-sm font-medium",
                tab === value ? "bg-primary text-white" : "bg-[#edf3fb] text-primary-strong",
              ].join(" ")}
            >
              {label}
            </button>
          ))}

          <label className="ml-auto flex items-center gap-2 text-sm text-ink-muted">
            <input
              type="checkbox"
              checked={unreadOnly}
              onChange={(event) => setUnreadOnly(event.target.checked)}
            />
            仅看未读
          </label>
        </div>

        {loading ? (
          <div className="rounded-2xl bg-[#f7f9fc] px-5 py-6 text-sm text-ink-muted">
            正在加载消息...
          </div>
        ) : messages.length === 0 ? (
          <EmptyState
            title="暂无消息通知"
            description="当前筛选条件下没有可展示的通知。"
            icon="notifications_off"
          />
        ) : (
          <div className="space-y-4">
            {messages.map((item) => (
              <div key={item.id} className="rounded-2xl bg-[#f7f9fc] px-5 py-4">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                  <div className="min-w-0">
                    <div className="font-medium text-primary-strong">{item.title}</div>
                    <div className="mt-1 text-sm text-ink-muted">{item.summary}</div>
                  </div>
                  <div className="flex flex-wrap items-center gap-3">
                    <StatusBadge notificationType={item.type} />
                    <StatusBadge notificationStatus={item.status} />
                    <span className="text-sm text-ink-muted">{item.time}</span>
                    <BackofficeButton variant="ghost" onClick={() => void openMessage(item)}>
                      查看详情
                    </BackofficeButton>
                    {item.status === "unread" ? (
                      <BackofficeButton
                        variant="ghost"
                        onClick={async () => {
                          await markOneRead(item.id);
                        }}
                      >
                        标记已读
                      </BackofficeButton>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </SectionCard>

      <Dialog
        open={Boolean(selectedMessage)}
        title={selectedMessage?.title ?? "消息详情"}
        description={selectedMessage?.time}
        onClose={() => setSelectedMessage(null)}
        footer={
          <>
            {selectedMessage?.actionPath ? (
              <BackofficeButton
                variant="secondary"
                onClick={() => {
                  const target = selectedMessage.actionPath;
                  if (!target) {
                    return;
                  }
                  setSelectedMessage(null);
                  navigate(target);
                }}
              >
                去处理
              </BackofficeButton>
            ) : null}
            <BackofficeButton onClick={() => setSelectedMessage(null)}>关闭</BackofficeButton>
          </>
        }
      >
        <div className="space-y-4">
          <div className="flex flex-wrap gap-3">
            <StatusBadge notificationType={selectedMessage?.type} />
            <StatusBadge notificationStatus={selectedMessage?.status} />
          </div>
          <div className="rounded-2xl bg-[#f7f9fc] px-4 py-4 text-sm leading-7 text-ink-muted">
            {selectedMessage?.content ?? selectedMessage?.summary ?? "暂无详细内容"}
          </div>
        </div>
      </Dialog>
    </div>
  );
}
