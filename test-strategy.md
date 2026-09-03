# Test Strategy

## Overview

Comprehensive testing approach covering unit tests, integration tests, API tests, and end-to-end tests. Focus is on validating the state machine logic and ensuring data integrity.

---

## Test Scope

### What We Test

- **State Machine:** All valid transitions succeed, all invalid transitions fail
- **CRUD Operations:** Create, read, update, delete ticket functionality
- **Validation:** Input validation at backend (required fields, lengths, formats)
- **Errors:** Error handling and meaningful error messages
- **Search & Filter:** Searching and filtering work correctly
- **Comments:** Adding comments, retrieving comments
- **Data Persistence:** Tickets persist after restart
- **API Contracts:** Endpoints return correct status codes and response formats

### What We Don't Test (Out of Scope)

- Performance/load testing (Stretch goal)
- Security vulnerability scanning (external tool)
- UI rendering/CSS styling (manual verification)
- Accessibility compliance (WCAG - manual + automated)
- Real-time updates (not implemented in Core)
- Concurrent updates (out of scope for initial implementation)

---

## Test Pyramid

```
         ┌─────────────────────┐
         │  E2E Tests (5-10)   │  ← Cypress
         │  Full user flows    │
         ├─────────────────────┤
         │ API Tests (20-30)   │  ← Integration tests
         │ Endpoint behavior   │
         ├─────────────────────┤
         │ Unit Tests (50+)    │  ← JUnit
         │ Logic, utilities    │
         └─────────────────────┘
```

**Distribution:**
- 60% Unit tests (individual components)
- 30% Integration/API tests (services with JCR)
- 10% E2E tests (full user flows)

---

## Unit Tests

### Test Files Location
- `core/src/test/java/com/example/aem/ticket/` directory
- Follow Maven convention: `*Test.java` naming

### 1. StateTransitionValidator Tests

**File:** `StateTransitionValidatorTest.java`

**Test Cases:**

| Test Name | Input | Expected Output | Notes |
|-----------|-------|-----------------|-------|
| `testOpenToInProgress` | Open → In Progress | Valid | Valid transition |
| `testOpenToCancelled` | Open → Cancelled | Valid | Valid transition |
| `testOpenToResolved` | Open → Resolved | Invalid | Must go through In Progress |
| `testOpenToClosed` | Open → Closed | Invalid | Must go through In Progress + Resolved |
| `testInProgressToResolved` | In Progress → Resolved | Valid | Valid transition |
| `testInProgressToCancelled` | In Progress → Cancelled | Valid | Valid transition |
| `testInProgressToOpen` | In Progress → Open | Invalid | Cannot revert to Open |
| `testResolvedToClosed` | Resolved → Closed | Valid | Valid transition |
| `testResolvedToInProgress` | Resolved → In Progress | Invalid | Cannot revert |
| `testClosedToAny` | Closed → [any] | Invalid | Terminal state |
| `testCancelledToAny` | Cancelled → [any] | Invalid | Terminal state |
| `testGetValidNextStatesOpen` | Open | [In Progress, Cancelled] | Correct set of valid states |
| `testGetValidNextStatesInProgress` | In Progress | [Resolved, Cancelled] | Correct set of valid states |

**Example Test:**
```java
@Test
public void testOpenToInProgress() {
    StateTransitionValidator validator = new StateTransitionValidator();
    assertTrue(validator.isValidTransition("Open", "In Progress"));
}

@Test(expected = InvalidTransitionException.class)
public void testOpenToResolved() {
    StateTransitionValidator validator = new StateTransitionValidator();
    validator.validateTransition("Open", "Resolved");
}
```

### 2. Ticket Service Unit Tests (with Mocks)

**File:** `TicketServiceTest.java`

**Dependencies Mocked:**
- `ResourceResolver`
- `Resource` (mock JCR nodes)
- `ValueMap` (mock properties)

**Test Cases:**

