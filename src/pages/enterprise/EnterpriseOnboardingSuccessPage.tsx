import { BackofficeButton, EmptyState } from "@/components/backoffice/BackofficePrimitives";

export function EnterpriseOnboardingSuccessPage() {
  return (
    <EmptyState
      icon="task_alt"
      title="入驻申请已提交"
      description="预计 1-3 个工作日内完成审核。你可以返回工作台查看审核进度，也可以继续完善企业资料。"
      actions={
        <>
          <BackofficeButton to="/enterprise/dashboard">返回工作台</BackofficeButton>
          <BackofficeButton to="/enterprise/profile" variant="secondary">
            查看申请详情
          </BackofficeButton>
        </>
      }
    />
  );
}
