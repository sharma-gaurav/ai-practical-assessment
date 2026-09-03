# Implementation Plan

## Overview

Build a Support Ticket Management System on AEM as a Cloud Service with a Vanilla JavaScript frontend, OSGi backend services, and JCR persistence. The system enforces a strict state machine for ticket lifecycle. Implementation follows a backend-first approach: define models → implement business logic → build API → create UI.

**Estimated Duration:** 8-12 focused hours  
**Approach:** Iterative, with validation at each layer

## Task Breakdown

### Phase 1: Architecture & Setup (1-2 hours)

**1.1 Define Content Structure**
- Design JCR node types for Ticket, Comment, and User
- Document properties: id, title, description, priority, status, assignedTo, createdBy, createdAt, updatedAt
- Define state machine transitions and validation rules
- Deliverable: `data-model.md`, JCR structure documented

**1.2 Setup Backend Bundle Structure**
- Create or update `core` module with OSGi service interfaces
- Create `TicketService` interface for CRUD operations
- Create `StateTransitionValidator` interface for state machine logic
- Create Sling Models for Ticket and Comment exposition
- Configure service user mapping in `ui.config` module for JCR permissions
- Deliverable: Maven module structure, interfaces defined, service user configured

**1.3 Setup Frontend Module**
- Ensure `ui.frontend` is properly configured for Vanilla JavaScript
- Create module structure: TicketListManager, TicketDetailEditor, TicketCreateForm, CommentManager, StatusControl
- Setup DOM-based state management with data attributes and variables
- Deliverable: Vanilla JS module scaffolding, build configured

### Phase 2: Backend Implementation (3-4 hours)

**2.1 Implement Data Models**
- Create JCR-backed Ticket model with getters for all properties
- Create Comment model
- Implement proper serialization for REST API
- Write unit tests for models
- Deliverable: Sling Models, JUnit tests (80%+ coverage)

**2.2 Implement Business Logic Services**
- Implement `TicketService` with methods: create, read, update, delete, list, search, filterByStatus
- Implement `StateTransitionValidator` with rules for valid transitions
- Implement `CommentService` for adding comments
- Write integration tests for all services
- **Test State Machine:** Valid transitions succeed, invalid transitions throw exceptions
- Deliverable: OSGi Services, integration tests passing

**2.3 Implement REST API Endpoints**
- Create Sling Servlets for:
  - GET `/bin/api/tickets` — list all tickets with optional search/filter
  - POST `/bin/api/tickets` — create new ticket
  - GET `/bin/api/tickets/{id}` — get ticket detail
  - PUT `/bin/api/tickets/{id}` — update ticket fields
  - PUT `/bin/api/tickets/{id}/status` — change status (validated)
  - GET `/bin/api/tickets/{id}/comments` — list comments
  - POST `/bin/api/tickets/{id}/comments` — add comment
- Validate input, return appropriate HTTP status codes
- Deliverable: All endpoints functional, tested with curl/Postman

**2.4 Seed Data**
- Create script to populate initial users and sample tickets
- Ensure script is idempotent (can re-run without duplication)
- Deliverable: Seed data loaded, sample tickets visible in list

### Phase 3: Frontend Implementation (2-3 hours)

**3.1 Build Ticket List View**
- Display all tickets in a table/grid with title, status, priority, assignee
- Implement keyword search input
- Implement status filter dropdown
- Click ticket to open detail view
- Show error states for failed loads
- Deliverable: List view functional, styled

**3.2 Build Ticket Detail View**
- Display full ticket information
- Show all comments in chronological order
- Add "Comment" form
- Show "Edit Ticket" form for updating fields
- Implement status change dropdown with validation
- Show error messages for invalid transitions
- Deliverable: Detail view functional, state machine respected

**3.3 Build Ticket Create Form**
- Form with fields: title, description, priority, assignee
- Client-side validation with error messages
- Submit button that calls API
- Show success/error response
- Redirect to list or detail view on success
- Deliverable: Create flow working end-to-end

**3.4 Polish UI**
- Ensure responsive design (mobile-friendly)
- Add loading spinners during API calls
- Ensure accessibility (ARIA labels, keyboard navigation)
- Style consistently with AEM design patterns
- Deliverable: UI polished, no console errors

### Phase 4: Testing & Validation (1-2 hours)

**4.1 Integration Tests**
- Test state machine transitions (valid and invalid)
- Test search and filter combinations
- Test error handling for edge cases
- Use AEM Testing Clients to test against running AEM
- Deliverable: `it.tests/` module with passing tests

**4.2 End-to-End Tests**
- Use Cypress to test full user flows
- Test create → update → search → filter flow
- Test invalid state transitions show errors
- Test form validation messages
- Deliverable: `ui.tests/` with passing Cypress tests

