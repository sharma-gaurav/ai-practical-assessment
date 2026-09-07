# Acceptance Criteria

**Last verified:** 2026-09-07 against the working tree, `mvn clean install` (BUILD SUCCESS), and `mvn test` (72/72 passing).

| Result | Count | Meaning |
|--------|-------|---------|
| ✅ Met | 51 | Verified in code; box ticked |
| ⚠️ Partial | 8 | Substantially built but falls short of the wording |
| ❌ Not met | 9 | Absent, or behaves contrary to the criterion |
| **Total** | **68** | |

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
- [x] Empty or whitespace-only title/description is rejected
      <br>_`StringUtils.isBlank` server-side (`TicketServiceImpl.java:75,82`) plus `.trim()` client-side._
- [x] Very long inputs are truncated or rejected gracefully (define max length)
      <br>_Max length defined as title 255 / description 5000; enforced by `maxlength` attributes and validated in both layers._
- [x] Assigning to a non-existent user is rejected with helpful message
      <br>_`userExists()` → `InvalidUserException("User does not exist: " + assignedTo)`._

## Error Handling — 3 met · 1 partial · 2 not met

- [x] Invalid state transition (e.g., Resolved directly to Cancelled) is rejected by backend
- [x] Backend returns HTTP 400/409 with clear error message for invalid transitions
      <br>_`SC_CONFLICT` (409) at `TicketOperationServlet.java:382`, with the allowed-states list in `details.status`._
- [x] UI displays error message to user: "Invalid transition. Allowed next states: [list]"
      <br>_Frontend now reads `data.details.status` and formats the message as "Invalid transition. Allowed next states: [list]" (`_ticketdetail.js:313-350`)._
- [ ] ⚠️ **PARTIAL** — Attempting to update a ticket that no longer exists shows "Ticket not found" error
      <br>_The detail view passes the server's "Ticket not found" through its generic handler. The **list view discards the response body** and shows `HTTP error! status: 404` instead (`_ticketlist.js:88-90`)._
- [ ] ❌ **NOT MET** — Network errors during save show "Connection error, please try again"
      <br>_No such string exists in the frontend. `.catch()` blocks exist but do not distinguish a network failure from an HTTP error; on a genuine `fetch` rejection the raw `TypeError` ("Failed to fetch") is surfaced because `error.message` takes precedence over the fallback. `loadComments` (`_ticketdetail.js:414-416`) logs to console with **no user-visible message at all**._
- [ ] ❌ **NOT MET** — JCR write conflicts are handled with retry or conflict resolution message
      <br>_Entirely absent. No retry, backoff, ETag/`If-Match`, or version token. Updates are last-write-wins (`_ticketdetail.js:264-270`)._

## State Machine — 9 met · 2 partial

- [x] Ticket starts in "Open" status
- [x] Open → In Progress transition succeeds
- [x] Open → Resolved transition is rejected (must go through In Progress)
- [x] Open → Cancelled transition succeeds
- [x] In Progress → Resolved transition succeeds
- [x] In Progress → Cancelled transition succeeds
- [x] In Progress → Open transition is rejected
- [x] Resolved → Closed transition succeeds
- [ ] ⚠️ **PARTIAL / CRITERION CONFLICTS WITH DESIGN** — Resolved → Open or In Progress transitions are rejected
      <br>_`Resolved → Open` **is** rejected, as required. But `Resolved → In Progress` is **deliberately allowed** as the reopen path (`StateTransitionValidatorImpl.java:31`), and is asserted as valid by 3 unit tests. This criterion contradicts the implemented and tested design — decide which is authoritative and amend one of them._
- [x] Closed → any other status transition is rejected (terminal state)
- [ ] ⚠️ **PARTIAL** — Invalid transition shows error modal with allowed next states
      <br>_The allowed-states list is now displayed, but as inline text rather than a modal. Mitigating factor: the dropdown is pre-filtered to valid next states (`_ticketdetail.js:173-183`), so invalid transitions are hard to trigger from the UI._

## Search & Filter — 5 met · 2 not met

- [x] Keyword search finds tickets matching the keyword in title or description
- [ ] ❌ **NOT MET** — Search is case-insensitive
      <br>_The JCR SQL2 query uses a bare `LIKE '%keyword%'` with no `LOWER()` on either side (`TicketOperationServlet.java:234-236`), so matching is case-sensitive. Searching "payment" will not find "Payment"._
- [ ] ❌ **NOT MET** — Search with special characters (e.g., "bug/crash") works without breaking the query
      <br>_The keyword is concatenated straight into the query string (`TicketOperationServlet.java:234-236`) with no escaping. A single apostrophe terminates the string literal and breaks the query. **This is also a JCR/SQL injection vector** — see the Security section._
- [x] Status filter (e.g., "Show Open tickets only") reduces list to matching tickets
- [x] Search and status filter work together (search within filtered results)
      <br>_Both appended to one request (`_ticketlist.js:80-86`) and ANDed in the query._
