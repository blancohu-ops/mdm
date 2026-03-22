import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        surface: "var(--color-surface)",
        "surface-low": "var(--color-surface-low)",
        "surface-card": "var(--color-surface-card)",
        "surface-muted": "var(--color-surface-muted)",
        line: "var(--color-line)",
        primary: "var(--color-primary)",
        "primary-strong": "var(--color-primary-strong)",
        accent: "var(--color-accent)",
        ink: "var(--color-ink)",
        "ink-muted": "var(--color-ink-muted)",
        "chip-bg": "var(--color-chip-bg)",
        "chip-text": "var(--color-chip-text)",
      },
      fontFamily: {
        display: ["'Manrope'", "sans-serif"],
        body: ["'Noto Sans SC'", "'Inter'", "sans-serif"],
      },
      boxShadow: {
        soft: "0 14px 50px -24px rgba(17, 24, 39, 0.28)",
        panel: "0 18px 60px -28px rgba(15, 23, 42, 0.32)",
      },
      backgroundImage: {
        "industrial-gradient":
          "linear-gradient(135deg, var(--color-primary-strong), var(--color-primary))",
        "steel-grid":
          "linear-gradient(rgba(255,255,255,0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.08) 1px, transparent 1px)",
      },
      animation: {
        float: "float 5s ease-in-out infinite",
        pulseDot: "pulseDot 1.2s ease-in-out infinite",
      },
      keyframes: {
        float: {
          "0%, 100%": { transform: "translateY(0px)" },
          "50%": { transform: "translateY(-8px)" },
        },
        pulseDot: {
          "0%, 80%, 100%": { opacity: "0.35", transform: "translateY(0)" },
          "40%": { opacity: "1", transform: "translateY(-4px)" },
        },
      },
    },
  },
  plugins: [],
} satisfies Config;
