# AI Prompt Recording Guide

A practical guide to systematically record all AI prompts as you work on the project.

---

## Overview

Record ALL prompts you use during development to show:
- How you use AI across the lifecycle
- Your thought process and validation
- What you accepted vs. changed vs. rejected
- Evidence of iteration and critical thinking

---

## File Organization

Prompts are organized by **activity** in `ai-prompts/` directory:

```
ai-prompts/
├── planning.md         ← Requirements, clarifications, planning
├── design.md           ← Architecture, design decisions
├── implementation.md   ← Code generation, scaffolding
├── testing.md          ← Test strategy, test cases
├── debugging.md        ← Issues, troubleshooting
├── code-review.md      ← Code review, best practices
└── documentation.md    ← Docs, guides, explanations
```

---

## Quick Template (Copy & Use)

### For Each Prompt:

```markdown
## Prompt N: [Brief Title]

**Date:** [Date or leave blank]

**Activity:** [planning/design/implementation/testing/debugging/review/docs]

**Prompt Summary:** [One-line summary]

**Prompt Text:**
```
[Paste or describe your actual prompt]
```

**AI Response Summary:**
[Describe what AI returned - 2-3 sentences]

**What I Accepted:**
- [Point 1]
- [Point 2]

**What I Changed:**
- [Change 1] - why: [reason]
- [Change 2] - why: [reason]

**What I Rejected:**
- [Rejected item 1] - why: [reason]

**Status:** [Complete/In Progress/Deferred]

---
```

---

## Step-by-Step Workflow

### When You Ask Claude Something:

**1. Start the Prompt (30 seconds)**
```
Go to: ai-prompts/[activity].md

Add a new section:
## Prompt N: [Title]

**Prompt Summary:** [Quick summary]
```

**2. After Getting Response (2 minutes)**
```
Edit the same section and add:

**AI Response Summary:** [What you got back]
```

**3. After Testing/Validating (5 minutes)**
```
Edit again and add:

**What I Accepted:** [What worked]
**What I Changed:** [What you modified]
**What I Rejected:** [What didn't apply]
```

---

## Examples for Each Activity Type

### Example 1: PLANNING Prompt

**File:** `ai-prompts/planning.md`

```markdown
## Prompt 1: Clarify State Machine Rules

**Prompt Summary:** Validate state machine transitions and identify edge cases

**Prompt Text:**
```
I'm implementing a ticket status state machine:
- Open → [In Progress, Cancelled]
- In Progress → [Resolved, Cancelled]
- Resolved → [Closed]

What edge cases should I handle? Any concurrent issues I should consider?
```

**AI Response Summary:**
Claude explained the transitions clearly and identified 4 edge cases: concurrent updates, terminal state handling, invalid transition errors, and form state conflicts.

**What I Accepted:**
- Edge case: Concurrent status changes (last-write-wins)
- Edge case: Terminal state prevention
- Recommendation: Clear error messages with valid next states
- Recommendation: Validate on both frontend and backend

**What I Changed:**
- Decided NOT to implement optimistic locking (keeping simple for Core)
- Will handle concurrent updates with simple last-write-wins

**What I Rejected:**
- Complex distributed transaction handling (not needed for single AEM instance)

**Status:** Complete
```

---

### Example 2: DESIGN Prompt

**File:** `ai-prompts/design.md`

```markdown
## Prompt 1: AEM Content Hierarchy for Tickets

**Prompt Summary:** Design JCR content structure for ticket storage

**Prompt Text:**
```
Design content hierarchy for ticket management in AEM:
- Store tickets as pages
- Store comments as child nodes
- Need search by keyword and status filter
- Query performance important