| Test Name | Setup | Expected | Notes |
|-----------|-------|----------|-------|
| `testCreateTicketWithValidData` | Valid inputs | Ticket created | ID assigned, timestamps set |
| `testCreateTicketMissingTitle` | No title | Exception | Validation fails |
| `testCreateTicketInvalidPriority` | priority="UNKNOWN" | Exception | Only HIGH/MEDIUM/LOW allowed |
| `testCreateTicketTitleTooLong` | title > 255 chars | Exception | Length validation |
| `testUpdateTicketField` | Update title | Success | UpdatedAt changes, createdAt unchanged |
| `testUpdateTicketInvalidStatus` | status="INVALID" | Exception | Validation fails |
| `testGetTicketById` | Valid ID | Ticket object | Properties mapped correctly |
| `testGetTicketNotFound` | Invalid ID | Exception | Resource not found |
| `testListAllTickets` | Multiple tickets | List<Ticket> | All returned, sorted by createdAt DESC |
| `testSearchTickets` | keyword="payment" | Filtered list | Only matching title/description |
| `testFilterByStatus` | status="Open" | Filtered list | Only Open tickets |

### 3. Validation Tests

**File:** `ValidationUtilTest.java`

**Test Cases:**

| Test Name | Input | Valid? | Notes |
|-----------|-------|--------|-------|
| `testValidateTitle_Success` | "Payment issue" | ✓ | Normal string |
| `testValidateTitle_Empty` | "" | ✗ | Fails |
| `testValidateTitle_Whitespace` | "   " | ✗ | Fails after trim |
| `testValidateTitle_TooLong` | 256 char string | ✗ | Max 255 |
| `testValidateDescription_Success` | "Full description..." | ✓ | Normal string |
| `testValidateDescription_Empty` | "" | ✗ | Fails |
| `testValidatePriority_High` | "HIGH" | ✓ | Valid |
| `testValidatePriority_InvalidCase` | "high" | ✗ | Case sensitive |
| `testValidatePriority_Invalid` | "CRITICAL" | ✗ | Not in allowed set |

---

## Integration Tests

### Test Files Location
- `it.tests/src/main/java/com/example/aem/tests/` directory
- Uses `AemContext` for JCR simulation

### 1. State Machine Integration Tests

**File:** `StateTransitionIntegrationTest.java`

**Setup:** Create ticket in JCR, verify transitions work with actual resource writes

**Test Cases:**

| Test Name | Setup | Action | Expected | Verification |
|-----------|-------|--------|----------|--------------|
| `testTransitionOpenToInProgress` | Ticket created as Open | Update status to In Progress | Success | JCR node status property changed |
| `testTransitionValidSequence` | Start: Open | Open→In Progress→Resolved→Closed | All succeed | Each step persists to JCR |
| `testTransitionInvalidRejected` | Start: Open | Try Open→Resolved | 409 Conflict | Status unchanged in JCR |
| `testTransitionClosedTerminal` | Start: Closed | Try Closed→Resolved | 409 Conflict | Status remains Closed |
| `testConcurrentTransitionHandling` | Ticket: Open | Two requests: A→In Progress, B→In Progress | One succeeds, one fails | JCR versioning or optimistic locking |

**Example Test:**
```java
@Test
public void testTransitionOpenToInProgress() {
    // Setup
    context.load().json("/initial-ticket.json", "/content/ai-practical-assessment/tickets");
    
    Ticket ticket = context.resourceResolver()
        .getResource("/content/ai-practical-assessment/tickets/ticket-001")
        .adaptTo(Ticket.class);
    
    assertEquals("Open", ticket.getStatus());
    
    // Action
    ticketService.updateStatus(ticket.getId(), "In Progress");
    
    // Verify
    Resource resource = context.resourceResolver()
        .getResource("/content/ai-practical-assessment/tickets/ticket-001/jcr:content");
    assertEquals("In Progress", resource.getValueMap().get("status"));
}
```

### 2. CRUD Integration Tests

**File:** `TicketCrudIntegrationTest.java`

**Test Cases:**

| Test Name | Operation | Verification |
|-----------|-----------|--------------|
| `testCreateTicket` | Create with all fields | Ticket appears in JCR with correct path and properties |
| `testReadTicket` | Get existing ticket | All properties returned correctly |
| `testUpdateTicket` | Update description | Change persisted, updatedAt updated, createdAt unchanged |
| `testDeleteTicket` | Delete ticket | Resource removed from JCR |
| `testListTickets` | Create 5, list all | All 5 returned, sorted by createdAt DESC |
| `testListTicketsEmpty` | Delete all, list | Empty list returned |

