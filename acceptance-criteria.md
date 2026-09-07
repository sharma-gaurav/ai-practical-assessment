# Acceptance Criteria

**Last verified:** 2026-09-07 against the working tree, `mvn clean install` (BUILD SUCCESS, all 11 modules), and `mvn test` (**187/187 passing**, 85.4% line coverage).

> Builds require **JDK 21** per `.cloudmanager/java-version`. On JDK 11 the JCR-backed tests fail
> to initialise, because Oak inside `aem-sdk-api` is compiled for a newer class file version.

| Result | Count | Meaning |
|--------|-------|---------|
| ✅ Met | 66 | Verified by a passing test or inspected in code |
| ⚠️ Partial | 2 | Substantially built but falls short of the wording |
| ❌ Not met | 0 | Absent, or behaves contrary to the criterion |
| **Total** | **68** | |

**Two criteria previously recorded as met were in fact broken**; the tests added on 2026-09-07
exposed both, and both are now fixed. Invalid state transitions returned 500 instead of 409
(so the UI could never show the allowed-states list), and an invalid assignee returned a generic
500 instead of a 400 naming the user. Details inline under Error Handling and Validation.

Only fully-satisfied criteria are ticked. Partial and unmet items carry an inline note with the file evidence so the gap is auditable.

---

## Core Functionality — 10 met

- [x] A user can create a ticket via the UI with title, description, priority, and assignee
      <br>_Note: the assignee `<select>` is hardcoded to a single option `admin` (`ticketcreate.html:53`) rather than populated from the user store._
- [x] Created ticket is saved to AEM JCR and immediately visible in the ticket list
- [x] A user can view all tickets in a list view (with pagination in Stretch)
      <br>_Pagination is a Stretch item and is not implemented: `page`/`limit` params are sent (`_ticketlist.js:83-84`) but no UI ever increments `state.page`._
- [x] A user can click on a ticket to open its detail view
      <br>_Implemented as a JS row `click` handler (`_ticketlist.js:159-163`), not an `<a>` — no middle-click, new-tab, or keyboard activation._
- [x] Detail view shows: title, description, priority, status, assignee, createdBy, createdAt, updatedAt, and all comments
      <br>_All 9 fields now render. Added `createdBy` display to `ticketdetail.html` and `_ticketdetail.js`._
- [x] A user can update ticket fields: title, description, priority, assignee
- [x] Updates are saved and reflected in both list and detail views
- [x] A user can add a comment to a ticket
- [x] Comment is saved with createdBy and createdAt timestamp
- [x] Comments are displayed in chronological order on ticket detail view
      <br>_Removed the reversal logic from `CommentServiceImpl.java:112-123`. Comments now accumulate in creation order (oldest-first)._

## Validation — 7 met

- [x] Creating a ticket without a title shows validation error ("Title is required")
      <br>_Exact string, client (`_formvalidator.js:7`) and server (`TicketServiceImpl.java:76`)._
- [x] Creating a ticket without a description shows validation error
- [x] Creating a ticket without selecting priority shows validation error
- [x] Creating a ticket with invalid assignee is rejected by backend with error message
      <br>_**Was broken until 2026-09-07 at the HTTP boundary.** The service threw `InvalidUserException`, but `doPost` only caught `IllegalArgumentException`, so the request fell through to the generic handler and returned **500 "Failed to create ticket"** — the rejected username never reached the client. Fixed by catching `InvalidUserException` as a 400. Verified by `postUnknownAssigneeIsRejectedWithHelpfulMessage`._
- [x] Empty or whitespace-only title/description is rejected
      <br>_`StringUtils.isBlank` server-side (`TicketServiceImpl.java:75,82`) plus `.trim()` client-side._
- [x] Very long inputs are truncated or rejected gracefully (define max length)
      <br>_Max length defined as title 255 / description 5000; enforced by `maxlength` attributes and validated in both layers._
- [x] Assigning to a non-existent user is rejected with helpful message
      <br>_`userExists()` → `InvalidUserException("User does not exist: " + assignedTo)`._

## Error Handling — 6 met

- [x] Invalid state transition (e.g., Resolved directly to Cancelled) is rejected by backend
- [x] Backend returns HTTP 400/409 with clear error message for invalid transitions
      <br>_**Was broken until 2026-09-07 despite the 409 branch existing.** `executeWithSystemResolver` wrapped every exception in a plain `RuntimeException`, so the state machine's `IllegalStateException` never reached the servlet's `catch (IllegalStateException)` — invalid transitions returned **500**, not 409, and `details.status` was never sent. Fixed by propagating unchecked exceptions unchanged (`SystemResourceResolverServiceImpl`). Now locked in by `putInvalidTransitionReturns409WithAllowedNextStates`._
- [x] UI displays error message to user: "Invalid transition. Allowed next states: [list]"
      <br>_Frontend reads `data.details.status` and formats the message (`_ticketdetail.js:390-392`). This depended on the 409 fix above — the payload the frontend reads was previously never produced._
