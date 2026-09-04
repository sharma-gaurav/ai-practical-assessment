package com.example.aem.core.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.example.aem.core.services.StateTransitionValidator;

@Component(service = StateTransitionValidator.class)
public class StateTransitionValidatorImpl implements StateTransitionValidator {

    private static final Map<String, Set<String>> STATE_TRANSITIONS = new HashMap<>();

    static {
        // Define valid state transitions
        Set<String> openTransitions = new HashSet<>();
        openTransitions.add("In Progress");
        openTransitions.add("Cancelled");
        STATE_TRANSITIONS.put("Open", Collections.unmodifiableSet(openTransitions));

        Set<String> inProgressTransitions = new HashSet<>();
        inProgressTransitions.add("Resolved");
        inProgressTransitions.add("Cancelled");
        STATE_TRANSITIONS.put("In Progress", Collections.unmodifiableSet(inProgressTransitions));

        Set<String> resolvedTransitions = new HashSet<>();
        resolvedTransitions.add("Closed");
        resolvedTransitions.add("In Progress");
        STATE_TRANSITIONS.put("Resolved", Collections.unmodifiableSet(resolvedTransitions));

        // Closed is terminal - no transitions allowed
        STATE_TRANSITIONS.put("Closed", Collections.unmodifiableSet(new HashSet<>()));

        // Cancelled is terminal - no transitions allowed
        STATE_TRANSITIONS.put("Cancelled", Collections.unmodifiableSet(new HashSet<>()));
    }

    @Override
    public boolean isValidTransition(String currentStatus, String nextStatus) {
        if (currentStatus == null || nextStatus == null) {
            return false;
        }

        Set<String> validNextStates = STATE_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        return validNextStates.contains(nextStatus);
    }

    @Override
    public Set<String> getValidNextStates(String currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }

        Set<String> validStates = STATE_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        return new HashSet<>(validStates);
    }

    @Override
    public void validateTransition(String currentStatus, String nextStatus) throws IllegalStateException {
        if (!isValidTransition(currentStatus, nextStatus)) {
            Set<String> validStates = getValidNextStates(currentStatus);
            String message = String.format(
                "Invalid status transition from '%s' to '%s'. Valid next states: %s",
                currentStatus, nextStatus, validStates.isEmpty() ? "none (terminal state)" : validStates
            );
            throw new IllegalStateException(message);
        }
    }
}