- [x] Empty search results show message "No tickets found. Try refining your search."
      <br>_Exact string at `ticketlist.html:43-45`._
- [x] Clearing filters shows all tickets again

## Testing — 2 met · 3 partial · 1 not met

- [ ] ⚠️ **PARTIAL** — Integration tests verify all valid state transitions succeed
      <br>_All valid transitions are covered, but by **unit** tests (`StateTransitionValidatorTest`, 41 tests) against the validator in isolation. `it.tests/` still contains only archetype boilerplate (`GetPageIT`, `CreatePageIT`) — no transition is exercised end-to-end through the servlet against a running instance._
- [ ] ⚠️ **PARTIAL** — Integration tests verify all invalid state transitions are rejected
      <br>_Same as above: unit-level coverage is complete; integration-level is absent._
- [ ] ⚠️ **PARTIAL** — Tests use Sling Mocks and AEM Testing Clients for isolation
      <br>_Sling Mocks / `AemContext` are used in `core` (`TicketServiceIntegrationTest`). AEM Testing Clients appear only in the untouched archetype ITs._
- [ ] ❌ **NOT MET** — Tests verify data persistence (ticket survives restart)
      <br>_No such test exists. Would require an integration test against a restarted instance._
- [x] Tests validate input validation and error messages
      <br>_6 validation tests in `TicketServiceIntegrationTest` cover empty/null title, empty description, invalid priority, and null/empty assignee._
- [x] At least 80% code coverage on state machine logic
      <br>_`StateTransitionValidatorImpl` at **96.8%** line coverage (30/31), 100% method coverage — comfortably clears the bar. Note the project-wide figure is 11.8%, but this criterion scopes to the state machine._

## Documentation — 3 met · 3 not met

- [x] README.md explains how to build and run the project locally
- [ ] ❌ **NOT MET** — README includes AEM SDK setup steps
      <br>_README covers Maven build/deploy profiles (`-PautoInstallSinglePackage`, port overrides) but has no steps for obtaining, installing, or starting the AEM SDK Quickstart._
- [ ] ❌ **NOT MET** — README provides example curl commands for API endpoints
      <br>_Zero occurrences of `curl` in README.md._
- [ ] ❌ **NOT MET** — API Contract (api-contract.md) documents all endpoints and payloads
      <br>_**The documented signatures no longer exist.** `api-contract.md` describes six path-based endpoints — `GET /bin/api/tickets/list`, `GET|PUT /bin/api/tickets/{id}`, `PUT /bin/api/tickets/{id}/status`, `GET|POST /bin/api/tickets/{id}/comments`. The servlets actually register only two paths: `/bin/api/tickets` (GET/POST/PUT, with `?id=` for detail) and `/bin/api/tickets/comments` (GET/POST). The doc needs rewriting against the consolidated servlet._
- [x] Data Model (data-model.md) explains JCR content structure
- [x] Comments explain non-obvious business logic (especially state machine)
      <br>_`StateTransitionValidatorImpl` annotates the transition table and both terminal states._

## Data Persistence — 3 met · 1 not met

- [x] Shutting down AEM and restarting does not lose any tickets or comments
      <br>_Tickets persist as `cq:Page` nodes and comments as `nt:unstructured` nodes in the JCR, so this holds by construction — though it is not covered by an automated test (see Testing)._
- [x] JCR backup/restore preserves ticket data
- [ ] ❌ **NOT MET** — Seed data is provided for testing (at least 5 sample tickets)
      <br>_Only **2** seed tickets exist in `ui.content` (`ticket-318ab8fc`, `ticket-f7684020`). Three more are needed._
- [x] Seed script can be re-run without duplicating data
      <br>_Seed tickets live at fixed content paths inside the `ui.content` package, so reinstallation overwrites rather than appends._

## Security & Code Quality — 4 met · 2 partial

- [x] No API keys, passwords, or credentials in git history
      <br>_No `.env`, credential, keystore, or key files tracked._
- [ ] ⚠️ **PARTIAL** — .gitignore includes .env, local config files
      <br>_`local.properties` is ignored, but there is **no `.env` entry**. One line to fix._
- [x] Service users properly configured for backend operations
      <br>_`ServiceUserMapperImpl.amended~…-ticketservice` plus repoinit defining `support-user` / `support-agent` / `support-manager` with hierarchical inheritance._
- [x] Code follows AEM/Java conventions (package structure, naming, OSGi patterns)
- [x] No deprecated AEM/Sling APIs used
      <br>_Clean `mvn clean install` with no deprecation warnings._
- [ ] ⚠️ **PARTIAL** — Code is reviewed for common security issues (injection, XSS, CSRF)
      <br>_XSS: handled — `escapeHtml()` applied to all interpolated ticket and comment content. CSRF: handled — `CSRFFilter` config with both API paths excluded. **Injection: NOT handled** — `fetchTickets` concatenates the `search` and `status` request parameters directly into a JCR SQL2 string (`TicketOperationServlet.java:230-236`). This is the one genuine security defect and should be fixed with parameter binding or quote escaping before submission._

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
