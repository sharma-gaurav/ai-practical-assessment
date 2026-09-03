# Debugging Prompts

Record of AI prompts used for troubleshooting and debugging.

---

## Prompt 1: Maven Build Failure - ESLint Error

**Date:** 2026-09-01

**Problem:** Build failed with ESLint error in ticket create component during `mvn clean install -PautoInstallSinglePackage`

**Prompt Text:**
```
Debug why is my build failing
```

**AI Response Summary:**
AI ran the build and captured the output, identifying the specific ESLint error without requiring user interaction.

**Investigation Steps Suggested by AI:**
1. Read the error output from Maven build to identify the specific issue
2. Examined the _ticketcreate.js file to locate line 13
3. Identified the pattern causing the error: `const self = this;` aliasing
4. Replaced with arrow function to preserve context naturally

**Final Root Cause:** ESLint rule `@typescript-eslint/no-this-alias` disallows aliasing `this` to a local variable. Line 13 had `const self = this;` and line 23 used it in a regular function callback.

**How AI Helped:** 
- Immediately ran the build and extracted the error message from the output
- Located the exact file and line number causing the failure
- Read the file to understand the context
- Provided a targeted fix using arrow functions instead of the aliasing pattern

**What AI Missed:** 
Nothing significant - the diagnosis and fix were straightforward once the error output was available.

**Resolution:**
- Removed `const self = this;` declaration
- Changed event listener callback to arrow function: `(e) => { this.onFormSubmit(e); }`
- Arrow functions preserve `this` context automatically, eliminating the need for aliasing
- Build succeeded after this fix

**Time Saved:** ~5 minutes (would have required manual investigation of ESLint rules otherwise)

---

## Prompt 2: Component Not Appearing in AEM Sidepanel

**Date:** 2026-09-01

**Problem:** "Ticket Create Form" component doesn't appear in the AEM component selector/sidepanel after build and deployment.

**Prompt Text:**
```
Debug why I am not able to see Ticket create Form in list of components.
```

**AI Response Summary:**
AI investigated the component structure by:
1. Listing component files to verify deployment
2. Comparing ticketcreate with working components (helloworld, button)
3. Identifying that helloworld component has a `_cq_dialog` folder which was missing from ticketcreate

**Investigation Steps Suggested by AI:**
1. Check if component files exist and are deployed
2. Compare with existing working components
3. Identify structural differences
4. Look for missing required configuration

**Final Root Cause:** Missing `_cq_dialog` folder. AEM requires a dialog configuration for components to appear in the sidepanel and be droppable on pages.

**How AI Helped:**
- Systematically compared component structures
- Identified the exact missing piece by examining working components
- Provided clear next steps (create dialog structure)

**What I Changed:**
- Created `_cq_dialog/.content.xml` with authorable properties for form fields
- Updated ticketcreate.html to use HTL expressions for dynamic field labels/placeholders
- Made authorable: formTitle, titleLabel, titlePlaceholder, descriptionLabel, descriptionPlaceholder, priorityLabel, assigneeLabel, submitButtonText, cancelButtonText
- Added component icon and description to .content.xml

**Resolution:**
1. Created `_cq_dialog` folder structure
2. Defined 9 authorable properties in dialog configuration
3. Updated HTL template to use ${properties.*} with fallback defaults
4. Rebuilt and redeployed with `mvn clean install -PautoInstallSinglePackage`
5. Component now discoverable and authorable in AEM

**Time Saved:** ~15 minutes (would have required manual JCR debugging otherwise)

**Learning:** AEM components require dialog configuration to be visible in author UI, even if not all properties are editable.

---

## Prompt 3: Component Group Not Allowed in Page Template

**Date:** 2026-09-01

**Problem:** "AI Practical Assessment - Forms" component group needs to be explicitly allowed in the "Page Content" template so the "Ticket Create Form" component can be added to pages.

**User Feedback:** "AI Practical Assessment - Forms should be allowed in page template by default"

**Solution Implemented:**
1. Located the page template policy at `/conf/ai-practical-assessment/settings/wcm/templates/page-content/policies/.content.xml`
2. Added ticketcreate component reference to the template mapping
3. Created ticketcreate policy entry in `/conf/ai-practical-assessment/settings/wcm/policies/`
4. Rebuilt and redeployed with Maven

