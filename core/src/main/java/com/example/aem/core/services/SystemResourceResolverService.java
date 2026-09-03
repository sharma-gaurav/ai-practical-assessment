package com.example.aem.core.services;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Service for obtaining system resource resolver with elevated privileges.
 * Used for backend operations that require full JCR permissions.
 */
public interface SystemResourceResolverService {

    /**
     * Get a system resource resolver for the specified service user.
     * Caller is responsible for closing the resolver.
     *
     * @param serviceUser the service user identifier
     * @return ResourceResolver with system permissions
     * @throws RuntimeException if resolver cannot be obtained
     */
    ResourceResolver getSystemResourceResolver(String serviceUser);

    /**
     * Execute an operation with a system resource resolver.
     * The resolver is automatically managed and closed after operation.
     *
     * @param serviceUser the service user identifier
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     */
    <T> T executeWithSystemResolver(String serviceUser, ResolverOperation<T> operation);

    /**
     * Functional interface for operations that need system resource resolver.
     */
    @FunctionalInterface
    interface ResolverOperation<T> {
        T execute(ResourceResolver resolver) throws Exception;
    }
}
