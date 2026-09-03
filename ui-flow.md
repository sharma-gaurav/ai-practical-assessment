# UI Flow

## User Journeys

### Journey 1: Create a New Ticket

**Actor:** Support staff member

**Steps:**
1. User navigates to `/content/ai-practical-assessment/create-ticket.html`
2. Form is displayed with fields:
   - Title (text input, required)
   - Description (textarea, required)
   - Priority (dropdown: HIGH / MEDIUM / LOW, required)
   - Assignee (dropdown of users, required)
3. User fills in all fields
4. User clicks "Create Ticket" button
5. Frontend validates form (all fields required, title ≤ 255 chars, description ≤ 5000 chars)
6. If validation passes:
   - Frontend sends POST request to `/bin/api/tickets` with form data
   - Backend validates and creates ticket in JCR
   - On success (201), redirect to `/tickets` (list view)
   - Display success message: "Ticket created successfully"
7. If validation fails:
   - Display inline error messages under each field
   - Do not allow form submission
   - Example: "Title is required", "Priority must be selected"

**Edge Cases:**
- Very long title (> 255 chars) → Truncated with warning
- Special characters in title/description → Displayed and stored correctly
- User logs out during form fill → Redirect to login
- Network error during submission → Show "Connection error, please retry"

---

### Journey 2: View All Tickets (List & Search)

**Actor:** Support staff member

**Steps:**
1. User navigates to `/content/ai-practical-assessment/tickets`
2. Page loads with:
   - Search bar at top (placeholder: "Search by keyword...")
   - Status filter dropdown (All, Open, In Progress, Resolved, Closed, Cancelled)
   - Table with columns: Title, Status, Priority, Assigned To
   - All tickets displayed (sorted by newest first)
3. User types keyword in search bar (e.g., "payment")
4. JavaScript event listener triggers on input, fetches `/bin/api/tickets?search=payment`
5. Table updates with matching tickets
6. User clicks status filter dropdown and selects "Open"
7. Table updates to show only Open tickets matching search term
8. User clicks on a ticket row
9. Browser navigates to `/tickets/{ticket-id}`

**UI Elements:**
```
┌─────────────────────────────────────────────────┐
│ Ticket Management System                        │
├─────────────────────────────────────────────────┤
│ [Search: _______________]  [Status: All ▼]      │
│ [+ Create New Ticket]                           │
├─────────────────────────────────────────────────┤
│ Title              Status         Priority  To   │
├─────────────────────────────────────────────────┤
│ Payment broken     Open           HIGH     John  │ ← Clickable
│ Missing emails     In Progress    MEDIUM   Jane  │ ← Clickable
│ DB timeout         Resolved       LOW      Mike  │ ← Clickable
└─────────────────────────────────────────────────┘
```

**Edge Cases:**
- Empty search results → Show message "No tickets found"
- Search with special characters → Escape properly, don't break query
- Filter by status=Closed, then clear search → Show all closed tickets
- Click same filter twice → No change (toggling not needed)
- Large result set (100+ tickets) → Show first 20 (pagination in Stretch)

---

### Journey 3: View & Edit Ticket Detail

**Actor:** Support staff member

**Steps:**
1. User clicks on a ticket from list view
2. Browser navigates to `/tickets/{ticket-id}`
3. Page loads in AEM edit mode with:
   - Ticket title (editable field)
   - Description (editable textarea)
   - Priority (editable dropdown)
   - Status (read-only display, separate status change control)
   - Assigned To (editable dropdown)
   - Created By, Created At, Updated At (read-only)
   - Status change dropdown (showing only valid next states)
   - Comments section (list of all comments + add comment form)
4. User edits ticket title
5. User clicks "Save" button
6. Frontend validates form (required fields, length limits)
7. Frontend sends PUT request to `/bin/api/tickets/{id}` with updated fields
8. Backend validates and updates ticket in JCR
9. On success (200), display "Ticket updated successfully"
10. On error, display "Failed to update: [error message]"

**UI Elements:**
```
┌────────────────────────────────────────────┐
│ Ticket Detail                              │
├────────────────────────────────────────────┤
│ Title: [Payment processing broken      ✎] │
│ Description:                               │
│ [Users cannot complete payment...      ✎] │
│                                            │
│ Priority: [HIGH ▼]  Assigned To: [John ▼] │
│ Status: Open                               │
│ Change to: [In Progress ▼] [Apply]        │
│                                            │
│ Created By: John Smith (2026-09-01)       │
│ Updated At: 2026-09-01 14:30              │
│ [Save Changes]                            │
│                                            │
│ ── Comments ──────────────────────────────│
│ Jane Smith (2026-09-01 11:00):            │
│ "I can reproduce on staging"              │
│                                            │
│ Mike Wilson (2026-09-01 14:30):           │
│ "Fix deployed, testing prod"              │
│                                            │
│ Add Comment:                               │
│ [_____________________________] [Post]     │
└────────────────────────────────────────────┘
```

**Edge Cases:**
- User edits title but doesn't click Save → Show warning if leaving page
- Backend rejects update (validation error) → Show error message inline
- Status change to invalid next state → Prevent dropdown selection or show error
- Comments load very slowly → Show loading spinner
- User loses connection during edit → Show "Offline, please retry"

