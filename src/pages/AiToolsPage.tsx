import { useEffect, useState } from "react";
import { AiServiceCard } from "@/components/business/AiServiceCard";
import { AiInputPanel } from "@/components/business/AiInputPanel";
import { AiOutputPanel } from "@/components/business/AiOutputPanel";
import { ProcessingIndicator } from "@/components/business/ProcessingIndicator";
import { SubsidyBanner } from "@/components/business/SubsidyBanner";
import { FeatureCard } from "@/components/common/FeatureCard";
import { SectionHeader } from "@/components/common/SectionHeader";
import { aiBanner, aiHighlights, aiInput, aiResult, aiServices, aiToolHero } from "@/mocks/ai-tools";

export function AiToolsPage() {
  const [input, setInput] = useState(aiInput.sample);
  const [loading, setLoading] = useState(false);
  const [resultVisible, setResultVisible] = useState(true);

  useEffect(() => {
    let timer: number | undefined;

    if (loading) {
      timer = window.setTimeout(() => {
        setLoading(false);
        setResultVisible(true);
      }, 1200);
    }

    return () => {
      if (timer) {
        window.clearTimeout(timer);
      }
    };
  }, [loading]);

  return (
    <section className="section-spacing">
      <div className="shell-container">
        <SectionHeader eyebrow={aiToolHero.eyebrow} title={aiToolHero.title} description={aiToolHero.description} />
        <div className="mt-10">
          <SubsidyBanner title={aiBanner.title} description={aiBanner.description} />
        </div>
        <div className="mt-16">
          <SectionHeader
            eyebrow="AI Service Matrix"
            title="AI 智能服务矩阵"
            description="围绕外贸单证、邮件协同、客户服务与流程自动化，平台将逐步开放更完整的 AI 服务能力。"
          />
          <div className="mt-8 grid gap-6 md:grid-cols-2 xl:grid-cols-4">
            {aiServices.map((service) => (
              <AiServiceCard key={service.id} service={service} />
            ))}
          </div>
        </div>
        <div className="mt-10 grid gap-8 lg:grid-cols-[0.92fr_1.08fr]">
          <div className="space-y-6">
            <AiInputPanel
              data={aiInput}
              value={input}
              onChange={setInput}
              loading={loading}
              onGenerate={() => {
                setLoading(true);
                setResultVisible(false);
              }}
            />
            {loading ? <ProcessingIndicator /> : null}
          </div>
          {resultVisible ? (
            <AiOutputPanel result={aiResult} />
          ) : (
            <div className="industrial-card p-8 text-sm leading-7 text-ink-muted">AI 正在根据当前输入生成演示结果...</div>
          )}
        </div>
        <div className="mt-20 grid gap-6 md:grid-cols-3">
          {aiHighlights.map((item) => (
            <FeatureCard key={item.title} title={item.title} description={item.description} icon={item.icon} />
          ))}
        </div>
      </div>
    </section>
  );
}
