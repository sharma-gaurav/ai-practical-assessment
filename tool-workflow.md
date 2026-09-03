# AI Workflow - Claude Code

## 1. Primary AI Tool Used

**Claude Code** (IDE extension and CLI tool) - Used for requirement analysis, design, code generation, testing, debugging, and code review.

## 2. How I Provide Project Context to the Tool

- **CLAUDE.md and AGENTS.md:** Project-specific instructions, AEM architecture, and development guidelines
- **MEMORY.md:** Persistent context about project decisions, user preferences, and workflow
- **IDE Selection:** Using VSCode extension to highlight code for context-aware suggestions
- **File Reading:** Referencing specific file paths and line numbers for precise context
- **Conversation History:** Building on prior exchanges to maintain coherent understanding across tasks

## 3. How I Use AI for Requirement Analysis

- Reviewed the Requirements-Document.md to understand the Support Ticket Management System specification
- Used AI to clarify ambiguous requirements and identify edge cases (e.g., state machine validation rules)
- Broke down Business Context, Core Features, and Stretch Goals into actionable items
- Identified assumptions specific to AEM (JCR persistence, user management, OOTB components)
- Asked AI to validate understanding against the given constraints and clarifications

## 4. How I Use AI for Planning and Design

- Provided requirements to AI and asked for architecture recommendations considering AEM constraints
- Collaborated on dividing work into phases: architecture → backend models → frontend UI → testing
- Used AI to design the state machine validation logic and error handling strategy
- Created task breakdown with estimated effort for each component
- Validated design decisions against non-functional requirements (scalability, maintainability)

## 5. How I Use AI for Code Generation

- Shared design notes and API contract with AI before generating code
- Asked AI to generate OSGi services, Sling Models, and HTL components following AEM patterns
- Requested code with context: "Generate a Sling Model for the Ticket entity that exposes JCR properties as getters"
- Reviewed generated code before merging, checking for proper OSGi annotations and Sling best practices
- Iterated on code with specific feedback: "This service needs injected ResourceResolver, not direct access"

## 6. How I Validate AI-Generated Code

- Ran Maven builds to catch compilation errors immediately
- Tested code locally against the AEM SDK
- Checked for proper OSGi lifecycle (activate/deactivate methods)
- Verified JCR query patterns and content structure compliance
- Cross-referenced AI suggestions against official AEM documentation
- Wrote unit tests to validate business logic (especially state machine transitions)

## 7. How I Use AI for Testing

- Asked AI to generate JUnit tests following AEM test patterns (AemContext, ResourceResolver mocks)
- Requested test cases for state machine edge cases and invalid transitions
- Used AI to design integration tests using AEM Testing Clients
- Asked for Cypress test scripts for UI flows (create ticket, search, status change)
- Validated test coverage by asking AI to identify untested paths

## 8. How I Use AI for Debugging

- Described errors to AI with full stack traces and context
- Asked AI to explain what specific exception meant in AEM context
- Used AI to identify common causes (e.g., missing service annotations, wrong query syntax)
- Requested help understanding JCR query results or bundle resolution issues
- Collaborated on isolating issues (code vs. configuration vs. AEM SDK setup)

## 9. How I Use AI for Code Review

- Pasted code snippets and asked for review against AEM best practices
- Requested identification of: security issues, performance concerns, anti-patterns
- Asked for simplification suggestions and naming improvements
- Validated error handling and input validation logic
- Checked for proper resource management (closing ResourceResolvers, etc.)

## 10. What Information I Avoid Sharing with AI Tools

- No production secrets, API keys, or credentials
- No proprietary business logic or confidential product roadmaps
- No personal data from real users (used synthetic seed data only)
- No internal performance metrics or security vulnerabilities discovered in live systems
- When discussing AEM issues, avoided sharing customer-specific customizations without sanitizing

## 11. How I Reuse This Workflow in a Real Project

- **Reusable Prompts:** Template prompts for "Generate OSGi Service", "Create Sling Model", "Write JCR Query"
- **Persistent Specs:** Store acceptance criteria and API contracts in the repo for consistency across changes
- **Task Tracking:** Use version-controlled task lists to maintain alignment between requirements and implementation
- **AI Prompt Library:** Build a `ai-prompts/` folder structure that others can reference
- **Documentation:** Keep design rationale and trade-off decisions visible so future changes understand context
- **Code Review Checklists:** Document AI-assisted review patterns so peers can apply similar rigor