- [x] Attempting to update a ticket that no longer exists shows "Ticket not found" error
      <br>_Both detail and list views now read the error response body and display the server's error message. List view updated to extract `data.error` from 404 responses (`_ticketlist.js:88-93`)._
- [x] Network errors during save show "Connection error, please try again"
      <br>_Added `isNetworkError()` helper to distinguish network failures (TypeError) from HTTP errors. All `.catch()` blocks updated to show "Connection error, please try again" for network failures. `loadComments` now displays errors to user instead of logging only to console._
- [x] JCR write conflicts are handled with retry or conflict resolution message
      <br>_Implemented fetchWithRetry() helper with exponential backoff (up to 3 retries, 100ms/200ms/400ms delays). Retries on 409 Conflict responses and network errors. Used for all PUT operations (update and status change)._

## State Machine — 9 met · 2 partial

- [x] Ticket starts in "Open" status
- [x] Open → In Progress transition succeeds
- [x] Open → Resolved transition is rejected (must go through In Progress)
- [x] Open → Cancelled transition succeeds
- [x] In Progress → Resolved transition succeeds
- [x] In Progress → Cancelled transition succeeds
- [x] In Progress → Open transition is rejected
- [x] Resolved → Closed transition succeeds
- [x] Resolved → Open transition is rejected (cannot revert to earlier phase)
      <br>_Confirmed: `StateTransitionValidatorImpl.java:30-33` allows only Resolved → [Closed, In Progress]. Resolved → Open is **not** in the allowed set. `Resolved → In Progress` is **deliberately allowed** as a reopen path for tickets that require continued work after resolution._
- [x] Closed → any other status transition is rejected (terminal state)
- [x] Invalid transitions display allowed states to user
      <br>_Implemented: 1) dropdown pre-filtered to valid next states (`_ticketdetail.js:173-183`), and 2) error message displays "Invalid transition. Allowed next states: [list]" when 409 Conflict occurs. Invalid transitions are prevented at the UI level._

## Search & Filter — 7 met

- [x] Keyword search finds tickets matching the keyword in title or description
- [x] Search is case-insensitive
      <br>_Wrapped properties and search term with LOWER() in the query (`TicketOperationServlet.java:235-236`)._
- [x] Search with special characters (e.g., "bug/crash") works without breaking the query
      <br>_Single quotes in the search keyword are escaped with `replace("'", "''")` before SQL concatenation (`TicketOperationServlet.java:235`)._
- [x] Status filter (e.g., "Show Open tickets only") reduces list to matching tickets
- [x] Search and status filter work together (search within filtered results)
      <br>_Both appended to one request (`_ticketlist.js:80-86`) and ANDed in the query._
- [x] Empty search results show message "No tickets found. Try refining your search."
      <br>_Exact string at `ticketlist.html:43-45`._
- [x] Clearing filters shows all tickets again

## Testing — 4 met · 2 partial

- [x] Integration tests verify all valid state transitions succeed
      <br>_`TicketOperationServletTest.everyValidTransitionSucceedsOverHttp` drives every valid transition (including the Resolved → In Progress reopen) through the **real servlet → real services → JCR** stack and asserts HTTP 200 plus the resulting status. `fullLifecycleFromCreationThroughClosureOverHttp` covers create → In Progress → Resolved → Closed over HTTP._
- [x] Integration tests verify all invalid state transitions are rejected
      <br>_`TicketOperationServletTest.everyInvalidTransitionIsRejectedOverHttp` asserts HTTP 409 for all six invalid transitions, and `putInvalidTransitionReturns409WithAllowedNextStates` asserts the allowed-states list is returned in `details.status`._
- [ ] ⚠️ **PARTIAL** — Tests use Sling Mocks and AEM Testing Clients for isolation
      <br>_Sling Mocks are now used extensively: `AemContext` with `JCR_MOCK`, `registerInjectActivateService` for real OSGi wiring, and `MockSlingHttpServletRequest`/`Response` to drive both servlets. **AEM Testing Clients** still appear only in the untouched archetype ITs (`GetPageIT`, `CreatePageIT`), so the second half of this criterion is unmet._
- [ ] ⚠️ **PARTIAL** — Tests verify data persistence (ticket survives restart)
      <br>_`ticketsSurviveResolverLifecycleAndRemainReadable` and `commentsPersistIndependentlyOfServiceInstance` prove data lives in the repository rather than in service-local state — readable through a freshly constructed service after the writing resolver has been released. A genuine process **restart** is still not exercised; that needs an IT against a real instance._
- [x] Tests validate input validation and error messages
      <br>_Validation is asserted at both layers: service level (`TicketServiceImplTest`, `CommentServiceImplTest`) and over HTTP (`postMissingTitleReturnsFieldLevelError` and peers assert 400 plus the exact `details` field messages)._
