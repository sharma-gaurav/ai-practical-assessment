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