### 3. Comment Integration Tests

**File:** `CommentServiceIntegrationTest.java`

**Test Cases:**

| Test Name | Setup | Action | Expected |
|-----------|-------|--------|----------|
| `testAddComment` | Ticket exists | Add comment | Comment node created under /comments folder |
| `testGetComments` | Ticket with 3 comments | Get all | All 3 returned, newest first |
| `testGetCommentsEmpty` | Ticket with 0 comments | Get | Empty list returned |
| `testCommentImmutable` | Comment created | Try to update | Fails or no-op |
| `testCommentCreatedBySet` | Add comment | Check createdBy | Set to current user |
| `testCommentTimestampSet` | Add comment | Check createdAt | Timestamp set to current time |

### 4. Search & Filter Integration Tests

**File:** `TicketSearchIntegrationTest.java`

**Setup:** Load 10 sample tickets with varied titles, descriptions, statuses

**Test Cases:**

| Test Name | Search | Filter | Expected |
|-----------|--------|--------|----------|
| `testSearchByKeyword` | "payment" | — | 3 tickets with "payment" in title/description |
| `testSearchCaseInsensitive` | "PAYMENT" | — | Same results as lowercase |
| `testSearchNoResults` | "xyz123" | — | Empty list |
| `testFilterByStatus` | — | "Open" | Only Open tickets (5/10) |
| `testSearchAndFilter` | "payment" | "Open" | "payment" tickets that are Open (2/5) |
| `testFilterStatusNotFound` | — | "Closed" | All 0 Closed tickets (if none exist) |

---

## API Tests (Endpoint Tests)

### Test Files Location
- `it.tests/src/main/java/com/example/aem/tests/api/` directory

### 1. Ticket Endpoint Tests

**Test Class:** `TicketApiTest.java`

**Preconditions:** AEM instance running, test servlets deployed

**Test Cases:**

| Method | Endpoint | Status | Verification |
|--------|----------|--------|--------------|
| GET | `/bin/api/tickets` | 200 | Returns all tickets in JSON array |
| POST | `/bin/api/tickets` (valid data) | 201 | Ticket created, location header set |
| POST | `/bin/api/tickets` (missing title) | 400 | Error response with details |
| GET | `/bin/api/tickets/ticket-001` | 200 | Ticket object returned |
| GET | `/bin/api/tickets/invalid-id` | 404 | Not found error |
| PUT | `/bin/api/tickets/ticket-001` (update field) | 200 | Ticket updated |
| PUT | `/bin/api/tickets/ticket-001/status` (valid) | 200 | Status changed |
| PUT | `/bin/api/tickets/ticket-001/status` (invalid) | 409 | Conflict error with validNextStates |

### 2. Comment Endpoint Tests

**Test Class:** `CommentApiTest.java`

**Test Cases:**

| Method | Endpoint | Status | Verification |
|--------|----------|--------|--------------|
| GET | `/bin/api/tickets/ticket-001/comments` | 200 | Array of comments |
| POST | `/bin/api/tickets/ticket-001/comments` (valid) | 201 | Comment created |
| POST | `/bin/api/tickets/ticket-001/comments` (empty) | 400 | Validation error |
| GET | `/bin/api/tickets/invalid-id/comments` | 404 | Not found |

---

## End-to-End Tests (E2E)

### Test Framework
**Cypress** (in `ui.tests/` module)

### 1. Create Ticket Flow

**Test:** `cypress/e2e/create-ticket.cy.js`

```javascript
describe('Create Ticket Flow', () => {
  it('should create a ticket and appear in list', () => {
    cy.visit('/content/ai-practical-assessment/create-ticket');
    
    cy.get('#title').type('Test Issue');
    cy.get('#description').type('This is a test issue');
    cy.get('#priority').select('HIGH');
    cy.get('#assignee').select('user-123');
    
    cy.get('button[type="submit"]').click();
    
    cy.url().should('include', '/tickets');
    cy.contains('Test Issue').should('be.visible');
  });

  it('should show validation error for missing title', () => {
    cy.visit('/content/ai-practical-assessment/create-ticket');
    
    cy.get('#description').type('Description without title');
    cy.get('button[type="submit"]').click();
    
    cy.contains('Title is required').should('be.visible');
  });
});
```