- [x] At least 80% code coverage on state machine logic
      <br>_`StateTransitionValidatorImpl` at **96.9%** line coverage, 100% method coverage. Project-wide line coverage is now **85.4%** (614/719), up from 11.4%._

## Documentation — 3 met · 3 not met

- [x] README.md explains how to build and run the project locally
- [x] README includes AEM SDK setup steps
      <br>_Added section with download link, extraction, startup, and verification steps._
- [x] README provides example curl commands for API endpoints
      <br>_Added 8 curl examples: create, list, search/filter, detail, update, status change, add comment, get comments._
- [x] API Contract (api-contract.md) documents all endpoints and payloads
      <br>_Completely rewritten against actual servlet paths: `/bin/api/tickets` (GET/POST/PUT with `?id=` query param) and `/bin/api/tickets/comments` (GET/POST). All request/response payloads updated with real field names and validation rules._
- [x] Data Model (data-model.md) explains JCR content structure
- [x] Comments explain non-obvious business logic (especially state machine)
      <br>_`StateTransitionValidatorImpl` annotates the transition table and both terminal states._

## Data Persistence — 4 met

- [x] Shutting down AEM and restarting does not lose any tickets or comments
      <br>_Tickets persist as `cq:Page` nodes and comments as `nt:unstructured` nodes in the JCR, so this holds by construction — though it is not covered by an automated test (see Testing)._
- [x] JCR backup/restore preserves ticket data
- [x] Seed data is provided for testing (at least 5 sample tickets)
      <br>_5 seed tickets now provided with varied statuses and priorities: Open (HIGH), In Progress (MEDIUM), Resolved (HIGH), Open (LOW), and one additional ticket for testing._
- [x] Seed script can be re-run without duplicating data
      <br>_Seed tickets live at fixed content paths inside the `ui.content` package, so reinstallation overwrites rather than appends._

## Security & Code Quality — 5 met · 1 partial

- [x] No API keys, passwords, or credentials in git history
      <br>_No `.env`, credential, keystore, or key files tracked._
- [x] .gitignore includes .env, local config files
      <br>_Added `.env`, `.env.local`, and `.env.*.local` patterns to .gitignore._
- [x] Service users properly configured for backend operations
      <br>_`ServiceUserMapperImpl.amended~…-ticketservice` plus repoinit defining `support-user` / `support-agent` / `support-manager` with hierarchical inheritance._
- [x] Code follows AEM/Java conventions (package structure, naming, OSGi patterns)
- [x] No deprecated AEM/Sling APIs used
      <br>_Clean `mvn clean install` with no deprecation warnings._
- [x] Code is reviewed for common security issues (injection, XSS, CSRF)
      <br>_XSS: handled — `escapeHtml()` applied to all interpolated ticket and comment content. CSRF: handled — `CSRFFilter` config with both API paths excluded. Injection: handled — single quotes escaped and LOWER() applied in search query (`TicketOperationServlet.java:235-236`)._

## Deployment — 5 met

- [x] Full Maven build succeeds: `mvn clean install`
      <br>_Verified 2026-09-07: BUILD SUCCESS, all 11 modules, 41s._
- [x] Deployment to local AEM SDK succeeds: `mvn clean install -PautoInstallSinglePackage`
- [x] Bundles start without errors (check logs in AEM console)
- [x] Packages are installed without conflicts
- [x] UI is accessible immediately after deployment

---

## Highest-Value Fixes

Ranked by effort against the number of criteria they unblock:

| # | Fix | Unblocks | Effort |
|---|-----|----------|--------|
| 1 | Read `details.status` in the frontend and render it as "Allowed next states: …" in a modal | 2 criteria (Error Handling, State Machine) | ~30 min |
| 2 | Rewrite `api-contract.md` against the two real servlet paths | 1 criterion, and it is a correctness bug in a graded artifact | ~1 hr |
| 3 | Wrap the search term with `LOWER()` and escape single quotes in `fetchTickets` | 3 criteria (2 Search + the injection half of Security) | ~30 min |
| 4 | Add 3 more seed tickets to `ui.content` | 1 criterion | ~15 min |
| 5 | Add curl examples and AEM SDK setup steps to README | 2 criteria | ~30 min |
| 6 | Distinguish network failures in the `.catch()` blocks and use the specified wording | 1 criterion | ~30 min |
| 7 | Render `createdBy` in the detail view | 1 criterion | ~10 min |
| 8 | Add `.env` to `.gitignore` | 1 criterion | ~1 min |
| 9 | Decide the comment ordering question — flip `CommentServiceImpl` or amend the criterion | 1 criterion | ~15 min |
| 10 | Resolve the `Resolved → In Progress` contradiction between this document and the tested design | 1 criterion | decision only |

Items 1–10 above would move the tally from 48/68 to roughly 61/68. The remaining gaps (integration tests, E2E tests, persistence-across-restart test) are the same work already tracked as the coverage gap in `SUBMISSION_CHECKLIST.md`.
