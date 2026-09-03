# Design Prompts

Record of AI prompts used for architecture and design decisions.

---

## Prompt 1: AEM Content Structure

**Date:** [To be filled]

**Prompt Summary:** Recommend content hierarchy and node types for tickets

**Prompt Text:**
```
[Actual prompt to be filled]
```

**AI Response Summary:**
[Response summary]

**What I Accepted:**
[Points accepted]

**What I Changed:**
- Changed from `/var/tickets` to `/content/ai-practical-assessment/tickets` hierarchy

**What I Rejected:**
[Points rejected and why]

---

## Prompt 2: OSGi Service Architecture

**Date:** [To be filled]

**Prompt Summary:** Design services for ticket management

**Prompt Text:**
```
[Actual prompt to be filled]
```

**AI Response Summary:**
[Response summary]

**What I Accepted:**
[Points accepted]

**What I Changed:**
[Changes made]

**What I Rejected:**
[Points rejected and why]

---

## Prompt 3: Frontend Architecture

**Date:** [To be filled]

**Prompt Summary:** Recommend frontend approach for AEM

**Prompt Text:**
```
[Initial: Should I use React or Vanilla JS?]
```

**AI Response Summary:**
[AI suggested React initially]

**What I Accepted:**
[None - corrected to vanilla JavaScript per project requirements]

**What I Changed:**
- Redirected to vanilla JavaScript approach

**What I Rejected:**
- React framework suggestion (replaced with vanilla JS)

---

## Prompt 4: Ticket List Component UI/UX Design

**Date:** 2026-09-03

**Prompt Summary:** Design ticket list component UI with search and filter

**Design Considerations:**

1. **Table Layout**
   - Columns: ID, Title, Description, Priority, Status
   - Responsive design (hide description on mobile)
   - Color-coded badges for priority and status
   - Truncate description (100 char limit)

2. **Search & Filter**
   - Search input with placeholder text
   - Status dropdown (All/Open/In Progress/Resolved/Closed/Cancelled)
   - Clear filters button
   - Combined search + filter capability

3. **States**
   - Loading state: "Loading tickets..."
   - Empty state: "No tickets found. Try refining your search."
   - Error state: Display error message
   - Success state: Display populated table

4. **Interactivity**
   - Row click navigates to ticket detail
   - Search with 300ms debounce for performance
   - Real-time filter without page reload
   - Visual feedback on hover

**Component Styling Decisions:**
- Badge colors for priority: HIGH (red), MEDIUM (orange), LOW (green)
- Badge colors for status: Open (blue), In Progress (purple), Resolved (teal), Closed (green), Cancelled (pink)
- Dark mode support with CSS variables
- BEM naming for maintainability

**Accessibility Features:**
- ARIA labels on inputs
- Live regions for loading/error messages
- Semantic HTML table structure
- Keyboard navigable

**What I Accepted:**
- Table-based layout for structured data
- Color-coded badges for quick visual recognition
- Debounced search for performance
- Responsive design with mobile considerations

**What I Changed:**
- Added truncation to description in list view
- Made rows clickable for detail navigation
- Added visual hover effects
- Added clear filters button for UX clarity

**Status:** Design implemented and validated

---

## Prompt 5: UI/UX Refinements & Layout Fixes

**Date:** 2026-09-03

**Activity:** UI Polish & Responsive Design

**Prompt Summary:** Fix layout issues and refine UI for better user experience

**Issues Addressed:**

1. **Layout Issues Fixed:**
   - **Overlapping elements:** Increased search min-width, adjusted flex properties
   - **Width overflow:** Added `box-sizing: border-box` to inputs
   - **Alignment inconsistency:** Changed priority/status to left-aligned
   - **Responsive breakpoint:** Added 900px breakpoint for better mobile experience

2. **Dark Mode Enhancements:**
   - Text colors fixed (main component, buttons, table cells)
   - Primary button: `#4040ff` with `#ffffff` text
   - Secondary button: `#333` background
   - Table headers: `#2d2d2d` background
   - Table cells: `#e0e0e0` text color

3. **UI Simplification:**
   - Removed visual `<label>` elements
   - Kept intuitive placeholders: "Search by title or description..."
   - Cleaner, more minimal interface
   - Better space utilization

4. **Responsive Design:**
   - Desktop (> 900px): Horizontal layout, search takes priority
   - Tablet (600px - 900px): Stacked vertically
   - Mobile (< 600px): Full-width elements
   - Proper white-space handling

5. **Visual Polish:**
   - Color-coded badges (priority: red/orange/green, status: 5 distinct colors)
   - Smooth transitions (0.2s) on all interactive elements
   - Proper hover states with visual feedback
   - Rounded corners (4px - 8px) for modern look

**Layout Improvements:**
✓ No overlapping elements
✓ Proper spacing (1rem gaps)
✓ Intuitive form layout
✓ Responsive at all breakpoints
✓ Accessible color contrast

**Design System:**
✓ CSS variables for theming
✓ BEM naming convention
✓ Consistent spacing scale
✓ Light and dark mode support
✓ Mobile-first approach

**Status:** Design refined and production-ready

---

---

## Effective Design Patterns

### Pattern 1: Architecture Trade-offs
```
Template: "I'm considering [approach A] vs [approach B] for [component].
What are the trade-offs? Which would be better for [context]?"
```

### Pattern 2: Pattern Validation
```
Template: "Is [pattern] the right approach for [problem] in AEM?
What are the alternatives?"
```

### Pattern 3: Data Modeling
```
Template: "I need to store [data]. Should I use [option 1] or [option 2]?
What are performance/scalability considerations?"
```

---

## Design Decisions Validated

| Decision | AI Input | Final Choice | Reasoning |
|----------|----------|--------------|-----------|
| Content Structure | Recommended hierarchy | Page-based under /content | Corrected per requirements |
| Frontend | Suggested React | Vanilla JavaScript | Project requirement |
| State Machine | Suggested config-driven | Hardcoded in code | Simplicity for Core |
| Comments | Suggested mutable | Immutable | Simpler logic, audit trail |

---

## Key Takeaways

- [Lessons learned from design phase]
- [Effective AI input areas]
- [Where human judgment was critical]