What structure would you recommend?
```

**AI Response Summary:**
Claude recommended page-based hierarchy at `/content/ai-practical-assessment/tickets/` with cq:Page for tickets and nt:unstructured for comments. Suggested indexes on status and title properties.

**What I Accepted:**
- Page-based structure (leverages AEM's native page management)
- Index recommendations (status, title, createdAt)
- Query patterns suggested

**What I Changed:**
- Corrected path from `/var/tickets` to `/content/ai-practical-assessment/tickets` per project requirements
- Added `jcr:content` property mapping for Sling Models
- Defined specific node property names

**What I Rejected:**
- Asset-based approach (less AEM-native)
- Complex nested structures (simpler flat hierarchy better)

**Status:** Complete
```

---

### Example 3: IMPLEMENTATION Prompt

**File:** `ai-prompts/implementation.md`

```markdown
## Prompt 1: Generate TicketService Implementation

**Prompt Summary:** Generate OSGi service for ticket CRUD operations

**Prompt Text:**
```
Generate an OSGi service for managing tickets in AEM:
- Service name: TicketService
- Methods: create(title, description, priority, assignee)
         read(ticketId)
         update(ticketId, fields)
         list()
         search(keyword)
         filterByStatus(status)
- Store in JCR under /content/ai-practical-assessment/tickets
- Validate: title/description not empty, priority in [HIGH, MEDIUM, LOW]
- Handle errors: missing ticket, invalid assignee, permission issues
- Use @Reference for ResourceResolver injection
```

**AI Response Summary:**
Claude generated a complete TicketService with OSGi @Component annotation, all CRUD methods, JCR queries, input validation, and proper error handling.

**What I Accepted:**
- OSGi service structure and annotations
- All CRUD method signatures
- JCR query patterns for search/filter
- Error handling approach

**What I Changed:**
- Added more specific error messages (include valid states for state machine)
- Enhanced validation to check user existence
- Added logging for debugging
- Simplified query patterns (original was too complex)

**What I Rejected:**
- None - code quality was high

**Status:** Complete, tested and deployed
```

---

### Example 4: TESTING Prompt

**File:** `ai-prompts/testing.md`

```markdown
## Prompt 1: Generate State Machine Test Cases

**Prompt Summary:** Generate comprehensive unit tests for state machine

**Prompt Text:**
```
Generate JUnit test cases for StateTransitionValidator:

Test all valid transitions:
- Open→InProgress ✓
- Open→Cancelled ✓
- InProgress→Resolved ✓
- InProgress→Cancelled ✓
- Resolved→Closed ✓

Test all invalid transitions:
- Open→Resolved ✗
- Open→Closed ✗
- InProgress→Open ✗
- Resolved→* ✗
- Closed→* ✗
- Cancelled→* ✗

Also test: getValidNextStates(status) method

Use parameterized tests for readability.
```

**AI Response Summary:**
Claude generated 22 test cases covering all transitions plus tests for getValidNextStates(). Used @ParameterizedTest for clean code. All tests included proper assertions and clear test names.

**What I Accepted:**
- Test structure (parameterized tests)
- All test case combinations
- Test naming conventions
- Assertion patterns

**What I Changed:**
- Added @Test(expected=Exception.class) for invalid transitions
- Specified exact exception types
- Added comment explaining state machine rules in test class

**What I Rejected:**
- None - excellent test code

**Status:** Complete, all 22 tests passing
```

---

### Example 5: DEBUGGING Prompt

**File:** `ai-prompts/debugging.md`

```markdown
## Issue 1: JCR Query Not Finding Tickets

**Prompt Summary:** Troubleshoot JCR query returning empty results

**Prompt Text:**
```
I'm getting empty results from this JCR query:

SELECT * FROM [cq:Page]
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets])
  AND [jcr:content/jcr:title] LIKE '%payment%'

Query works in JCR console but not in code. Path exists and tickets exist.
What could be wrong?
```

**AI Response Summary:**
Claude identified that query escaping might be needed for special characters, suggested debugging steps: (1) check ResourceResolver session, (2) verify query syntax with JCR console, (3) check if results iterator is being properly consumed, (4) verify property names match actual JCR structure.

**Investigation Steps Suggested by AI:**
1. Log the query being executed
2. Check ResourceResolver permissions
3. Verify property names with JCR console
4. Test with simpler query first

**How AI Helped:**
Suggested systematic debugging approach; identified that it was likely a property name mismatch, not query syntax.

**What I Found:**
Property was stored as `title` not `jcr:title`. Query worked after fixing property name.

**Status:** Complete, issue resolved
```