### 2. Search & Filter Flow

**Test:** `cypress/e2e/search-filter.cy.js`

```javascript
describe('Search and Filter', () => {
  beforeEach(() => {
    cy.visit('/content/ai-practical-assessment/tickets');
  });

  it('should filter tickets by status', () => {
    cy.get('#status-filter').select('Open');
    cy.get('table tbody tr').should('have.length.greaterThan', 0);
    cy.get('table tbody tr').each(row => {
      cy.wrap(row).should('contain', 'Open');
    });
  });

  it('should search tickets by keyword', () => {
    cy.get('#search-input').type('payment');
    cy.get('table tbody tr').each(row => {
      cy.wrap(row).should('contain.text', /payment/i);
    });
  });

  it('should combine search and filter', () => {
    cy.get('#search-input').type('payment');
    cy.get('#status-filter').select('Open');
    // Both conditions applied
  });
});
```

### 3. State Machine Flow

**Test:** `cypress/e2e/state-machine.cy.js`

```javascript
describe('State Machine', () => {
  it('should allow valid status transition', () => {
    cy.visitTicket('ticket-001'); // Open ticket
    cy.get('#status-change').select('In Progress');
    cy.get('button.apply-status').click();
    cy.contains('Status changed to In Progress').should('be.visible');
  });

  it('should reject invalid status transition', () => {
    cy.visitTicket('ticket-001'); // Open ticket
    cy.get('#status-change').select('Resolved'); // Invalid
    cy.contains('Invalid transition').should('be.visible');
  });
});
```

### 4. Comment Flow

**Test:** `cypress/e2e/comments.cy.js`

```javascript
describe('Comments', () => {
  it('should add comment to ticket', () => {
    cy.visitTicket('ticket-001');
    cy.get('#comment-input').type('This is a test comment');
    cy.get('button.post-comment').click();
    cy.contains('This is a test comment').should('be.visible');
  });
});
```

---

## Data Persistence Test

### Test File
- `it.tests/src/main/java/com/example/aem/tests/DataPersistenceTest.java`

**Test:** `testDataSurvivesRestart`

**Procedure:**
1. Create ticket "Test Persistence"
2. Add comment "Comment 1"
3. Shutdown AEM SDK
4. Restart AEM SDK
5. Query ticket with ID from step 1
6. Verify ticket and comment still exist with same data

**Verification:**
- Ticket data unchanged
- Comment data unchanged
- Timestamps preserved

---

## Test Coverage Goals

- **Overall:** ≥ 80% code coverage
- **State Machine Logic:** 100% coverage (all transitions tested)
- **Validation Logic:** ≥ 95% coverage
- **Error Handling:** ≥ 90% coverage
- **UI Components:** Manual testing (Cypress for critical flows)

---

## Continuous Integration

**Maven Execution:**
```bash
# Run all unit tests
mvn test

# Run integration tests
mvn verify

# Run with coverage report
mvn verify jacoco:report
```

**Expected Output:**
- All tests pass (0 failures)
- Coverage report in `target/site/jacoco/index.html`
- No deprecated API warnings

---

## Test Data

### Sample Tickets for Testing

```java
Ticket 1:
  ID: ticket-001
  Title: "Payment processing broken"
  Description: "Users cannot complete checkout"
  Priority: HIGH
  Status: Open
  AssignedTo: user-123
  CreatedBy: user-456

Ticket 2:
  ID: ticket-002
  Title: "Email notifications missing"
  Description: "Payment confirmation emails not sent"
  Priority: MEDIUM
  Status: In Progress
  AssignedTo: user-789

Ticket 3:
  ID: ticket-003
  Title: "Database timeout on reports page"
  Description: "Reports page loading slowly"
  Priority: LOW
  Status: Resolved
```

---

## Known Issues / Not Covered

- Real-time synchronization across multiple browser tabs
- Performance with 10,000+ tickets (pagination recommended in Stretch)
- Distributed transaction scenarios (not applicable to single AEM instance)
- Browser compatibility testing (manual only)
