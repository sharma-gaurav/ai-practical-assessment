# Unit Test Creation Workflow

## Overview

This document defines the iterative workflow for creating, executing, and improving unit tests for the AI Practical Assessment application. The goal is to achieve **≥70% code coverage** through systematic test development.

---

## Prerequisites

### Testing Tools & Frameworks
- **Test Framework:** JUnit 5 (Jupiter)
- **Mocking:** Mockito (mockito-core, mockito-junit-jupiter)
- **AEM Testing:** io.wcm.testing.aem-mock.junit5
- **Coverage Tool:** JaCoCo (Maven plugin)
- **Build Tool:** Maven

### Project Structure
```
core/
  ├── src/main/java/com/example/aem/core/
  │   ├── services/
  │   ├── servlets/
  │   └── models/
  └── src/test/java/com/example/aem/core/
      ├── services/
      ├── servlets/
      └── models/
```

### Maven Commands
```bash
# Run unit tests only
mvn test

# Run all tests with coverage
mvn verify

# Generate coverage report
mvn jacoco:report

# View coverage report
open target/site/jacoco/index.html

# Run specific test class
mvn test -Dtest=TicketOperationServletTest
```

---

## Workflow Steps

### Phase 1: Test Planning & Analysis

#### Step 1.1: Identify Classes to Test
- Review the component/class that needs testing
- Check existing test coverage gaps using JaCoCo report
- Prioritize by criticality (services > utilities > models)

**Questions to Ask:**
- Is this class already tested? Check `target/site/jacoco/`
- What are the public methods that need testing?
- Are there any complex business logic branches?
- Does this class have dependencies (services, DAOs)?

#### Step 1.2: List Test Cases
For each public method, identify:
- **Happy Path:** Valid input, expected success
- **Edge Cases:** Boundary conditions, null values, empty collections
- **Error Cases:** Invalid input, exceptions, null dependencies

**Example for TicketOperationServlet.doGet():**
| Scenario | Input | Expected | Coverage |
|----------|-------|----------|----------|
| List tickets (no params) | GET /bin/api/tickets | 200, list of tickets | Happy path |
| Get ticket detail | GET /bin/api/tickets?id=ticket-001 | 200, ticket object | Happy path |
| Get invalid ticket | GET /bin/api/tickets?id=invalid | 404, error message | Error case |
| Empty ticket list | (no tickets in repo) | 200, empty array | Edge case |

---

### Phase 2: Test Implementation

#### Step 2.1: Create Test Class
Create test file: `core/src/test/java/com/example/aem/core/servlets/TicketOperationServletTest.java`

**Template:**
```java
@ExtendWith(AemContextExtension.class)
class TicketOperationServletTest {

    private TicketOperationServlet servlet;
    
    @Mock
    private TicketService ticketService;
    
    @Mock
    private StateTransitionValidator validator;

    @BeforeEach
    void setUp(AemContext context) {
        servlet = new TicketOperationServlet();
        // Initialize mocks and dependencies
    }

    @Test
    void testGetListTickets() {
        // Arrange
        MockSlingHttpServletRequest request = createMockRequest("GET", "/bin/api/tickets");
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        
        // Act
        servlet.doGet(request, response);
        
        // Assert
        assertEquals(200, response.getStatus());
        assertTrue(response.getOutputAsString().contains("tickets"));
    }
}
```

#### Step 2.2: Implement Test Cases
For each test case from Step 1.2:
- Write descriptive test method name: `test[MethodName][Scenario]`
- Use Arrange-Act-Assert (AAA) pattern
- Mock external dependencies
- Assert both return value and behavior

**Best Practices:**
- ✅ One assertion per test (or related assertions)
- ✅ Clear test names that describe what's being tested
- ✅ Use constants for test data
- ✅ Mock external services/repositories
- ❌ Don't test framework code (JUnit, AEM core)
- ❌ Don't create dependencies between tests

---

### Phase 3: Test Execution

#### Step 3.1: Run Unit Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TicketOperationServletTest

# Run specific test method
mvn test -Dtest=TicketOperationServletTest#testGetListTickets
```

**Success Criteria:**
- ✅ All tests pass (BUILD SUCCESS)
- ✅ No compilation errors
- ✅ No test timeouts

#### Step 3.2: Run with Coverage Analysis
```bash
# Generate JaCoCo report
mvn verify jacoco:report