**4.3 Manual Testing**
- Create a new ticket, verify it appears in list
- Search for ticket by keyword, verify results
- Filter by status, verify only matching tickets shown
- Update ticket fields, verify changes persist
- Add comment, verify it appears on detail view
- Test invalid state transition, verify error shown
- Restart AEM, verify data still exists
- Deliverable: Testing notes, screenshot/logs of validation

### Phase 5: Documentation & Reflection (1 hour)

**5.1 Documentation**
- Update README.md with setup and run instructions
- Create API Contract (api-contract.md) documenting all endpoints
- Create Data Model (data-model.md) documenting JCR structure
- Create UI Flow (ui-flow.md) explaining user journeys
- Deliverable: All docs complete and accurate

**5.2 Review & Reflect**
- Review code for quality, security, maintainability
- Document any issues found and how they were fixed
- Write reflection on AI usage and workflow
- Organize prompt history by activity
- Deliverable: review-notes.md, debugging-notes.md, reflection.md, prompt history organized

## Milestones

| Milestone | Target | Success Criteria |
|-----------|--------|------------------|
| M1: Backend Models Ready | End of Phase 1 | Sling Models compile, unit tests pass |
| M2: Services Implemented | Mid Phase 2 | All business logic tests pass, state machine validated |
| M3: API Functional | End of Phase 2 | All endpoints tested with curl, integration tests pass |
| M4: Frontend Scaffolding | Start of Phase 3 | Components created, build succeeds |
| M5: Core Flows Working | End of Phase 3 | Create/read/update/search/filter/comment flows functional |
| M6: Testing Complete | End of Phase 4 | Integration and E2E tests passing, edge cases covered |
| M7: Documentation & Review | End of Phase 5 | All artifacts complete, code reviewed, reflection written |

## AI Usage Plan

### How AI Will Help

1. **Requirement Clarification:** Validate my understanding of the state machine and edge cases
2. **Design:** Suggest OSGi service patterns, Sling Model best practices, JCR query optimization
3. **Code Generation:** Generate service implementations, Sling servlets, Vanilla JS modules
4. **Testing:** Generate JUnit/integration test cases, especially for state machine validation
5. **Debugging:** Help identify issues (bundle resolution, JCR query problems, Vanilla JS bugs)
6. **Code Review:** Validate code against AEM best practices, security concerns
7. **Documentation:** Help organize prompt history and write clear API contracts

### How I Will Validate AI Output

- Compile and test generated code immediately
- Cross-reference suggestions against official AEM docs
- Review for proper OSGi lifecycle and Sling conventions
- Check for security vulnerabilities (hardcoded values, insecure queries, XSS risks)
- Run tests after code generation to catch issues early
- Manually test user flows before accepting code as complete

### Prompts I Will Use

- "Generate an OSGi service for Ticket CRUD that uses ResourceResolver and JCR queries"
- "What are valid state transitions for this state machine? Generate validation logic"
- "Create integration tests that prove valid transitions succeed and invalid ones fail"
- "Generate Vanilla JavaScript modules for listing and filtering tickets"
- "Review this Sling query for performance and security issues"

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|-----------|
| **JCR Query Performance** | Medium | Slow searches with large datasets | Write optimized queries, test with seed data, use indexes |
| **State Machine Edge Cases** | High | Unexpected transition rules | Document transitions, test thoroughly with grid, ask AI to validate |
| **Vanilla JS/AEM Integration** | Medium | JavaScript not working in AEM context | Use AEM's client library patterns, test in AEM UI early |
| **Bundle Resolution Issues** | Low | Services not available at runtime | Use proper OSGi dependencies, check logs frequently |
| **Scope Creep** | Medium | Stretch features overshadow Core | Stick to MVP, save enhancements for Stretch section |
| **Time Management** | Medium | Running out of time before deadline | Track progress, adjust if needed, prioritize testing |

## Mitigation

1. **JCR Performance:** Use AEM's query builder, test queries against sample data, monitor logs for slow queries
2. **State Machine:** Create a transition matrix early, validate with AI, write tests for each rule
3. **Vanilla JS/AEM:** Deploy to local AEM SDK frequently, not just at the end
4. **Bundle Resolution:** Check AEM error logs regularly, use Dependency Analyzer
5. **Scope:** Define Core strictly, document Stretch ideas separately, resist adding features mid-sprint
6. **Time:** Review remaining hours at end of each phase, adjust next phase if behind

## Success Criteria for Completion

- ✓ All acceptance criteria met
- ✓ Core functionality working end-to-end
- ✓ State machine thoroughly tested
- ✓ Code follows AEM/Java conventions
- ✓ All required documentation present
- ✓ No secrets in git history
- ✓ Prompt history organized and shows iteration
- ✓ Reflection demonstrates understanding and ownership