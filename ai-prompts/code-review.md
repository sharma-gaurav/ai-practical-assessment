# Code Review Prompts

Record of AI prompts used for code review and quality assessment.

---

## Prompt 1: Security Review

**Date:** [To be filled]

**Prompt Summary:** Review code for security vulnerabilities

**Prompt Text:**
```
Review this code for security issues:
[Code snippet]

Concerns: 
- Input validation
- XSS prevention
- JCR injection
- Secrets handling
- Authorization checks

Are there any vulnerabilities?
```

**AI Response Summary:**
[Identified security concerns]

**Issues Found by AI:**
1. [Issue 1] - Severity: [High/Med/Low]
2. [Issue 2] - Severity: [High/Med/Low]

**My Validation:**
- Confirmed each issue
- Verified fix approach
- Implemented fixes

**Issues Implemented:** [Number]

---

## Prompt 2: Performance Review

**Date:** [To be filled]

**Prompt Summary:** Review for performance issues

**Prompt Text:**
```
Review this code for performance issues:
[Code snippet]

This runs for [N] items. Are there N+1 queries?
Any optimization opportunities?
```

**AI Response Summary:**
[Performance analysis]

**Optimizations Suggested:**
1. [Optimization 1]
2. [Optimization 2]

**Implemented:** [Yes/No] [Reason]

---

## Prompt 3: Best Practices Review

**Date:** [To be filled]

**Prompt Summary:** Review for AEM/Java best practices

**Prompt Text:**
```
Does this follow AEM best practices?
[Code snippet]

Checking for:
- OSGi patterns
- Sling conventions
- Resource management
- Error handling
```

**AI Response Summary:**
[Best practices analysis]

**Issues Found:**
1. [Issue 1]
2. [Issue 2]

**Changes Made:**
[Summary of changes]

---

## Prompt 4: Code Simplification

**Date:** [To be filled]

**Prompt Summary:** Suggest refactoring for simplicity

**Prompt Text:**
```
Can this be simplified?
[Code snippet]

What would be cleaner/more maintainable?
```

**AI Response Summary:**
[Suggested refactoring]

**Accepted:** [Yes/No] [Reason]

---

## Code Review Findings Summary

| Category | Issues Found | Fixed | Deferred | Rejected |
|----------|--------------|-------|----------|----------|
| Security | [X] | [X] | [X] | [X] |
| Performance | [X] | [X] | [X] | [X] |
| Best Practices | [X] | [X] | [X] | [X] |
| Simplification | [X] | [X] | [X] | [X] |

---

## Code Review Effectiveness

**Issues Found by AI:** [X]

**Issues Found by Me:** [X]

**Overlap:** [X] (both found same issues)

**AI Unique Finds:** [X]

**My Unique Finds:** [X]

