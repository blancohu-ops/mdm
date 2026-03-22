import type { ReactNode } from "react";

export type NavItem = {
  label: string;
  path: string;
};

export type FooterGroup = {
  title: string;
  links: NavItem[];
};

export type HeroStat = {
  value: string;
  label: string;
};

export type CtaConfig = {
  eyebrow?: string;
  title: string;
  description: string;
  primaryAction: NavItem;
  secondaryAction?: NavItem;
};

export type FeatureItem = {
  title: string;
  description: string;
  icon: ReactNode;
  tag?: string;
};

export type FaqItem = {
  question: string;
  answer: string;
};

export type StepItem = {
  title: string;
  description: string;
};
