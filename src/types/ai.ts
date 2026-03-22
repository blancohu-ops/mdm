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
