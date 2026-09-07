package com.example.aem.core.testcontext;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;

import com.example.aem.core.services.SystemResourceResolverService;

import io.wcm.testing.mock.aem.junit5.AemContext;

/**
 * Test double for {@link SystemResourceResolverService} that runs operations against the
 * AemContext's mock JCR instead of acquiring a real service user resolver.
 *
 * <p>The session is exposed as a {@link JackrabbitSession} so that
 * {@code TicketServiceImpl.userExists()} can resolve users; membership is controlled by
 * {@link #addUser(String)} rather than a real principal store.
 *
 * <p>Exception wrapping mirrors {@code SystemResourceResolverServiceImpl} exactly, because
 * callers and servlets match on the resulting message text (e.g. "not found").
 */
public final class TestSystemResolverService implements SystemResourceResolverService {

    private final Set<String> knownUsers = new HashSet<>();
    private final ResourceResolver resolver;
    private String currentUserId = "admin";

    public TestSystemResolverService(AemContext context) {
        Session realSession = context.resourceResolver().adaptTo(Session.class);
        if (realSession == null) {
            throw new IllegalStateException(
                "AemContext must use a JCR-backed ResourceResolverType (e.g. JCR_MOCK)");
        }

        UserManager userManager = mock(UserManager.class);
        Authorizable authorizable = mock(Authorizable.class);
        JackrabbitSession jackrabbitSession = mock(JackrabbitSession.class, delegatesTo(realSession));
        try {
            // Resolved against the mutable knownUsers set at call time, so addUser()
            // can be called after construction.
            doAnswer(inv -> knownUsers.contains((String) inv.getArgument(0)) ? authorizable : null)
                .when(userManager).getAuthorizable(anyString());
            doReturn(userManager).when(jackrabbitSession).getUserManager();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stub JCR user management", e);
        }

        this.resolver = mock(ResourceResolver.class);
        doReturn(jackrabbitSession).when(this.resolver).adaptTo(Session.class);
        doReturn(true).when(this.resolver).isLive();
        doAnswer(inv -> currentUserId).when(this.resolver).getUserID();
    }

    /** Marks a user id as existing, so assignment validation accepts it. */
    public TestSystemResolverService addUser(String userId) {
        knownUsers.add(userId);
        return this;
    }

    /** Sets the id returned by {@code resolver.getUserID()}, used for createdBy. */
    public TestSystemResolverService withCurrentUser(String userId) {
        this.currentUserId = userId;
        return this;
    }

    @Override
    public ResourceResolver getSystemResourceResolver(String serviceUser) {
        return resolver;
    }

    @Override
    public <T> T executeWithSystemResolver(String serviceUser, ResolverOperation<T> operation) {
        try {
            return operation.execute(resolver);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Operation failed: " + e.getMessage(), e);
        }
    }
}
