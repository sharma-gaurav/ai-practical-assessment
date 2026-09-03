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

## Prompt 2: [To be documented]

**Date:** [To be filled]

**Prompt Summary:** [Summary]

**Prompt Text:**
```
[Actual prompt]
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
