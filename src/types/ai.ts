export type AiToolDemoInput = {
  label: string;
  placeholder: string;
  sample: string;
};

export type AiToolDemoResult = {
  englishDescription: string;
  hsCode: string;
  hsDescription: string;
  categories: string[];
  highlights: string[];
};

export type AiServiceIntro = {
  id: string;
  title: string;
  icon: string;
  description: string;
  features: string[];
  status: "available" | "coming_soon";
};