# View report
open target/site/jacoco/index.html  # macOS
start target/site/jacoco/index.html # Windows
```

---

### Phase 4: Coverage Evaluation

#### Step 4.1: Review Coverage Report
Open `target/site/jacoco/index.html` and check:
- **Overall Coverage:** Target ≥70%
- **Line Coverage:** % of lines executed
- **Branch Coverage:** % of if/else branches covered
- **Method Coverage:** % of methods executed

#### Step 4.2: Identify Coverage Gaps
Look for:
- **Red indicators:** Uncovered lines/branches
- **Yellow indicators:** Partially covered code
- **Methods with 0% coverage:** Need new tests

**Coverage Report Structure:**
```
TicketOperationServlet
├── Line Coverage: 68%  ❌ Below 70%
├── Branch Coverage: 55%  ❌ Low
├── Method Coverage: 80%  ✅ Good
│
└── Uncovered Methods:
    ├── doPut() → Line 286-318: 45% covered
    ├── handleStatusChange() → Line 364-397: 20% covered
    └── fetchTickets() → Line 212-284: 30% covered
```

#### Step 4.3: Calculate Coverage
```
Formula: (Lines Covered / Total Lines) × 100

Example:
- Total lines in servlet: 437
- Lines covered: 297
- Coverage: (297 / 437) × 100 = 68%
- Status: ❌ BELOW TARGET (need ≥70%)
```

---

### Phase 5: Coverage Improvement (If Needed)

#### Step 5.1: Add Tests for Uncovered Branches
For each uncovered line/branch identified in Step 4.2:
1. Understand the code path
2. Create test case that exercises that path
3. Run tests again to verify coverage increased

**Example - Uncovered doPut() Method:**

Current coverage: 45%
Uncovered scenarios:
- PUT with missing ticket ID
- PUT with invalid JSON
- PUT for status change with 409 Conflict

**Add test cases:**
```java
@Test
void testPutMissingTicketId() {
    // Test PUT /bin/api/tickets (no ?id parameter)
    // Should return 400 Bad Request
}

@Test
void testPutInvalidJson() {
    // Test PUT with malformed JSON body
    // Should return 400 Bad Request
}

@Test
void testPutStatusChangeInvalidTransition() {
    // Test PUT with invalid status transition
    // Should return 409 Conflict
}
```

#### Step 5.2: Repeat Test Execution
```bash
# After adding new tests
mvn clean test
mvn verify jacoco:report
```

#### Step 5.3: Verify Coverage Improvement
- Check updated coverage percentage
- If still below 70%, repeat Steps 5.1-5.3
- Document any uncovered code that cannot be tested (dead code, impossible branches)

---

### Phase 6: Record Results

#### Step 6.1: Update test-results.md

Template entry:
```markdown
## Test Execution: TicketOperationServlet
**Date:** 2026-09-07
**Test Class:** TicketOperationServletTest
**Status:** ✅ PASSED

### Coverage Summary
- Line Coverage: 72%
- Branch Coverage: 68%
- Method Coverage: 90%
- Overall: 72% ✅

### Test Results
- Total Tests: 15
- Passed: 15 ✅
- Failed: 0
- Skipped: 0

### Coverage Gaps
- None (all methods covered ≥70%)

### Next Steps
- Test CommentServiceImpl
- Test StateTransitionValidatorImpl
```

#### Step 6.2: Update test-strategy.md (If Needed)
- Add new test cases to the strategy document
- Update test scope if new requirements discovered
- Document any deviations from planned approach

**Checklist:**
- [ ] Are all test cases from test-strategy.md implemented?
- [ ] Are all test cases passing?
- [ ] Is coverage ≥70%?
- [ ] Are test names descriptive and follow convention?
- [ ] Are edge cases and error cases covered?
- [ ] Is test-strategy.md still accurate?

---

## Decision Tree: When to Stop Testing a Class

```
Is coverage ≥70% AND all tests passing?
├─ YES → Move to next class
│   └─ Update test-results.md with status: COMPLETE
│
└─ NO → Coverage < 70% OR tests failing
    ├─ Can the code path be tested?
    │  ├─ YES → Add more tests (Phase 5)
    │  └─ NO → Document as "Not Testable" (dead code/framework code)
    │
    └─ Repeat Phase 3-5 until coverage ≥70%