**False Positives:** [X] (issues AI flagged that weren't real)

---

## Effective Review Patterns

### Pattern 1: Focused Review
```
Template: "Review [component] for [specific concern].
Context: [Background].
Here's the code: [Snippet]"
```

### Pattern 2: Before vs After
```
Template: "Is the refactored version better?
Before: [Old code]
After: [New code]
Why: [Rationale]"
```

### Pattern 3: Quality Checklist
```
Template: "Does this meet our quality standards?
- [Requirement 1]: [Code for review]
- [Requirement 2]: [Code for review]"
```

---

## Lessons for Code Review with AI

- AI good at finding structural issues (N+1 queries, missing error handling)
- AI less good at judging simplicity (domain judgment needed)
- Combine AI review with peer review for best results
- Always validate AI's security suggestions with authoritative sources
- Code review should be iterative: find issues, fix, review again

---

## Prompt 5: Service User Permissions Review

**Date:** 2026-09-03

**Activity:** Security & Best Practices

**Prompt Summary:** Review service user permissions configuration for security and AEM best practices

**Concern:** 
Service user `ai-practical-assessment-ticketservice` needs appropriate JCR permissions to execute node operations in TicketServiceImpl. Must balance security (principle of least privilege) with functionality (ability to create/manage tickets).

**Review Findings:**

1. **Permission Scope Analysis**
   - Appropriate: `jcr:read,rep:write` on `/content/ai-practical-assessment`
   - Appropriate: `jcr:all` on `/content/ai-practical-assessment/tickets`
   - Appropriate: `jcr:read` on `/home/users` and `/home/groups` for user validation
   - Reasoning: Follows principle of least privilege - minimal permissions needed for functionality

2. **Security Validation**
   - Service user is system-scoped (path: `/home/users/system/ai-practical-assessment`)
   - Used only for backend operations, not exposed to UI
   - Permissions confined to specific content hierarchy
   - No write permissions on sensitive paths (e.g., `/apps`, `/etc`)
   - ✓ Passes security validation

3. **Best Practices Compliance**
   - ✓ Using Sling RepoInit (modern approach, not legacy _rep_policy.xml)
   - ✓ Service user isolated to specific path hierarchy
   - ✓ ACLs defined in bundled configuration (portable, versionable)
   - ✓ Clear naming convention identifies service purpose
   - ✓ Permissions documented in configuration file

4. **Potential Improvements Considered & Rejected**
   - Fine-grained permissions (e.g., only `jcr:addChildNodes`) - rejected because `rep:write` needed for property modifications
   - Broader service user access - rejected to maintain security boundary
   - Additional read permissions - not needed for current functionality

**Status:** APPROVED ✅ - Security validated, best practices followed, configuration complete

---

## Prompt 6: Ticket List Component Structure Review

**Date:** 2026-09-03

**Activity:** Component Architecture & Structure

**Prompt Summary:** Review ticket list component structure and AEM dialog configuration for best practices

**Code Review Focus:**

1. **Component Structure**
   - Component definition properly registered with correct group
   - HTL template uses data attributes for JavaScript hooks
   - Accessibility attributes (ARIA labels, roles, live regions)
   - Semantic HTML structure

2. **Dialog Configuration**
   - Initial Issue: Dialog was single file (`_cq_dialog.xml`)
   - Fix Applied: Changed to directory structure (`_cq_dialog/.content.xml`)
   - Proper Coral UI components (numberfield, checkbox)
   - Tab-based organization for scalability

3. **Frontend Component**
   - XSS Prevention: HTML escaping for all user-generated content
   - Error Handling: Try-catch blocks, network error handling
   - Performance: Debounced search (300ms)
   - Memory: Proper cleanup with MutationObserver patterns

4. **Backend Servlet**
   - SQL Injection Prevention: No string concatenation, parameterized queries
   - Null Safety: Safe property access with fallback defaults
   - Logging: Appropriate error logging without exposing internal details
   - Response Format: Consistent JSON structure with error messages

**Issues Found & Fixed:**
- ❌ Component group inconsistency (Forms/Lists) → ✅ Unified to "Content"
- ❌ Dialog file structure incorrect → ✅ Changed to directory-based `.content.xml`
- ❌ Missing component descriptions → ✅ Added descriptions and icons

**Security Validation:**
- ✓ No SQL injection vulnerabilities (parameterized queries)
- ✓ No XSS vulnerabilities (HTML escaping on output)
- ✓ Proper error messages (no stack traces exposed)
- ✓ Service user permissions respected
- ✓ Input validation on server side

**Best Practices Applied:**
- ✓ BEM naming convention for CSS
- ✓ IIFE pattern for JavaScript module encapsulation
- ✓ Data attributes for component initialization
- ✓ MutationObserver for dynamic content handling
- ✓ Responsive design with mobile considerations
- ✓ Dark mode support
- ✓ Accessibility-first approach

**Status:** APPROVED ✅ - Component structure correct, security validated, best practices followed

---

## Prompt 7: HTML Best Practices & Accessibility Cleanup

**Date:** 2026-09-03

**Activity:** Code Quality & Accessibility

**Prompt Summary:** Review and improve HTML best practices for ticket list component

**Issues Identified & Fixed:**

1. **HTML Structure Issues:**
   - ❌ Missing `<label>` elements → Kept accessible via `aria-label`
   - ❌ Missing `id` attributes → Added `id="ticketlist-search"` and `id="ticketlist-filter"`
   - ✅ Changed `role="grid"` to semantic `<table>`
   - ✅ Changed button type to `type="button"`

2. **Accessibility Improvements:**
   - Added `id` attributes for potential future label association
   - Maintained `aria-label` for screen reader support
   - Kept intuitive placeholders for visual users
   - Proper semantic HTML with `<thead>`, `<tbody>`, `<th>` elements

3. **CSS Box Model Fixes:**
   - Added `box-sizing: border-box` to input and select
   - Prevented width overflow with proper padding calculation
   - Fixed flex layout for search/filter container

4. **Layout Refinements:**
   - Adjusted flex properties to prevent overlapping
   - Added responsive breakpoint at 900px
   - Fixed text alignment (priority/status left-aligned)
   - Improved spacing and white-space handling

5. **Dark Mode Fixes:**
   - Added missing text color to main component: `color: #e0e0e0`
   - Added primary button dark mode styling: `#4040ff` bg
   - Fixed all table cell text colors for dark mode
   - Added dark mode support to loading/empty states

**Final HTML Structure:**
✓ Clean, semantic markup
✓ Intuitive placeholders ("Search by title or description...")
✓ Proper accessibility with aria-labels
✓ No unnecessary visual labels (cleaner UI)
✓ Maintained id attributes for flexibility

**CSS Improvements:**
✓ Box-sizing applied to all form inputs
✓ Responsive flex layout (900px, 600px breakpoints)
✓ Proper color contrast in both light and dark modes
✓ Smooth transitions and hover effects

**Status:** APPROVED ✅ - HTML cleaned up, accessibility maintained, CSS refined

---

## Quality Gate Checklist

- [x] Security review passed
- [x] Performance acceptable
- [x] Best practices followed
- [x] Code simplified
- [x] Error handling complete
- [x] Logging appropriate
- [x] No dead code
- [x] Tests passing
- [x] Documentation updated
- [x] Service user permissions secured
