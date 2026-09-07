package com.example.aem.core.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.aem.core.services.SystemResourceResolverService;

/**
 * Tests {@link SystemResourceResolverServiceImpl}, in particular its exception-translation
 * contract: unchecked exceptions must reach the caller with their type intact so that
 * servlets can map them to the right HTTP status.
 */
class SystemResourceResolverServiceImplTest {

    private ResourceResolverFactory factory;
    private ResourceResolver resolver;
    private SystemResourceResolverService service;

    @BeforeEach
    void setUp() throws Exception {
        factory = mock(ResourceResolverFactory.class);
        resolver = mock(ResourceResolver.class);
        when(resolver.isLive()).thenReturn(true);
        when(factory.getServiceResourceResolver(anyMap())).thenReturn(resolver);

        // Injected directly rather than through AemContext, which registers its own
        // ResourceResolverFactory and would win the @Reference binding.
        SystemResourceResolverServiceImpl impl = new SystemResourceResolverServiceImpl();
        Field field = SystemResourceResolverServiceImpl.class.getDeclaredField("resolverFactory");
        field.setAccessible(true);
        field.set(impl, factory);
        service = impl;
    }

    @Test
    void getSystemResourceResolverReturnsFactoryResolver() {
        assertSame(resolver, service.getSystemResourceResolver("test-service"));
    }

    @Test
    void getSystemResourceResolverWrapsFactoryFailure() throws Exception {
        when(factory.getServiceResourceResolver(anyMap()))
            .thenThrow(new org.apache.sling.api.resource.LoginException("no such service user"));

        RuntimeException e = assertThrows(RuntimeException.class,
            () -> service.getSystemResourceResolver("missing-service"));

        assertTrue(e.getMessage().contains("Failed to get system resource resolver"));
    }

    @Test
    void executeWithSystemResolverReturnsOperationResult() {
        String result = service.executeWithSystemResolver("test-service", r -> "computed");

        assertEquals("computed", result);
    }

    @Test
    void executeWithSystemResolverPassesResolverToOperation() {
        service.executeWithSystemResolver("test-service", r -> {
            assertSame(resolver, r);
            return null;
        });
    }

    @Test
    void executeWithSystemResolverClosesResolverOnSuccess() {
        service.executeWithSystemResolver("test-service", r -> "done");

        verify(resolver).close();
    }

    @Test
    void executeWithSystemResolverClosesResolverOnFailure() {
        assertThrows(RuntimeException.class, () -> service.executeWithSystemResolver("test-service", r -> {
            throw new IllegalStateException("boom");
        }));

        verify(resolver).close();
    }

    @Test
    void executeWithSystemResolverPreservesIllegalStateExceptionType() {
        // The servlet answers 409 only if the state machine's IllegalStateException
        // survives this call unchanged; wrapping it would degrade the response to 500.
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> service.executeWithSystemResolver("test-service", r -> {
                throw new IllegalStateException("Invalid status transition");
            }));

        assertEquals("Invalid status transition", e.getMessage());
    }

    @Test
    void executeWithSystemResolverPreservesRuntimeExceptionMessage() {
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> service.executeWithSystemResolver("test-service", r -> {
                throw new RuntimeException("Ticket not found: ticket-123");
            }));

        assertTrue(e.getMessage().contains("not found"),
            "Callers match on this text to produce a 404");
    }

    @Test
    void executeWithSystemResolverWrapsCheckedExceptions() {
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> service.executeWithSystemResolver("test-service", r -> {
                throw new IOException("disk error");
            }));

        assertTrue(e.getMessage().contains("Operation failed"));
        assertNotNull(e.getCause());
        assertTrue(e.getCause() instanceof IOException);
    }
}
