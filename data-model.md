# Data Model

## Overview

The Support Ticket Management System uses AEM's JCR (Java Content Repository) for data persistence. Data is organized as pages under `/content/ai-practical-assessment/tickets` hierarchy.

---

## Content Structure

### Root Container Page
- **Path:** `/content/ai-practical-assessment`
- **Type:** `cq:Page`
- **Purpose:** Root folder for all ticket-related content
- **Properties:** Standard page properties (title, description)

### Tickets Container
- **Path:** `/content/ai-practical-assessment/tickets`
- **Type:** `cq:Page`
- **Purpose:** Container page listing all tickets
- **Children:** Individual ticket pages (ticket-001, ticket-002, etc.)

### Individual Ticket Page
- **Path:** `/content/ai-practical-assessment/tickets/<ticket-id>`
- **Type:** `cq:Page`
- **Purpose:** Represents a single ticket with all its properties
- **Node Name:** Auto-generated or derived from title (URL-friendly)

### Ticket Content Properties
- **Path:** `/content/ai-practical-assessment/tickets/<ticket-id>/jcr:content`
- **Type:** `nt:unstructured`
- **Properties:**

| Property | Type | Required | Mutable | Description |
|----------|------|----------|---------|-------------|
| `jcr:title` | String | Yes | Yes | Ticket title (max 255 chars) |
| `description` | String | Yes | Yes | Full ticket description (max 5000 chars) |
| `priority` | String | Yes | Yes | Priority level: HIGH, MEDIUM, LOW |
| `status` | String | Yes | Yes | Status: Open, In Progress, Resolved, Closed, Cancelled |
| `assignedTo` | String | No | Yes | User ID of assignee (AEM user) |
| `createdBy` | String | Yes | No | User ID who created ticket (immutable) |
| `createdAt` | Date | Yes | No | Creation timestamp (immutable) |
| `updatedAt` | Date | Yes | Yes | Last modification timestamp |

**Example JCR Structure:**
```
/content/ai-practical-assessment/tickets/ticket-001/jcr:content
├── jcr:title = "Payment processing broken"
├── description = "Users cannot complete payment transactions in checkout flow"
├── priority = "HIGH"
├── status = "Open"
├── assignedTo = "user-123"
├── createdBy = "user-456"
├── createdAt = 2026-09-01T10:30:00.000Z
└── updatedAt = 2026-09-01T10:30:00.000Z
```

### Comments Folder
- **Path:** `/content/ai-practical-assessment/tickets/<ticket-id>/jcr:content/comments`
- **Type:** `nt:unstructured`
- **Purpose:** Container for all comments on this ticket
- **Children:** Individual comment nodes

### Individual Comment Node
- **Path:** `/content/ai-practical-assessment/tickets/<ticket-id>/jcr:content/comments/<comment-id>`
- **Type:** `nt:unstructured`
- **Purpose:** Represents a single comment

### Comment Properties

| Property | Type | Required | Mutable | Description |
|----------|------|----------|---------|-------------|
| `message` | String | Yes | No | Comment text (max 2000 chars, immutable) |
| `createdBy` | String | Yes | No | User ID who wrote comment (immutable) |
| `createdAt` | Date | Yes | No | Comment creation timestamp (immutable) |

**Example JCR Structure:**
```
/content/ai-practical-assessment/tickets/ticket-001/jcr:content/comments/comment-001
├── message = "I can reproduce this on staging environment"
├── createdBy = "user-123"
└── createdAt = 2026-09-01T11:00:00.000Z
```

---

## JCR Queries

### List All Tickets

**Query:**
```sql
SELECT * FROM [cq:Page] 
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets]) 
  AND NOT NAME([*]) = 'jcr:content'
ORDER BY [jcr:content/createdAt] DESC
```

**Returns:** All ticket pages under the tickets container, ordered by newest first

### Search Tickets by Keyword

**Query:**
```sql
SELECT * FROM [cq:Page] 
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets]) 
  AND (
    [jcr:content/jcr:title] LIKE '%keyword%' 
    OR [jcr:content/description] LIKE '%keyword%'
  )
ORDER BY [jcr:content/createdAt] DESC
```

**Returns:** Tickets matching keyword in title or description

### Filter Tickets by Status

**Query:**
```sql
SELECT * FROM [cq:Page] 
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets]) 
  AND [jcr:content/status] = 'Open'
ORDER BY [jcr:content/createdAt] DESC
```

**Returns:** All tickets with given status

### Filter by Status and Search

**Query:**
```sql
SELECT * FROM [cq:Page] 
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets]) 
  AND [jcr:content/status] = 'Open'
  AND (
    [jcr:content/jcr:title] LIKE '%keyword%' 
    OR [jcr:content/description] LIKE '%keyword%'
  )
ORDER BY [jcr:content/createdAt] DESC
```

**Returns:** Tickets matching both status and keyword

### Get All Comments for a Ticket

**Query:**
```sql
SELECT * FROM [nt:unstructured]
WHERE ISDESCENDANTNODE([/content/ai-practical-assessment/tickets/ticket-001/jcr:content/comments])
ORDER BY [createdAt] DESC
```

**Returns:** All comments for a specific ticket, newest first

---

## Sling Model Mapping

### Ticket Sling Model

