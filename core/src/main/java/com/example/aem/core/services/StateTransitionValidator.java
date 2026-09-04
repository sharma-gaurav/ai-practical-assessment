package com.example.aem.core.services;

import java.util.Set;

public interface StateTransitionValidator {

    boolean isValidTransition(String currentStatus, String nextStatus);

    Set<String> getValidNextStates(String currentStatus);

    void validateTransition(String currentStatus, String nextStatus) throws IllegalStateException;
}