---

### Example 6: CODE-REVIEW Prompt

**File:** `ai-prompts/code-review.md`

```markdown
## Prompt 1: Security Review of Sling Servlet

**Prompt Summary:** Review servlet for security vulnerabilities

**Prompt Text:**
```
Review this Sling servlet for security issues:

[Code snippet pasted]

Concerns to check:
- Input validation
- XSS prevention
- Injection attacks
- Secrets/credentials
- Authorization checks

Any vulnerabilities?
```

**AI Response Summary:**
Claude identified 1 security issue: missing HTML escaping in error responses could allow XSS. Suggested using Sling's built-in escaping utilities.

**Issues Found by AI:**
1. Missing XSS escaping in error JSON responses - VALID

**My Validation:**
Confirmed issue by checking if untrusted user input could appear in error message. Yes, it could. Fixed by using Text.escape() on error messages.

**What I Implemented:**
Added proper output escaping for all user-provided data in responses.

**Status:** Complete, security issue fixed
```

---

### Example 7: DOCUMENTATION Prompt

**File:** `ai-prompts/documentation.md`

```markdown
## Prompt 1: Create API Contract Documentation

**Prompt Summary:** Generate API endpoint documentation

**Prompt Text:**
```
Create API contract documentation for these endpoints:

GET /bin/api/tickets - list with search/filter
POST /bin/api/tickets - create new ticket
GET /bin/api/tickets/{id} - get detail
PUT /bin/api/tickets/{id} - update fields
PUT /bin/api/tickets/{id}/status - change status
GET /bin/api/tickets/{id}/comments - list comments
POST /bin/api/tickets/{id}/comments - add comment

Include: request, response, validation rules, error cases
```

**AI Response Summary:**
Claude generated comprehensive documentation for all 7 endpoints with request/response examples, validation rules per field, error responses (400, 404, 409, 500), and status code explanations.

**What I Accepted:**
- Overall structure and format
- Example request/response payloads
- Error case documentation
- Validation rule descriptions

**What I Changed:**
- Made error examples more specific to our domain
- Added validation rule details (field lengths, enums)
- Enhanced status code explanations with business context
- Added specific error message examples

**Status:** Complete, documentation ready
```

---

## Recording Tips

### ✅ DO THIS

1. **Record immediately after asking Claude**
   - Write prompt summary right away
   - Don't wait until end of day (you'll forget)
   - Takes only 30 seconds

2. **Include specific details**
   - What problem were you solving?
   - Why did you ask this question?
   - What was your hypothesis?

3. **Be honest about changes**
   - Note what you had to fix
   - Explain WHY you changed it
   - This shows critical thinking

4. **Show your validation**
   - How did you test it?
   - What made you confident?
   - What surprised you?

5. **Record rejections with reasoning**
   - Why didn't you use AI's suggestion?
   - What was better about your approach?
   - Were you being too cautious or appropriately skeptical?

### ❌ DON'T DO THIS

1. Don't skip prompt recording
   - Makes it look like you didn't use AI
   - Loses evidence of your process
   - Assessment specifically requires prompt history

2. Don't copy-paste blindly without notes
   - Record what you changed
   - Shows you reviewed the code
   - Demonstrates ownership

3. Don't be vague
   - "Worked great" is not useful
   - "Fixed validation bug" explains what, not why
   - Be specific: "Changed error message to include valid next states because..."

4. Don't wait to finish before recording
   - You'll forget the details
   - Takes 30 seconds now vs 5 minutes later
   - You might lose the thread of your thinking

---

## Minimum Recording Standards

Each prompt record should include:

- ✅ Summary (one line)
- ✅ Prompt text (what you actually asked)
- ✅ Response summary (what AI returned)
- ✅ What you accepted (with reasons)
- ✅ What you changed (with reasons)
- ✅ What you rejected (with reasons)

