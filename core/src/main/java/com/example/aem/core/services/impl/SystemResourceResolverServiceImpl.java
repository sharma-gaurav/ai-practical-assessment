package com.example.aem.core.services.impl;

import java.util.Collections;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.aem.core.services.SystemResourceResolverService;

@Component(service = SystemResourceResolverService.class)
public class SystemResourceResolverServiceImpl implements SystemResourceResolverService {

    private static final Logger logger = LoggerFactory.getLogger(SystemResourceResolverServiceImpl.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public ResourceResolver getSystemResourceResolver(String serviceUser) {
        try {
            return resolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, serviceUser)
            );
        } catch (Exception e) {
            logger.error("Failed to get system resource resolver for service user: {}", serviceUser, e);
            throw new RuntimeException("Failed to get system resource resolver: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T executeWithSystemResolver(String serviceUser, ResolverOperation<T> operation) {
        ResourceResolver resolver = null;
        try {
            resolver = getSystemResourceResolver(serviceUser);
            return operation.execute(resolver);
        } catch (RuntimeException e) {
            // Propagate unchecked exceptions unchanged: callers discriminate on their type,
            // e.g. an IllegalStateException from the state machine must reach the servlet
            // intact so it can be answered with 409 rather than 500.
            logger.error("Error executing operation with system resolver for service user: {}", serviceUser, e);
            throw e;
        } catch (Exception e) {
            logger.error("Error executing operation with system resolver for service user: {}", serviceUser, e);
            throw new RuntimeException("Operation failed: " + e.getMessage(), e);
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }
}
