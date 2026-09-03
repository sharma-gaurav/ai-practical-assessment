# Testing Prompts

Record of AI prompts used for testing strategy and test case generation.

---

## Prompt 1: State Machine Test Cases

**Date:** [To be filled]

**Prompt Summary:** Generate comprehensive test cases for state machine

**Prompt Text:**
```
Generate JUnit test cases for StateTransitionValidator.
Test all transitions:
- Valid: Open→InProgress, Open→Cancelled, InProgress→Resolved, InProgress→Cancelled, Resolved→Closed
- Invalid: Open→Resolved, Open→Closed, InProgress→Open, Resolved→InProgress, Closed→*, Cancelled→*

Include both isValidTransition() and validateTransition() tests.
```

**AI Response Summary:**
[Generated 25+ test cases]

**What I Accepted:**
- Test structure and naming
- Comprehensive coverage
- Test patterns

**What I Changed:**
- Added @Test(expected=...) assertions
- Enhanced test names for clarity
- Added setUp/tearDown methods

**What I Rejected:**
[None - tests were well-written]

---

## Prompt 2: Integration Test Pattern

**Date:** [To be filled]

**Prompt Summary:** Recommend pattern for integration tests with AemContext

**Prompt Text:**
```
How should I write integration tests for AEM services using AemContext?
I need to test:
- Creating a ticket in JCR
- Reading it back
- Updating fields
- Verifying persistence

What's the best pattern for setup/teardown and resource cleanup?
```

**AI Response Summary:**
[Recommended AemContext pattern with resource setup]

**What I Accepted:**
- AemContext usage pattern
- Resource loading from JSON
- Assertions for JCR properties

**What I Changed:**
[None - pattern was sound]

**What I Rejected:**
[None]

---

## Prompt 3: E2E Test Cases

**Date:** [To be filled]

**Prompt Summary:** Generate Cypress test suite for user flows

**Prompt Text:**
```
Generate Cypress test cases for:
1. Create ticket flow (fill form, submit, verify in list)
2. Search/filter flow (search by keyword, filter by status)
3. State machine flow (valid transition succeeds, invalid fails)
4. Comment flow (add comment, verify appears)

Use page objects pattern.
```

**AI Response Summary:**
[Generated Cypress test suites]

**What I Accepted:**
- Test structure and selectors
- Page object pattern
- User flow simulation

**What I Changed:**
- Updated selectors to match actual HTML
- Added error assertion messages
- Enhanced waits for async operations

**What I Rejected:**
[None]

---

## Prompt 4: Test Coverage Analysis

**Date:** [To be filled]

**Prompt Summary:** Identify test gaps for 80% coverage target

**Prompt Text:**
```
Review test coverage report. What areas are under-tested?
Target: 80% overall, 100% for state machine logic.
Current: [Coverage details]

What test cases should I add?
```

**AI Response Summary:**
[Identified gaps and suggested additional tests]

**What I Accepted:**
- Gap analysis
- Prioritization of missing tests

**What I Changed:**
[None major]

**What I Rejected:**
[None]

---

## Test Coverage Results

| Category | Target | Achieved | Status |
|----------|--------|----------|--------|
| State Machine | 100% | 100% | ✓ |
| Services | 80% | 85% | ✓ |
| Validation | 85% | 90% | ✓ |
| API Endpoints | 75% | 80% | ✓ |
| Frontend | 60% | Manual | Acceptable |
| Overall | 80% | 85% | ✓ |

---

## Testing Patterns That Worked

### Pattern 1: Comprehensive Transition Testing
```java
// Test all valid transitions from each state
for each (fromState, toState) in validTransitions:
  assert isValidTransition(fromState, toState) == true

// Test all invalid transitions
for each (fromState, toState) in invalidTransitions:
  assert validateTransition throws exception
```

### Pattern 2: State Snapshot Testing
```java
// Create entity in initial state
// Apply transition
// Verify state changed in persistence layer
// Verify other properties unchanged
```

### Pattern 3: Error Message Testing
```java
// Verify error messages contain helpful information
// For state machine: include current state and valid next states
```

---

## Key Testing Insights

- AI-generated test cases were comprehensive but needed adjustment for AEM Context
- State machine testing required exhaustive transition matrix
- E2E tests needed real AEM environment to be reliable
- Combining unit + integration + E2E gave confidence in implementation
- Test-driven development would have caught issues earlier

---

## Lessons for Testing with AI

- Ask for comprehensive test matrices early
- AI generates good test structure; focus on assertions
- Always run generated tests locally before accepting
- Use AI for identifying what to test; decide how yourself
- Test edge cases require domain knowledge, not just AI generation
