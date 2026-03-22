# Design System Strategy: Industrial Authority & Data Sovereignty

## 1. Overview & Creative North Star: "The Architectural Ledger"
For a government-backed industrial platform like "工业企业出海主数据平台," the design must transcend standard "SaaS Blue" templates. Our Creative North Star is **"The Architectural Ledger."** 

This concept treats the UI not as a website, but as a high-precision digital instrument. We break the "template" look by moving away from boxed grids toward a layout driven by **intentional asymmetry and tonal depth**. By utilizing staggered content blocks and sophisticated editorial typography, we signal authority. The interface should feel as stable as a physical industrial foundation, yet as fluid as global data streams.

---

## 2. Color Theory & Tonal Application
Our palette is anchored in **Deep Industrial Blue** to convey state-backed stability, balanced by a high-tech **Cyan** to represent digital transformation.

### The "No-Line" Rule
Standard 1px borders are strictly prohibited for sectioning. High-end design conveys structure through **background color shifts**. 
- Use `surface-container-low` (#f2f4f7) to define a section sitting on a `surface` (#f7f9fc) background. 
- Separation is achieved through the contrast of these planes, not by drawing lines.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers—like stacked sheets of industrial glass. 
- **Base Layer:** `surface` (#f7f9fc)
- **Content Zones:** `surface-container` (#eceef1)
- **Primary Cards:** `surface-container-lowest` (#ffffff) to provide a "lifted" feel against the grey base.

### Signature Textures: The "Industrial Glow"
To avoid a flat, "cheap" feel, use subtle linear gradients on primary CTAs and hero backgrounds.
- **Primary Action Gradient:** Transition from `primary` (#00386c) to `primary_container` (#1a4f8b) at a 135-degree angle. This adds "visual soul" and a sense of metallic depth suitable for industrial contexts.

---

## 3. Typography: Editorial Authority
The type system pairs the technical precision of **Inter** with the structural elegance of **Manrope**.

| Level | Token | Font | Size | Intent |
| :--- | :--- | :--- | :--- | :--- |
| **Display** | `display-lg` | Manrope | 3.5rem | High-impact data totals / Hero statements. |
| **Headline**| `headline-md`| Manrope | 1.75rem | Major section headers (Semi-bold). |
| **Title**   | `title-md`   | Inter   | 1.125rem | Card titles and subsection navigation. |
| **Body**    | `body-md`    | Inter   | 0.875rem | General reporting data and descriptions. |
| **Label**   | `label-sm`   | Inter   | 0.6875rem | Meta-data, timestamps, and industrial tags. |

**Hierarchy Note:** Use wide tracking (letter-spacing: 0.05em) on `label` styles to evoke a "blueprint" aesthetic.

---

## 4. Elevation & Depth: The Layering Principle
We move away from traditional drop shadows toward **Tonal Layering** and **Ambient Light**.

*   **Shadow Construction:** When a card must float (e.g., a modal or active dropdown), use an extra-diffused shadow: `box-shadow: 0 12px 32px -4px rgba(25, 28, 30, 0.06)`. The shadow color is a tint of the `on-surface` color, never pure black.
*   **The Ghost Border:** If high-density data requires containment, use the `outline_variant` (#c2c6d1) at **15% opacity**. This creates a "Ghost Border" that guides the eye without cluttering the UI.
*   **Glassmorphism:** Use `surface_container_lowest` at 80% opacity with a `backdrop-blur(12px)` for navigation bars. This allows the industrial primary colors to bleed through softly, creating a "frosted glass" effect that feels modern and premium.

---

## 5. Component Logic

### Buttons: High-Stakes Interaction
*   **Primary:** Gradient fill (`primary` to `primary_container`), `lg` (0.5rem) roundedness. No border.
*   **Secondary:** `surface_container_high` (#e6e8eb) background with `on_primary_fixed_variant` (#0c4783) text.
*   **Tertiary:** Ghost style; text-only using `primary` (#00386c) with a subtle `3.5` (0.75rem) horizontal padding for hover states.

### Data Cards & Lists
*   **Constraint:** Forbid the use of divider lines between list items.
*   **Spacing:** Use `spacing-5` (1.1rem) or `spacing-6` (1.3rem) to separate entries. 
*   **Visual Shift:** Use alternating background colors (`surface_container_low` vs `surface_container_lowest`) for large data tables to maintain readability without "grid fatigue."

### Industrial Chips (Tags)
*   **Status Chips:** Use `secondary_container` (#cbe7f5) with `on_secondary_container` (#4e6874) text for a muted, professional categorization that doesn't distract from the data.

### Input Fields
*   **State:** Default state uses `surface_container_highest` (#e0e3e6) as a subtle background fill rather than a border.
*   **Focus:** Transition to a 2px "Ghost Border" using the tech-accent Cyan to indicate "system active."

---

## 6. Do’s and Don’ts

### Do:
*   **Do** use asymmetrical margins. For example, a page title might have a `spacing-16` (3.5rem) left margin while the content has `spacing-10` (2.25rem) to create a sophisticated editorial flow.
*   **Do** use `9999px` (Full) roundedness only for functional tags or "Status" indicators; keep containers at `lg` (0.5rem) to maintain a structural, industrial feel.
*   **Do** prioritize white space over lines. If the data feels "loose," increase the background contrast between the card and the page.

### Don’t:
*   **Don’t** use pure black (#000000) for text. Use `on_surface` (#191c1e) to keep the contrast accessible but "soft."
*   **Don’t** use traditional "E-commerce" animations. Keep transitions fast (200ms) and linear-out (ease-out) to mimic the precision of industrial machinery.
*   **Don’t** place 100% opaque borders around cards. It creates a "boxed-in" feeling that contradicts the "Outbound/Expansion" (出海) nature of the platform.