**What Changed:**
- Added `<ticketcreate>` mapping entry to template policies
- Added `<ticketcreate>` policy definition with component configuration
- Component now discoverable and droppable on pages using the "Page Content" template

**Result:** Component is now fully integrated into the template system and available by default when creating pages

**Time Saved:** ~5 minutes (vs manual policy configuration)

---

## Prompt 4: User Existence Check Using Hardcoded Path

**Date:** 2026-09-02

**Problem:** The `userExists()` method in TicketServiceImpl was checking for user existence using a hardcoded path `/home/users/{userId}`, which doesn't correctly match AEM's actual user node structure. User IDs don't directly correspond to node names due to AEM's internal user management implementation.

**Prompt Text:**
```
User does not exist as username does not match the name of node.
```

**AI Response Summary:**
AI identified that the method was using an incorrect approach to verify user existence in AEM and provided a fix using the proper AEM User Management API.

**Investigation Steps Suggested by AI:**
1. Examined the current userExists method implementation
2. Identified the issue: hardcoded path pattern doesn't match AEM's actual node structure
3. Recommended using the UserManager API instead of direct path lookup
4. Provided imports for JackrabbitSession and UserManager
5. Implemented proper user validation using userManager.getAuthorizable(userId)

**Final Root Cause:** AEM stores users in intermediate node hierarchies that don't directly match the userId parameter. The path `/home/users/{userId}` assumption is unreliable and doesn't work with AEM's actual user management structure.

**How AI Helped:**
- Immediately identified the architectural issue with the approach
- Provided the correct AEM API to use (UserManager)
- Implemented proper type casting and null checks
- Fixed the import statements

**Code Changes Made:**
```java
// Before:
Resource userResource = resolver.getResource("/home/users/" + userId);
return userResource != null;

// After:
if (session instanceof JackrabbitSession) {
    JackrabbitSession jrSession = (JackrabbitSession) session;
    UserManager userManager = jrSession.getUserManager();
    return userManager.getAuthorizable(userId) != null;
}
```

**Added Imports:**
- `org.apache.jackrabbit.api.JackrabbitSession`
- `org.apache.jackrabbit.api.security.user.UserManager`

**Resolution:**
1. Added JackrabbitSession import to enable access to UserManager
2. Cast Session to JackrabbitSession to access the user manager
3. Use userManager.getAuthorizable(userId) which properly handles AEM's user node structure
4. Added null checks for session and JackrabbitSession type validation

**Time Saved:** ~10 minutes (would have required manual AEM API research otherwise)

**Learning:** Always use AEM's built-in User Management API (UserManager) for user lookups instead of assuming path structures. The actual implementation details of how users are stored in JCR is abstracted by the UserManager API.

---

## Common Debugging Patterns

### Pattern 1: Stack Trace Analysis
```
Template: "I'm getting this error: [stack trace]. 
What does it mean in AEM context? How should I fix it?"
```

### Pattern 2: Unexpected Behavior
```
Template: "[Feature] is not working as expected.
Steps to reproduce: [Steps]
Expected: [Expected behavior]
Actual: [Actual behavior]
What could cause this?"
```

### Pattern 3: Performance Issues
```
Template: "[Operation] is slow. 
It takes [time] for [N] items.
My code: [Snippet]
How can I optimize?"
```

---

## Debugging Effectiveness

| Issue | AI Helpfulness | Time Saved | Confidence |
|-------|-----------------|-----------|------------|
| Maven Build - ESLint Error | High | 5 min | High |
| Component Not in Sidepanel | High | 15 min | High |
| Component Group Not in Template | User-directed | 5 min | High |
| API Path Migration & CSRF Config | User-directed | 10 min | High |

---

## AI Debugging Strengths

- Pattern recognition (this looks like X issue)
- Suggesting systematic investigation approaches
- Explaining AEM error messages
- Recommending common solutions

---

## AI Debugging Limitations

- Couldn't debug without seeing actual code
- Needed me to run tests and report results
- Sometimes suggested things I'd already tried
- Didn't always know AEM SDK-specific quirks

---

## Lessons Learned

- Always provide: error message, code snippet, steps to reproduce
- Log analysis easier when logs are well-formatted
- Some bugs only manifest in specific AEM SDK versions
- Pair debugging with systematic testing
