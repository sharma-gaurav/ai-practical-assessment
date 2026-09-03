# Planning Prompts

Record of AI prompts used for requirement analysis and planning.

---

## Prompt 1: Requirement Clarification

**Date:** [To be filled]

**Prompt Summary:** Clarify the state machine rules and identify edge cases

**Prompt Text:**
```
I'm implementing a Support Ticket Management System with a state machine.
Here are the rules:
- Open → [In Progress, Cancelled]
- In Progress → [Resolved, Cancelled]
- Resolved → [Closed]
- Closed → (terminal)
- Cancelled → (terminal)

What edge cases should I consider? Should I handle concurrent status changes?
```

**AI Response Summary:**
[Response summary to be filled]

**What I Accepted:**
- [Points accepted]

**What I Changed:**
- [Changes made to suggestions]

**What I Rejected:**
- [Points rejected and why]

---

## Prompt 2: Ticket List Component Design

**Date:** 2026-09-03

**Prompt Summary:** Design ticket list component with search and filter functionality

**Requirements Context:**
- FR4: Display all tickets in list view
- FR11: Search by keyword (title/description)
- FR12: Filter by status
- FR13: Combined search and filter
- Show: ID, Title, Description, Priority, Status

**Design Decisions:**

1. **Backend Approach**
   - Single servlet at `/bin/api/tickets/list`
   - JCR SQL2 queries with dynamic WHERE clauses
   - Pagination support (page, limit)
   - Accepts query params: search, status, page, limit

2. **Frontend Approach**
   - Vanilla JS component (no framework dependencies)
   - Debounced search (300ms)
   - Real-time filtering
   - MutationObserver for AEM compatibility

3. **Component Structure**
   - AEM component in "AI Practical Assessment - Content" group
   - Directory-based dialog: `_cq_dialog/.content.xml`
   - Coral UI form fields (numberfield, checkbox)
   - HTL template with data attributes

4. **Styling**
   - BEM naming convention
   - Color-coded badges (priority, status)
   - Responsive table (mobile-friendly)
   - Dark mode support

**What I Accepted:**
- Component group unified across all components
- Directory-based dialog structure for scalability
- Debounced search for performance
- Pagination for large datasets

**What I Changed:**
- Initial component groups (Forms/Lists) → unified to "Content"
- Single file dialog → directory structure with `.content.xml`
- Added Page Size configuration property

**Status:** Design approved and implemented

---

## Prompt 3: [To be documented]

[Follow same format]

---

## Effective Planning Patterns

### Pattern 1: Context-Rich Requirements Prompt
```
Template: "I'm building [project type] on [stack]. The requirement is [specification].
My understanding is [my summary]. Is this correct? What am I missing?"
```

### Pattern 2: Edge Case Discovery
```
Template: "For [feature], what edge cases should I handle? 
What could go wrong? Any concurrent issues?"
```

### Pattern 3: Architecture Validation
```
Template: "I'm planning to [architecture decision]. What are the pros and cons?
Any AEM-specific considerations?"
```

---

## Key Takeaways

- [Lessons learned from planning prompts]
- [Effective approaches identified]
- [Pitfalls to avoid in future]