```

---

## Ambiguities & Questions

### Q1: Should I Mock or Use Real AEM Context?
**Answer:** 
- Use **mocks** for unit tests (fast, isolated)
- Use **AemContext** for integration tests (test with JCR)

**Rule of Thumb:**
- Servlet tests: Mock services, use AemContext for requests
- Service tests: Mock JCR repository
- Validator tests: No mocks needed (pure logic)

---

### Q2: What Code Paths Are "Not Testable"?
**Examples:**
- Framework-generated code (synthetic methods)
- Reflection-based code in frameworks
- Impossible null checks (already validated)
- Dead code (unreachable branches)

**Action:** Document in test-results.md with reason

---

### Q3: Should I Aim for 100% Coverage?
**Answer:** No, 70% is the minimum target.

**Rationale:**
- 100% coverage doesn't mean no bugs
- Diminishing returns above 80%
- Focus on critical paths (business logic, validation, error handling)
- Framework boilerplate coverage is low value

**Coverage Targets by Type:**
| Type | Target |
|------|--------|
| Validators (pure logic) | 95%+ |
| Services (business logic) | 80%+ |
| Servlets (handlers) | 70%+ |
| Models (POJOs) | 60%+ |

---

### Q4: What If a Test Is Flaky (Sometimes Passes, Sometimes Fails)?
**Action:**
1. Identify the flaky assertion
2. Add `@Timeout(seconds = 5)` to prevent hangs
3. Avoid timing-dependent tests
4. Mock time-based operations
5. If still flaky, document as "Flaky" and skip with `@Disabled("Timing issue")`

---

### Q5: How to Test Private Methods?
**Answer:** Don't. If a private method needs testing, it should be:
1. **Public utility method** (move to separate utility class)
2. **Tested indirectly** (through public method that calls it)

**Example:**
```java
// ❌ DON'T: Test private method directly
@Test
void testPrivateValidation() throws Exception {
    Method method = Service.class.getDeclaredMethod("validateInput", String.class);
    method.setAccessible(true);  // Avoid this!
}

// ✅ DO: Test through public method
@Test
void testCreateWithInvalidInput() {
    assertThrows(Exception.class, () -> service.create(""));
    // This indirectly tests validateInput()
}
```

---

## Performance Considerations

### Build Time Impact
- Adding 50 unit tests: ~2-3 seconds
- Adding 20 integration tests: ~5-10 seconds
- Generating coverage report: ~3-5 seconds

**Total for full build with tests:** ~30-45 seconds

### Optimizations
```bash
# Run only unit tests (skip integration tests)
mvn test

# Skip coverage report (faster feedback during development)
mvn test -DskipJacoco

# Parallel test execution
mvn test -T 1C  # 1 thread per core
```

---

## Troubleshooting

### Issue: Tests Fail After Code Change
**Solution:**
1. Identify which tests failed
2. Review code change that broke tests
3. Update tests to match new behavior
4. Update test-strategy.md if requirements changed

### Issue: Coverage Drops After Refactoring
**Solution:**
- Coverage may change due to code structure changes
- Ensure no untested branches were introduced
- Add tests for any new methods added

### Issue: Cannot Mock AEM Services
**Solution:**
- Use `@Mock` annotation from Mockito
- Or use `context.registerService()` in AemContext
- Check test-strategy.md for mocking examples

### Issue: JaCoCo Report Not Generated
**Solution:**
```bash
# Ensure JaCoCo plugin is in pom.xml
mvn verify -Pjacoco

# If still missing, check target/jacoco.exec exists
```

---

## Success Criteria Checklist

For each class tested, verify:

- [ ] All public methods have at least 1 test case
- [ ] Happy path, edge cases, and error cases covered
- [ ] Line coverage ≥70% (or documented exception)
- [ ] Branch coverage ≥65% (or documented exception)
- [ ] All tests passing (0 failures)
- [ ] No deprecated warnings
- [ ] Test names are descriptive
- [ ] Test-strategy.md updated with actual test count
- [ ] test-results.md has execution record
- [ ] Code review completed and approved

---

## Timeline Example

```
Day 1:
  └─ Phase 1: Analyze TicketOperationServlet (1 hour)
  └─ Phase 2: Write 12 test cases (3 hours)
  └─ Phase 3: Run tests (10 min)
  └─ Phase 4: Review coverage → 65% (not enough)

Day 2:
  └─ Phase 5: Add 8 more tests for gaps (2 hours)
  └─ Phase 3: Re-run tests (10 min)
  └─ Phase 4: Review coverage → 72% ✅
  └─ Phase 6: Record results (30 min)

Total: ~6 hours for one servlet class
```

---

## Next Steps

1. **Identify first class to test** (e.g., StateTransitionValidator)
2. **Follow Phases 1-6** in order
3. **Record results** in test-results.md
4. **Repeat for next class** (e.g., CommentServiceImpl)
5. **Track cumulative coverage** across all classes

---

## References

- **Test Strategy:** `test-strategy.md`
- **Test Results:** `test-results.md`
- **Maven Surefire:** https://maven.apache.org/surefire/maven-surefire-plugin/
- **JaCoCo:** https://www.jacoco.org/jacoco/trunk/doc/maven.html
- **JUnit 5:** https://junit.org/junit5/docs/current/user-guide/
- **Mockito:** https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
