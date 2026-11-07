package com.mybudget.server.config;

import com.mybudget.server.dto.subscription.SubscriptionInfo;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.services.subscription.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Read-only mode: after the subscription (and its grace days) has ended, a user can
 * still sign in and read everything, but every request that changes data is refused
 * with 402 and a code the frontend recognises. Admins are never blocked.
 * Registered for /api/user/** in WebConfig.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (READ_METHODS.contains(request.getMethod())) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true; // Spring Security answers this request
        }
        return userRepository.findByUsername(auth.getName())
                .map(user -> {
                    SubscriptionInfo info = subscriptionService.infoOf(user);
                    if (info.canWrite()) {
                        return true;
                    }
                    response.setStatus(402);
                    response.setContentType("application/json");
                    try {
                        response.getWriter().write("{\"error\":\"SUBSCRIPTION_EXPIRED\",\"endsAt\":\""
                                + info.endsAt() + "\"}");
                    } catch (java.io.IOException ignored) {
                        // the status is already set
                    }
                    return false;
                })
                .orElse(true);
    }
}