---

## File-by-File Quick Start

### When implementing a feature:

1. **First thing:** Go to `ai-prompts/implementation.md`
2. **Add section for new prompt**
3. **Describe what you're building**
4. **Paste AI response summary**
5. **After testing, record: accepted/changed/rejected**

### When debugging:

1. **Go to:** `ai-prompts/debugging.md`
2. **Describe the problem**
3. **Record AI's suggestions**
4. **Record what actually fixed it**
5. **Note if it was AI's suggestion or your own finding**

### When reviewing code:

1. **Go to:** `ai-prompts/code-review.md`
2. **Describe what you reviewed**
3. **Record findings from AI**
4. **Record findings from yourself**
5. **Note what you actually fixed**

---

## Sample Workflow for Today

### 9:00 AM - Start Implementation
```
ai-prompts/implementation.md → Add "Prompt 1: Generate TicketService"
Record: prompt text, response summary
Then: Generate code and test
Later: Record what accepted/changed/rejected
```

### 11:00 AM - Hit a Bug
```
ai-prompts/debugging.md → Add "Issue 1: Query returning empty"
Record: problem description, AI suggestions
Then: Debug and find root cause
Record: what actually was wrong, if AI was right
```

### 2:00 PM - Code Review
```
ai-prompts/code-review.md → Add "Prompt N: Review TicketService"
Record: code snippet reviewed, AI findings
Then: Implement fixes
Record: what was actually wrong, fixes applied
```

### 4:00 PM - Update Docs
```
ai-prompts/documentation.md → Add "Prompt N: Generate API docs"
Record: what you asked, response summary
Then: Refine and customize
Record: what you changed and why
```

---

## Assessment Expectations

The assessment will look at:

1. **Comprehensiveness** - Did you record prompts across all activities?
2. **Honesty** - Did you admit when AI was wrong or you changed things?
3. **Thoughtfulness** - Did you explain your reasoning?
4. **Iteration** - Do your notes show refinement and improvement?
5. **Ownership** - Did you validate and own the output?

**Strong prompt history shows:**
- You used AI thoughtfully across the lifecycle
- You validated before accepting code
- You made independent judgments
- You understood what was generated
- You could explain your decisions

---

## Checklist Before Final Submission

- [ ] All 7 activity categories have at least one prompt
- [ ] Each prompt has: summary, text, response, accepted/changed/rejected
- [ ] Rejected items explain reasoning
- [ ] Changed items explain why
- [ ] Shows iteration (follow-up prompts, refinements)
- [ ] Demonstrates validation (testing, cross-referencing)
- [ ] No "perfect first draft" prompts (shows critical thinking)
- [ ] Evidence of learning (later prompts better than earlier ones)

---

## Template You Can Copy

Keep this open while working. Copy and paste for each new prompt:

```markdown
## Prompt X: [Title]

**Date:** [Date]

**Prompt Summary:** [One line]

**Prompt Text:**
```
[Your prompt]
```

**AI Response Summary:**
[What you got]

**What I Accepted:**
- [Item 1]
- [Item 2]

**What I Changed:**
- [Change 1] - [reason]

**What I Rejected:**
- [Item] - [reason]

**Status:** Complete
```

---

## Questions to Ask Yourself for Each Prompt

1. **Why did I ask this?** → Record context
2. **Was the response helpful?** → Record what worked
3. **Did I use it as-is?** → Record what you changed
4. **Why didn't I use some suggestions?** → Record what you rejected and why
5. **How did I validate it?** → Record your testing approach
6. **Would I do it differently next time?** → Record learnings

---

## Bottom Line

**Record prompts as you work. 30 seconds per prompt. 7 activity files. That's it.**

This is not extra work - it's **evidence of your thinking**. The assessment wants to see:
- How you use AI (across lifecycle)
- How you validate (testing, reasoning)
- How you own the result (what you changed and why)

Your prompt history is one of the most important deliverables.

**Start now. Record everything. Show your thinking. ✅**
