package com.example.aem.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.aem.core.services.impl.StateTransitionValidatorImpl;

/**
 * Unit tests for StateTransitionValidator
 * Tests state machine logic for ticket status transitions
 *
 * State Machine Rules:
 * - Open → In Progress, Cancelled
 * - In Progress → Resolved, Cancelled
 * - Resolved → Closed, In Progress
 * - Closed → Terminal (no further transitions)
 * - Cancelled → Terminal (no further transitions)
 */
class StateTransitionValidatorTest {

    private StateTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StateTransitionValidatorImpl();
    }

    // ===================== OPEN State Transitions =====================

    @Test
    void testOpenToInProgress_Valid() {
        // Act & Assert
        assertTrue(validator.isValidTransition("Open", "In Progress"));
    }

    @Test
    void testOpenToCancelled_Valid() {
        // Act & Assert
        assertTrue(validator.isValidTransition("Open", "Cancelled"));
    }

    @Test
    void testOpenToResolved_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Open", "Resolved"));
    }

    @Test
    void testOpenToClosed_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Open", "Closed"));
    }

    @Test
    void testOpenToOpen_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Open", "Open"));
    }

    // ===================== IN PROGRESS State Transitions =====================

    @Test
    void testInProgressToResolved_Valid() {
        // Act & Assert
        assertTrue(validator.isValidTransition("In Progress", "Resolved"));
    }

    @Test
    void testInProgressToCancelled_Valid() {
        // Act & Assert
        assertTrue(validator.isValidTransition("In Progress", "Cancelled"));
    }

    @Test
    void testInProgressToOpen_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("In Progress", "Open"));
    }

    @Test
    void testInProgressToClosed_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("In Progress", "Closed"));
    }

    @Test
    void testInProgressToInProgress_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("In Progress", "In Progress"));
    }

    // ===================== RESOLVED State Transitions =====================

    @Test
    void testResolvedToClosed_Valid() {
        // Act & Assert
        assertTrue(validator.isValidTransition("Resolved", "Closed"));
    }

    @Test
    void testResolvedToInProgress_Valid() {
        // Act & Assert
        assertTrue(validator.isValidTransition("Resolved", "In Progress"));
    }

    @Test
    void testResolvedToOpen_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Resolved", "Open"));
    }

    @Test
    void testResolvedToCancelled_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Resolved", "Cancelled"));
    }

    @Test
    void testResolvedToResolved_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Resolved", "Resolved"));
    }

    // ===================== CLOSED State Transitions (Terminal) =====================

    @Test
    void testClosedToOpen_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Closed", "Open"));
    }

    @Test
    void testClosedToInProgress_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Closed", "In Progress"));
    }

    @Test
    void testClosedToResolved_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Closed", "Resolved"));
    }

    @Test
    void testClosedToCancelled_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Closed", "Cancelled"));
    }

    @Test
    void testClosedToClosed_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Closed", "Closed"));
    }

    // ===================== CANCELLED State Transitions (Terminal) =====================

    @Test
    void testCancelledToOpen_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Cancelled", "Open"));
    }

    @Test
    void testCancelledToInProgress_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Cancelled", "In Progress"));
    }

    @Test
    void testCancelledToResolved_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Cancelled", "Resolved"));
    }

    @Test
    void testCancelledToClosed_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Cancelled", "Closed"));
    }

    @Test
    void testCancelledToCancelled_Invalid() {
        // Act & Assert
        assertFalse(validator.isValidTransition("Cancelled", "Cancelled"));
    }

    // ===================== GET VALID NEXT STATES =====================

    @Test
    void testGetValidNextStatesOpen() {
        // Act
        Set<String> validStates = validator.getValidNextStates("Open");

        // Assert
        assertEquals(2, validStates.size());
        assertTrue(validStates.contains("In Progress"));
        assertTrue(validStates.contains("Cancelled"));
        assertFalse(validStates.contains("Open"));
        assertFalse(validStates.contains("Resolved"));
        assertFalse(validStates.contains("Closed"));
    }

    @Test
    void testGetValidNextStatesInProgress() {
        // Act
        Set<String> validStates = validator.getValidNextStates("In Progress");

        // Assert
        assertEquals(2, validStates.size());
        assertTrue(validStates.contains("Resolved"));
        assertTrue(validStates.contains("Cancelled"));
        assertFalse(validStates.contains("Open"));
        assertFalse(validStates.contains("In Progress"));
        assertFalse(validStates.contains("Closed"));
    }

    @Test
    void testGetValidNextStatesResolved() {
        // Act
        Set<String> validStates = validator.getValidNextStates("Resolved");

        // Assert
        assertEquals(2, validStates.size());
        assertTrue(validStates.contains("Closed"));
        assertTrue(validStates.contains("In Progress"));
        assertFalse(validStates.contains("Open"));
        assertFalse(validStates.contains("Resolved"));
        assertFalse(validStates.contains("Cancelled"));
    }

    @Test
    void testGetValidNextStatesClosed() {
        // Act - Closed is terminal
        Set<String> validStates = validator.getValidNextStates("Closed");

        // Assert
        assertEquals(0, validStates.size());
        assertTrue(validStates.isEmpty());
    }

    @Test
    void testGetValidNextStatesCancelled() {
        // Act - Cancelled is terminal
        Set<String> validStates = validator.getValidNextStates("Cancelled");

        // Assert
        assertEquals(0, validStates.size());
        assertTrue(validStates.isEmpty());
    }

    // ===================== VALIDATE TRANSITION (Exception Throwing) =====================

    @Test
    void testValidateTransitionValid() {
        // Act & Assert - Should not throw exception
        validator.validateTransition("Open", "In Progress");
    }

    @Test
    void testValidateTransitionInvalid_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            validator.validateTransition("Open", "Resolved");
        });
    }

    @Test
    void testValidateTransitionTerminalState_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            validator.validateTransition("Closed", "Open");
        });
    }

    // ===================== EDGE CASES =====================

    @Test
    void testNullCurrentStatus() {
        // Act & Assert - Implementation handles null gracefully
        assertFalse(validator.isValidTransition(null, "In Progress"));
    }

    @Test
    void testNullNewStatus() {
        // Act & Assert - Implementation handles null gracefully
        assertFalse(validator.isValidTransition("Open", null));
    }

    @Test
    void testEmptyCurrentStatus() {
        // Act & Assert
        assertFalse(validator.isValidTransition("", "In Progress"));
    }

    @Test
    void testCaseInsensitive() {
        // Act & Assert - Status values should be case-sensitive in the implementation
        // If implementation handles "open" differently from "Open", this test documents that
        assertFalse(validator.isValidTransition("open", "in progress"));
    }

    @Test
    void testInvalidStatus() {
        // Act & Assert
        assertFalse(validator.isValidTransition("InvalidStatus", "Open"));
        assertFalse(validator.isValidTransition("Open", "InvalidStatus"));
    }

    // ===================== FULL WORKFLOW TEST =====================

    @Test
    void testCompleteTicketLifecycle() {
        // Test a valid ticket lifecycle: Open → In Progress → Resolved → Closed

        // Step 1: Open → In Progress
        assertTrue(validator.isValidTransition("Open", "In Progress"));

        // Step 2: In Progress → Resolved
        assertTrue(validator.isValidTransition("In Progress", "Resolved"));

        // Step 3: Resolved → Closed
        assertTrue(validator.isValidTransition("Resolved", "Closed"));

        // Step 4: Closed is terminal
        Set<String> closedStates = validator.getValidNextStates("Closed");
        assertTrue(closedStates.isEmpty());
    }

    @Test
    void testAlternativeLifecycleWithCancel() {
        // Test alternative lifecycle: Open → Cancelled (terminal at any point)

        // Step 1: Open → Cancelled (valid at any point)
        assertTrue(validator.isValidTransition("Open", "Cancelled"));

        // Step 2: Cancelled is terminal
        Set<String> cancelledStates = validator.getValidNextStates("Cancelled");
        assertTrue(cancelledStates.isEmpty());
    }

    @Test
    void testRejectedPath() {
        // Test invalid path: Open → Resolved should be rejected
        // Correct path would be: Open → In Progress → Resolved

        // Direct transition invalid
        assertFalse(validator.isValidTransition("Open", "Resolved"));

        // Verify correct path
        assertTrue(validator.isValidTransition("Open", "In Progress"));
        assertTrue(validator.isValidTransition("In Progress", "Resolved"));
    }
}
