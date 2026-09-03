# Project Context for Claude Code

## Project Overview

**Project Name:** Support Ticket Management System (AEM-based)

**Technology Stack:** AEM as a Cloud Service, Java, Vanilla JavaScript, JCR

**Team Size:** Solo developer

**Timeline:** 1 week (8-12 focused hours)

---

## AI Tool: Claude Code

**Version/Model:** Claude Haiku 4.5

**Access Method:** IDE Extension (VSCode) + CLI

**Key Features Used:**
- Context-aware code suggestions
- Multi-file editing
- Memory system for persistent context
- Direct AEM/Java pattern recognition

---

## Project Goals

### Primary
- Build working Support Ticket Management System
- Demonstrate AI-assisted workflow across full lifecycle
- Validate state machine implementation thoroughly
- Complete within time budget

### Secondary
- Create reusable AI prompts and patterns
- Document AI effectiveness and learnings
- Build prompt library for future projects
- Establish validation and testing practices

---

## Key Constraints

### Technical
- **Language:** Java for backend, Vanilla JavaScript for frontend (NO React)
- **Framework:** AEM as a Cloud Service (Cloud Manager compatible)
- **Persistence:** AEM JCR only (no external database)
- **Content Structure:** Page-based hierarchy under `/content/ai-practical-assessment/`
- **Frontend:** Vanilla JS with HTML/CSS (no frameworks)

### Project
- **Scope:** Strictly Core; Stretch is optional
- **Time:** ~10 hours total effort
- **Testing:** Must include unit, integration, and E2E tests
- **Documentation:** Required (lifecycle artifacts)

### Organizational
- **User Email:** gaurav.sharma@tothenew.com
- **Assessment Date:** 2026-09-01
- **Deliverable:** Git repository with all artifacts

---

## Critical Success Factors

1. **State Machine Validation:** All transitions tested, no invalid transitions allowed
2. **Complete Artifacts:** Requirement analysis, design, tests, documentation present
3. **Prompt History:** Organized and shows iteration/validation
4. **Code Quality:** Follows AEM best practices, no security vulnerabilities
5. **Data Persistence:** Tickets survive restart

---

## AI Workflow Established

### Context-Setting
- Provide CLAUDE.md with project constraints and rules
- Share requirements-analysis.md for clarity
- Reference design-notes.md for architecture decisions
- Explain state machine rules explicitly

### Code Generation
- Generate OSGi services with specific error handling
- Generate Sling servlets with validation
- Generate Vanilla JS modules (no React)
- Generate comprehensive tests
- Always test immediately after generation

### Validation
- Compile and run all code locally
- Check against AEM SDK
- Cross-reference with official docs
- Manual testing of critical paths
- Test execution before committing

### Documentation
- Record all prompts by activity
- Capture what AI got right and what I changed
- Document why changes were made
- Maintain decision log

---

## How to Use This Context

### For New Tasks
1. Reference this context when asking AI for help
2. Remind AI of constraints (Vanilla JS, AEM, state machine rules)
3. Provide code samples and error messages
4. Ask for validation against best practices

### For Code Generation
1. Share relevant design docs
2. Provide specific requirements and constraints
3. Ask AI to generate with error handling
4. Test immediately after
5. Refine if needed

### For Debugging
1. Provide error message and stack trace
2. Share relevant code snippet
3. Ask for systematic troubleshooting approach
4. Verify fix before committing

### For Testing
1. Ask for comprehensive test matrices
2. Generate test cases
3. Adapt for AemContext
4. Run and verify passing

---

## Repository Structure

```
ai-practical-assessment/
├── CLAUDE.md (project rules)
├── requirements-analysis.md
├── design-notes.md
├── implementation-plan.md
├── api-contract.md
├── data-model.md
├── ui-flow.md
├── test-strategy.md
├── acceptance-criteria.md
│
├── ai-prompts/ (prompt history by activity)
├── tool-specific/ (this directory, Claude workflow)
│
├── core/ (Java backend)
├── ui.frontend/ (JavaScript frontend)
├── it.tests/ (integration tests)
├── ui.tests/ (E2E tests)
└── [other AEM modules...]
```

---

## What Claude Code Knows

✓ AEM as a Cloud Service architecture  
✓ OSGi services and patterns  
✓ Sling Models and servlets  
✓ JCR content repository  
✓ Maven build system  
✓ Vanilla JavaScript patterns  
✓ Testing with JUnit and Cypress  
✓ Git and version control  
✓ State machine design  
✓ API contract design  

---

## What Claude Code Needs to Know

- Specific project requirements → Share requirements-analysis.md
- Content structure decisions → Share data-model.md
- UI layout expectations → Share ui-flow.md
- Testing priorities → Share test-strategy.md
- Code quality standards → Share CLAUDE.md rules
- Current progress → Explain what's been done, what's next

---

## Effective Prompt Patterns with Claude

### Pattern 1: Full Context
```
I'm working on [AEM project]. Constraint: [Constraint].
Requirement: [Requirement from requirements-analysis.md].
Design: [Design approach from design-notes.md].
I need help with: [Specific ask].
```

### Pattern 2: Generate with Specificity
```
Generate [component] for [purpose].
Requirements:
- [Req 1]
- [Req 2]
- Error handling: [Approach]
- Validation: [Rules]
Context: [Relevant background]
```

### Pattern 3: Validation Request
```
Review this [component] for [specific concern].
Code: [Snippet]
Context: [Background]
Checking for: [What to look for]
```

---

## Success Metrics

- ✓ All code compiles without errors
- ✓ All tests pass (unit, integration, E2E)
- ✓ State machine exhaustively tested
- ✓ No security vulnerabilities
- ✓ Prompt history shows iteration
- ✓ All documentation complete
- ✓ Artifact checklist complete

---

## Revision History

- **2026-09-01:** Initial context created
- [Dates and changes to be filled as project progresses]
