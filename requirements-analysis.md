# Requirement Analysis

## Selected Project Option

**Support Ticket Management System** - Full-stack application for managing internal support tickets with status lifecycle, comments, and search/filter capability.

## My Understanding (in own words)

This is a ticket tracking system where internal support staff can:
- Create tickets to track issues or requests
- Track ticket progress through defined states (Open → In Progress → Resolved → Closed, or Open/In Progress → Cancelled)
- Add comments and collaborate on tickets
- Search and filter tickets to find relevant ones quickly
- Assign tickets to team members and update priority

The key complexity is the **state machine** — only certain transitions are allowed, and invalid ones must be rejected both on the backend and handled gracefully in the UI. This enforces a defined workflow.

## Functional Requirements

**User Management**
- FR1: System supports seeded users with roles (e.g., Support Agent, Manager)
- FR2: No user-management UI required; users managed via AEM's OOTB user system

**Ticket CRUD**
- FR3: User can create a ticket with title, description, priority, and assign to another user
- FR4: User can view all tickets in a list
- FR5: User can view a specific ticket's detail (title, description, priority, status, comments, assignment)
- FR6: User can update ticket fields (title, description, priority, assignee)
- FR7: User can change ticket status only through valid state transitions
- FR8: Invalid status transitions are rejected by the backend and error is shown in UI

**Comments**
- FR9: User can add a comment to any ticket
- FR10: Comments show who wrote them and when (createdBy, createdAt)

**Search & Filter**
- FR11: User can search tickets by keyword (searches title and description)
- FR12: User can filter tickets by status
- FR13: Search and filter can be combined

**Data Persistence**
- FR14: All ticket and comment data persists in AEM JCR
- FR15: Data survives application restart

## Non-Functional Requirements

**Validation & Error Handling**
- NFR1: Backend validates all required fields (title, description, priority, assignee) and rejects incomplete submissions
- NFR2: UI shows meaningful error messages for validation failures
- NFR3: Invalid state transitions return clear error responses from API

**Security**
- NFR4: No secrets or credentials committed to repository
- NFR5: Proper AEM authentication/authorization enforced (service users for backend, UI respects user context)

**Code Quality**
- NFR6: Code follows AEM and Java best practices (OSGi patterns, Sling conventions)
- NFR7: State machine logic is testable and well-documented
- NFR8: Integration tests prove state machine rules

**Performance**
- NFR9: Searches complete in reasonable time (JCR query optimization)
- NFR10: List view paginates large datasets (stretch goal)

## Assumptions

1. **User Context:** The system runs in AEM's authenticated context; current user is available from ResourceResolver
2. **JCR Persistence:** All data is stored in AEM JCR; no external database required
3. **Frontend in AEM:** UI is built as Vanilla JavaScript modules deployed via AEM's client library mechanism
4. **No Authentication Extension:** System uses AEM's OOTB user/role system; no custom auth required for Core
5. **AEM SDK Environment:** Development on local AEM as a Cloud Service SDK
6. **State Machine is Immutable:** Once defined, state transitions cannot be changed without code deployment
7. **Single Author Instance:** No concerns about distributed state synchronization in Core (noted as stretch goal to avoid)

## Clarifications (Questions for Product Owner)

1. **Ticket Reassignment:** Can a ticket be reassigned multiple times, or only once?
   - *Assumption:* Can be reassigned multiple times; createdBy and assignedTo are independent fields.

2. **Comment Visibility:** Are all comments visible to all users, or role-based visibility?
   - *Assumption:* All comments visible to anyone viewing the ticket (no comment-level access control in Core).

3. **Ticket Deletion:** Can tickets be deleted, or only closed?
   - *Assumption:* Tickets cannot be deleted; closing is the end state. Closed tickets may be reopened in Stretch.

4. **Priority Levels:** What are the exact priority values?
   - *Assumption:* HIGH, MEDIUM, LOW (extensible to numeric scale if needed).

5. **Search Scope:** Does search include only open tickets or all tickets including closed?
   - *Assumption:* Search includes all tickets; filtering by status narrows results.

6. **Bulk Operations:** Can multiple tickets be updated at once (e.g., bulk reassign)?
   - *Assumption:* Not in Core; bulk operations are Stretch.

7. **Notification/Audit:** Should the system log who changed what and when?
   - *Assumption:* Not in Core; basic createdAt/updatedAt tracking only.

## Edge Cases

1. **State Machine Edge Cases:**
   - Attempting to transition from In Progress directly to Closed (invalid — must go through Resolved)
   - Attempting to reopen a Closed ticket (invalid — Closed is terminal in Core)
   - Rapid concurrent state changes (handled via optimistic locking or conflict detection)

2. **Data Validation:**
   - Empty title, description, or priority (rejected, must show which field is invalid)
   - Assigning ticket to a non-existent user (rejected, show available users)
   - Very long title/description (truncate in list, show full in detail)

3. **Concurrency:**
   - User A updates ticket while User B's change is in flight (last-write-wins or conflict detected)
   - Ticket deleted by admin while user has detail view open (show "no longer available")

4. **Search/Filter:**
   - Searching for special characters (e.g., "bug/crash", quoted strings) — JCR query escaping
   - Empty search results — show helpful message suggesting filter changes
   - Combining incompatible filters (e.g., status=Closed AND status=Open) — handled gracefully

5. **UI State:**
   - Form errors not clearing after successful submission (clear form state)
   - Back button after delete — show list instead of stale detail
   - Real-time updates — if ticket updated elsewhere, does UI reflect this? (not in Core)