```java
@Model(adaptables = Resource.class)
public class Ticket {
    
    @ValueMapValue
    private String jcrTitle;
    
    @ValueMapValue
    private String description;
    
    @ValueMapValue
    private String priority;
    
    @ValueMapValue
    private String status;
    
    @ValueMapValue
    private String assignedTo;
    
    @ValueMapValue
    private String createdBy;
    
    @ValueMapValue
    private Calendar createdAt;
    
    @ValueMapValue
    private Calendar updatedAt;
    
    // Getters and business logic methods
    public String getId() { /* return node name */ }
    public String getTitle() { return jcrTitle; }
    public String getDescription() { return description; }
    // ... other getters
}
```

### Comment Sling Model

```java
@Model(adaptables = Resource.class)
public class Comment {
    
    @ValueMapValue
    private String message;
    
    @ValueMapValue
    private String createdBy;
    
    @ValueMapValue
    private Calendar createdAt;
    
    // Getters
    public String getId() { /* return node name */ }
    public String getMessage() { return message; }
    // ... other getters
}
```

---

## User Model

Users are stored in AEM's standard user management system at `/home/users/`.

**User Reference Structure:**
- **Type:** `rep:User` (AEM's built-in user node type)
- **Properties Used:**
  - `rep:principalName`: Unique username
  - `profile/displayName`: User's display name
  - `profile/email`: User's email

**How Used in Tickets:**
- `assignedTo` field stores user ID (e.g., "user-123")
- `createdBy` field stores user ID of ticket creator
- Comments' `createdBy` stores user ID of commenter

**Validation:**
- When assigning ticket to a user, backend validates user exists in `/home/users/`
- No separate user management UI required; users managed via AEM console

---

## State Machine Definition

### Valid Transitions

```
Open
├── → In Progress
├── → Cancelled
└── (↛ Resolved, ↛ Closed)

In Progress
├── → Resolved
├── → Cancelled
└── (↛ Open, ↛ Closed)

Resolved
├── → Closed
└── (↛ Open, ↛ In Progress, ↛ Cancelled)

Closed
└── (Terminal - no transitions)

Cancelled
└── (Terminal - no transitions)
```

### State Descriptions

- **Open:** Ticket just created, not yet assigned to someone
- **In Progress:** Someone is actively working on the ticket
- **Resolved:** Work completed, awaiting confirmation
- **Closed:** Confirmed fixed, ticket complete
- **Cancelled:** Not reproducible, won't fix, or duplicate

---

## Constraints and Indexes

### Content Constraints

- Ticket title: 1-255 characters (non-empty, trimmed)
- Ticket description: 1-5000 characters (non-empty, trimmed)
- Priority: Must be one of [HIGH, MEDIUM, LOW]
- Status: Must be one of [Open, In Progress, Resolved, Closed, Cancelled]
- AssignedTo: If provided, must reference valid AEM user
- Comment message: 1-2000 characters (immutable after creation)

### JCR Indexes (for Performance)

**Recommended Indexes:**

1. **Index on status:**
   ```xml
   <index name="status">
     <property>jcr:content/status</property>
   </index>
   ```

2. **Index on priority:**
   ```xml
   <index name="priority">
     <property>jcr:content/priority</property>
   </index>
   ```

3. **Fulltext index on title and description:**
   ```xml
   <index name="fulltext">
     <property>jcr:content/jcr:title</property>
     <property>jcr:content/description</property>
   </index>
   ```

4. **Index on createdAt for sorting:**
   ```xml
   <index name="createdAt">
     <property>jcr:content/createdAt</property>
   </index>
   ```

---

## Migration & Initialization

### Seed Data Script

AEM Content Package (`ui.content` module) includes:
- Sample users (if not using AEM OOTB users)
- Sample tickets (5-10 representative tickets)
- Folder structure for tickets and comments

**Activation:** Deploy via `mvn clean install -PautoInstallPackage`

### Idempotency

Seed script:
- Creates pages only if they don't exist
- Updates existing pages with same ID (no duplicates)
- Can be re-run multiple times safely

---

## Data Retention

- **Tickets:** Retained indefinitely (closed/cancelled tickets remain in system)
- **Comments:** Retained with their ticket (immutable)
- **No soft deletes:** Physically delete only if explicitly required; in Core, deletion is optional

---

## Serialization for API

When returning ticket data via API:

```json
{
  "id": "ticket-001",
  "title": "Payment processing broken",
  "description": "Full description...",
  "priority": "HIGH",
  "status": "Open",
  "assignedTo": "user-123",
  "createdBy": "user-456",
  "createdAt": "2026-09-01T10:30:00Z",
  "updatedAt": "2026-09-01T10:30:00Z"
}
```

**Date Format:** ISO 8601 UTC (e.g., "2026-09-01T10:30:00Z")

---

## Backup & Recovery

- JCR data backed up via AEM's standard backup mechanisms
- Content packages can be exported/imported for manual backup
- Versioning enabled on ticket pages for change history

---

## Service User Permissions

### Service User: `ai-practical-assessment-ticketservice`

**Configuration Files:**
- Service Mapping: `ui.config/...ServiceUserMapperImpl.amended~ai-practical-assessment-ticketservice.cfg.json`
- Sling RepoInit: `ui.config/...RepositoryInitializer~ai-practical-assessment.cfg.json`

**Granted Permissions:**
- `jcr:read,rep:write` on `/content/ai-practical-assessment` — Navigate parent path and create child nodes
- `jcr:all` on `/content/ai-practical-assessment/tickets` — Full ticket CRUD operations
- `jcr:read` on `/home/users` and `/home/groups` — User validation for ticket assignment

**Usage:** Backend `TicketServiceImpl` uses this service user to create/manage tickets with appropriate JCR permissions while maintaining security boundaries.