---

### Journey 4: Change Ticket Status

**Actor:** Support staff member (usually ticket assignee or manager)

**Steps:**
1. User is viewing ticket detail at `/tickets/{ticket-id}`
2. Current status displays: "Status: Open"
3. Status change control shows dropdown: "Change to: [In Progress ▼]"
4. Dropdown only contains valid next states for current status
5. User clicks dropdown and selects "In Progress"
6. User clicks "Apply" or "Change Status" button
7. Frontend sends PUT request to `/bin/api/tickets/{id}/status` with body `{ "status": "In Progress" }`
8. Backend validates transition:
   - If valid (e.g., Open → In Progress): Updates ticket, returns 200
   - If invalid (e.g., Open → Resolved): Returns 409 with error details
9. On success (200):
   - Update display to show "Status: In Progress"
   - Show success message: "Status changed to In Progress"
   - Refresh comment section if needed
10. On error (409):
    - Show error dialog with message: "Invalid transition. From Open, you can only go to: In Progress, Cancelled"
    - Do not change status
    - Allow user to try again

**Valid State Transitions Shown in Dropdown:**
```
Current: Open        →  [In Progress ▼] or [Cancelled ▼]
Current: In Progress →  [Resolved ▼] or [Cancelled ▼]
Current: Resolved    →  [Closed ▼]
Current: Closed      →  (No dropdown, terminal)
Current: Cancelled   →  (No dropdown, terminal)
```

**Error Dialog Example:**
```
┌─────────────────────────────────────────┐
│ ⚠ Invalid Status Transition            │
├─────────────────────────────────────────┤
│ Cannot change from "Open" to "Resolved" │
│                                         │
│ Valid next states:                      │
│ • In Progress                           │
│ • Cancelled                             │
│                                         │
│ [Got it]                                │
└─────────────────────────────────────────┘
```

**Edge Cases:**
- User attempts invalid transition → Error shown, no change made
- Two users change status concurrently → Last-write-wins or conflict detected
- Status dropdown empty (already at terminal state) → Grayed out or hidden
- Backend timeout during status change → Show "Request timeout, please retry"

---

### Journey 5: Add Comment to Ticket

**Actor:** Support staff member

**Steps:**
1. User is viewing ticket detail at `/tickets/{ticket-id}`
2. Comments section at bottom shows all existing comments
3. "Add Comment" form at bottom with:
   - Textarea for message (required)
   - "Post Comment" button
4. User types comment: "I can reproduce this on staging"
5. User clicks "Post Comment"
6. Frontend validates message (non-empty, ≤ 2000 chars)
7. If valid:
   - Frontend sends POST request to `/bin/api/tickets/{id}/comments` with body `{ "message": "..." }`
   - Backend creates comment in JCR under ticket's comments folder
   - On success (201), add new comment to display (newest first)
   - Clear textarea and show success message
8. If validation fails:
   - Show error message: "Comment cannot be empty"
   - Do not allow submission

**Comments Display:**
```
── Comments (2) ───────────────────────────────

Jane Smith (2026-09-01 11:00):
"I can reproduce on staging with these steps:
 1. Login as user
 2. Add item to cart
 3. Click checkout"

Mike Wilson (2026-09-01 14:30):
"Fix deployed to production. Monitoring metrics."

Add Comment:
[_________________________________________]
[Post Comment]
```

**Edge Cases:**
- Very long comment (> 2000 chars) → Truncate or reject with message
- Comment with special characters/emojis → Display correctly
- Comments not loading initially → Show loading spinner, then comments list
- Backend error posting comment → Show "Failed to post comment, please retry"
- User submits empty comment → Show "Comment cannot be empty"

---

## Page Structure Mapping

| Page Path | Purpose | Components | Behavior |
|-----------|---------|------------|----------|
| `/tickets` | List view | TicketSearch, TicketTable | Fetch and render tickets, handle search/filter |
| `/create-ticket` | Create form | TicketCreateForm | Validate form, POST to API, redirect on success |
| `/tickets/{ticket-id}` | Detail view | TicketForm, StatusControl, CommentSection | Load ticket, allow edits, handle status change, display/add comments |

---

## Navigation Flows

```
[Create Ticket Page]
        ↓ (Create)
    (Success)
        ↓
[Ticket List Page]
        ↓ (Click Ticket)
[Ticket Detail Page]
        ↓ (Back)
[Ticket List Page]
```

---

## Error Handling in UI

### Validation Errors (Form Level)
- Display inline under each field
- Red text with icon
- Example: "Title is required (max 255 chars)"

### API Errors (Response Level)
- Display in alert/toast banner at top of page
- Show error message from API response
- Offer retry action if appropriate

### State Machine Errors (409 Conflict)
- Display in modal dialog
- Show current state, invalid request, valid next states
- Example: "Cannot go from Open to Resolved. Valid: In Progress, Cancelled"

### Network Errors
- Display "Connection error. Please check your network and retry."
- Offer retry button

---

## Accessibility Considerations

- All form fields labeled with `<label>` elements
- Error messages linked via `aria-describedby`
- Keyboard navigation (Tab through form fields)
- Focus management (focus to error field on validation fail)
- ARIA live regions for dynamic updates
- Alt text for icons/indicators
