# Design System Strategy: The Luminous Interface

## 1. Overview & Creative North Star
**Creative North Star: "The Ethereal Engine"**

This design system is engineered to feel less like a software utility and more like a high-end digital atelier. We are moving away from the "boxy" constraints of traditional SaaS. Our goal is to create a "Luminous" experience where the UI feels like it is projected onto layers of smoked glass, illuminated by the generative energy of the AI.

To break the "template" look, we embrace **Intentional Asymmetry**. Large-scale `display-lg` typography should often offset center-aligned content, and elements should overlap with varying degrees of transparency to create a sense of physical depth. The interface should breathe, using generous whitespace to signify premium quality.

---

## 2. Colors & Surface Philosophy

The palette is rooted in deep indigo (`#10131a`) to establish a "midnight" canvas, allowing our vibrant violets and tertiary cyans to pop like neon gas.

### The "No-Line" Rule
**Explicit Instruction:** 1px solid borders are strictly prohibited for sectioning. We do not "box" our content. Boundaries must be defined through:
1.  **Background Shifts:** Using `surface-container-low` against a `surface` background.
2.  **Tonal Transitions:** Defining a change in context through a subtle color shift.
3.  **Negative Space:** Using the spacing scale to create mental boundaries.

### Surface Hierarchy & Nesting
Treat the UI as a series of stacked, translucent layers. 
*   **Base:** `surface` (#10131a).
*   **Secondary Content:** `surface-container-low`.
*   **Active/Interactive Elements:** `surface-container-high` or `highest`.
By nesting a `surface-container-lowest` card inside a `surface-container-low` section, you create a "recessed" look that implies depth without a single line of CSS border.

### The "Glass & Gradient" Rule
Floating modals and high-priority cards must use **Glassmorphism**. 
*   **Recipe:** Apply `surface_variant` at 40-60% opacity with a `backdrop-blur` of 20px-40px. 
*   **Signature Textures:** Main Action Buttons and Hero headers should utilize a linear gradient from `primary_container` (#5d21df) to `primary` (#cdbdff) at a 135-degree angle. This provides the "visual soul" required for a premium high-tech aesthetic.

---

## 3. Typography

Our typography balances the technical precision of **Inter** with the editorial elegance of **Manrope**.

*   **Display & Headlines (Manrope):** These are your "Brand Moments." Use `display-lg` for hero statements. The wider apertures of Manrope convey a high-tech, welcoming openness.
*   **Body & Titles (Inter):** Inter is used for high-utility areas. Its neutral, "invisible" quality ensures that the user's focus remains on the AI-generated imagery, not the interface.
*   **Scale Contrast:** Always pair a `display-sm` headline with `body-md` secondary text to create a dramatic hierarchy that feels curated rather than generic.

---

## 4. Elevation & Depth

We eschew traditional shadows in favor of **Tonal Layering** and **Ambient Glows**.

### The Layering Principle
Hierarchy is achieved by "stacking" surface tiers. A `surface-container-highest` element naturally "feels" closer to the user than a `surface-dim` element. 

### Ambient Shadows
When an element must float (e.g., a dropdown or modal):
*   **Blur:** 40px - 80px.
*   **Opacity:** 8% - 12%.
*   **Color:** Use a tinted shadow (`#370096` at low opacity) instead of black. This creates a "glow" effect consistent with the deep indigo theme.

### The "Ghost Border" Fallback
If accessibility requirements demand a border, use a **Ghost Border**: `outline-variant` at 15% opacity. It should be felt, not seen.

---

## 5. Components

### Buttons
*   **Primary:** Gradient fill (`primary_container` to `primary`). `lg` (1rem) roundedness. Subtle outer glow using `primary` on hover.
*   **Secondary:** Glassmorphic background (`surface_variant` at 20% opacity) with a `primary` text color.
*   **Tertiary:** No background. `label-md` uppercase with 0.05em letter spacing.

### Chips (Image Tags/Styles)
*   **Selection:** Use `tertiary_container` with `on_tertiary_container` text.
*   **Default:** `surface-container-high` with `on_surface_variant`. 
*   No borders; use `md` (0.75rem) roundedness.

### Input Fields (The "Prompt" Bar)
*   The main prompt bar should be a `surface-container-highest` pill with a `xl` (1.5rem) roundedness.
*   Internal padding should be generous (24px horizontal).
*   Focus state: A 1px "Ghost Border" of `primary` at 30% opacity and a soft `primary` outer glow.

### Cards & Lists
*   **Strict Rule:** No dividers.
*   Separate image results using a grid with 24px gaps.
*   For meta-data lists, use `surface-container-low` for alternating rows or simply use vertical white space.

### Special Component: The "Luminescence" Loader
*   A generative AI app needs a signature loading state. Use a blurred, rotating gradient blob using `primary`, `secondary`, and `tertiary` colors behind a glassmorphic pane.

---

## 6. Do's and Don'ts

### Do
*   **DO** use `xl` (1.5rem) corner radius for large containers and `md` (0.75rem) for smaller UI elements.
*   **DO** embrace "Dark Mode by Default." Use `surface_dim` for the widest background areas to reduce eye strain.
*   **DO** use `tertiary` (#00daf3) sparingly as a "technical" highlight for success states or AI processing indicators.

### Don't
*   **DON'T** use pure black (#000000) or pure white (#FFFFFF). Use the provided `surface` and `on_surface` tokens to maintain tonal richness.
*   **DON'T** use 100% opaque borders. It breaks the "Ethereal" illusion.
*   **DON'T** crowd the layout. If in doubt, increase the padding by one step on the scale. High-end design is defined by what you leave out.