# Project Specification

Reference: This is a summary spec. Full details in root-level `requirements-analysis.md`.

---

## Project: Support Ticket Management System

### Business Goal
Enable internal teams to create, track, and collaborate on support tickets with enforcement of a defined workflow.

### Core User Stories

**Story 1: Create Ticket**
- As a support agent, I can create a new ticket with title, description, priority, and assignee
- The ticket is saved to the system and appears in the list
- Invalid/incomplete tickets are rejected with clear error messages

**Story 2: View and Search Tickets**
- As a support agent, I can view all tickets in a list
- I can search by keyword (title/description)
- I can filter by status
- Results appear quickly

**Story 3: Edit Ticket**
- As a support agent, I can update ticket title, description, priority, assignee
- Changes are saved and immediately visible
- I cannot change status (separate workflow)

**Story 4: Progress Ticket Through Workflow**
- As a support agent, I can change ticket status
- Only valid transitions allowed (enforced by state machine)
- Invalid transitions are clearly rejected with explanation
- Valid next states shown when attempting transition

**Story 5: Collaborate via Comments**
- As a support agent, I can add comments to tickets
- Comments show who wrote them and when
- Comments are visible to all who can view the ticket

---

## State Machine Rules

```
Open
├── → In Progress ✓
├── → Cancelled ✓
└── (↛ Resolved ✗, ↛ Closed ✗)

In Progress
├── → Resolved ✓
├── → Cancelled ✓
└── (↛ Open ✗, ↛ Closed ✗)

Resolved
├── → Closed ✓
└── (↛ Open ✗, ↛ In Progress ✗)

Closed (Terminal)
└── No transitions ✗

Cancelled (Terminal)
└── No transitions ✗
```

**Why:** Enforces logical workflow progression, prevents jumping steps, allows cancellation at any active stage.

---

## Technical Architecture

### Three-Tier Architecture
1. **Frontend:** Vanilla JavaScript UI for ticket management
2. **API:** Sling servlets exposing RESTful endpoints
3. **Backend:** OSGi services implementing business logic
4. **Persistence:** AEM JCR (page-based content hierarchy)

### Content Structure
- Tickets: `/content/ai-practical-assessment/tickets/<ticket-id>`
- Comments: `/content/ai-practical-assessment/tickets/<ticket-id>/jcr:content/comments/<comment-id>`

### Key Components
- TicketService: CRUD operations for tickets
- StateTransitionValidator: Enforces state machine rules
- CommentService: Manages comments
- Sling Servlets: HTTP endpoints

---

## Constraints

### Do's
- ✓ Use AEM JCR for all persistence
- ✓ Use Vanilla JavaScript (NO React)
- ✓ Use OSGi services for business logic
- ✓ Validate all input at backend
- ✓ Test state machine thoroughly
- ✓ Document all design decisions
- ✓ Show all AI usage in prompts

### Don'ts
- ✗ No external database
- ✗ No React or other JS frameworks
- ✗ No unauthenticated endpoints
- ✗ No unvalidated input
- ✗ No hardcoded credentials
- ✗ No deprecated AEM APIs
- ✗ No skipping tests

---

## Success Criteria

### Functional (Must Have)
- [ ] Create ticket with all fields
- [ ] List all tickets
- [ ] Search by keyword
- [ ] Filter by status
- [ ] View ticket detail
- [ ] Update ticket fields
- [ ] Change status (valid only)
- [ ] Add comments
- [ ] Data persists after restart

### Quality (Must Have)
- [ ] All tests pass
- [ ] State machine tested exhaustively
- [ ] Input validation complete
- [ ] Error messages helpful
- [ ] No security vulnerabilities
- [ ] Code follows conventions
- [ ] Documentation complete

### Delivery (Must Have)
- [ ] Git repository clean
- [ ] No secrets in code
- [ ] Prompt history organized
- [ ] README setup works
- [ ] README verified step-by-step

---

## Scope Definition

### Core (Required)
- Basic CRUD for tickets
- State machine with 5 states
- Comments (create only)
- Search and filter
- Unit + integration tests

### Stretch (Optional)
- Advanced filtering
- Pagination
- User management UI
- Authentication/authorization
- Bulk operations
- API documentation (Swagger)
- Docker setup

### Explicitly Out of Scope
- Real-time notifications
- User interface redesign
- Performance optimization
- Backup/restore automation
- Mobile app

---

## Definition of Done

A feature is "done" when:
1. Code written and tested locally
2. Tests pass (unit, integration, manual)
3. No console errors
4. Follows conventions
5. Documented (comments, API, design)
6. Git committed with clear message
7. Prompt history recorded
8. No secrets exposed

---

## Non-Functional Requirements

- **Performance:** API response <500ms
- **Availability:** Works in AEM SDK and Cloud Manager
- **Security:** No XSS, injection, or credential leaks
- **Maintainability:** Clean code, clear design
- **Scalability:** Handles 1000+ tickets (via indexing)
- **Testability:** 80%+ code coverage

---

## References

- Full requirements: `requirements-analysis.md`
- Design details: `design-notes.md`
- API details: `api-contract.md`
- Data details: `data-model.md`
- UI flows: `ui-flow.md`
- Testing: `test-strategy.md`
