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

## Prompt 4: [To be documented]

[Follow same format]

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
