package com.example.aem.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InvalidUserExceptionTest {

    @Test
    void carriesMessage() {
        InvalidUserException e = new InvalidUserException("User does not exist: ghost");

        assertEquals("User does not exist: ghost", e.getMessage());
    }

    @Test
    void carriesMessageAndCause() {
        Throwable cause = new IllegalArgumentException("root cause");

        InvalidUserException e = new InvalidUserException("User does not exist: ghost", cause);

        assertEquals("User does not exist: ghost", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    void isUncheckedSoItPropagatesThroughResolverCallbacks() {
        assertTrue(RuntimeException.class.isAssignableFrom(InvalidUserException.class));
    }
}
