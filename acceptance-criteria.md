# Acceptance Criteria

## Core Functionality

- [ ] A user can create a ticket via the UI with title, description, priority, and assignee
- [ ] Created ticket is saved to AEM JCR and immediately visible in the ticket list
- [ ] A user can view all tickets in a list view (with pagination in Stretch)
- [ ] A user can click on a ticket to open its detail view
- [ ] Detail view shows: title, description, priority, status, assignee, createdBy, createdAt, updatedAt, and all comments
- [ ] A user can update ticket fields: title, description, priority, assignee
- [ ] Updates are saved and reflected in both list and detail views
- [ ] A user can add a comment to a ticket
- [ ] Comment is saved with createdBy and createdAt timestamp
- [ ] Comments are displayed in chronological order on ticket detail view

## Validation

- [ ] Creating a ticket without a title shows validation error ("Title is required")
- [ ] Creating a ticket without a description shows validation error
- [ ] Creating a ticket without selecting priority shows validation error
- [ ] Creating a ticket with invalid assignee is rejected by backend with error message
- [ ] Empty or whitespace-only title/description is rejected
- [ ] Very long inputs are truncated or rejected gracefully (define max length)
- [ ] Assigning to a non-existent user is rejected with helpful message

## Error Handling

- [ ] Invalid state transition (e.g., Resolved directly to Cancelled) is rejected by backend
- [ ] Backend returns HTTP 400/409 with clear error message for invalid transitions
- [ ] UI displays error message to user: "Invalid transition. Allowed next states: [list]"
- [ ] Attempting to update a ticket that no longer exists shows "Ticket not found" error
- [ ] Network errors during save show "Connection error, please try again"
- [ ] JCR write conflicts are handled with retry or conflict resolution message

## State Machine

- [ ] Ticket starts in "Open" status
- [ ] Open → In Progress transition succeeds
- [ ] Open → Resolved transition is rejected (must go through In Progress)
- [ ] Open → Cancelled transition succeeds
- [ ] In Progress → Resolved transition succeeds
- [ ] In Progress → Cancelled transition succeeds
- [ ] In Progress → Open transition is rejected
- [ ] Resolved → Closed transition succeeds
- [ ] Resolved → Open or In Progress transitions are rejected
- [ ] Closed → any other status transition is rejected (terminal state)
- [ ] Invalid transition shows error modal with allowed next states

## Search & Filter

- [ ] Keyword search finds tickets matching the keyword in title or description
- [ ] Search is case-insensitive
- [ ] Search with special characters (e.g., "bug/crash") works without breaking the query
- [ ] Status filter (e.g., "Show Open tickets only") reduces list to matching tickets
- [ ] Search and status filter work together (search within filtered results)
- [ ] Empty search results show message "No tickets found. Try refining your search."
- [ ] Clearing filters shows all tickets again

## Testing

- [ ] Integration tests verify all valid state transitions succeed
- [ ] Integration tests verify all invalid state transitions are rejected
- [ ] Tests use Sling Mocks and AEM Testing Clients for isolation
- [ ] Tests verify data persistence (ticket survives restart)
- [ ] Tests validate input validation and error messages
- [ ] At least 80% code coverage on state machine logic

## Documentation

- [ ] README.md explains how to build and run the project locally
- [ ] README includes AEM SDK setup steps
- [ ] README provides example curl commands for API endpoints
- [ ] API Contract (api-contract.md) documents all endpoints and payloads
- [ ] Data Model (data-model.md) explains JCR content structure
- [ ] Comments explain non-obvious business logic (especially state machine)

## Data Persistence

- [ ] Shutting down AEM and restarting does not lose any tickets or comments
- [ ] JCR backup/restore preserves ticket data
- [ ] Seed data is provided for testing (at least 5 sample tickets)
- [ ] Seed script can be re-run without duplicating data

## Security & Code Quality

- [ ] No API keys, passwords, or credentials in git history
- [ ] .gitignore includes .env, local config files
- [ ] Service users properly configured for backend operations
- [ ] Code follows AEM/Java conventions (package structure, naming, OSGi patterns)
- [ ] No deprecated AEM/Sling APIs used
- [ ] Code is reviewed for common security issues (injection, XSS, CSRF)

## Deployment

- [ ] Full Maven build succeeds: `mvn clean install`
- [ ] Deployment to local AEM SDK succeeds: `mvn clean install -PautoInstallSinglePackage`
- [ ] Bundles start without errors (check logs in AEM console)
- [ ] Packages are installed without conflicts
- [ ] UI is accessible immediately after deployment
