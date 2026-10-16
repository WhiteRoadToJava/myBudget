package com.mybudget.server.filter;

import com.mybudget.server.util.SlidingWindowLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.rowset.serial.SerialException;
import java.io.IOException;
import java.util.Map;

public class RelateLimitFilter extends OncePerRequestFilter {

    private record Rule(int maxRequests, long windowMs) {
    }

    private static final long MINUTE = 60_000;

    // POST only. Tweak the numbers here (for example raise "register" while you are testing).
    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", new Rule(10, MINUTE),
            "/api/auth/register", new Rule(10, 60 * MINUTE),
            "/api/auth/forgot-password", new Rule(5, 15 * MINUTE),
            "/api/auth/resend-activation", new Rule(5, 15 * MINUTE),
            "/api/auth/activate", new Rule(10, 15 * MINUTE),
            "/api/auth/reset-password", new Rule(10, 15 * MINUTE)
    );

    private final SlidingWindowLimiter limiter = new SlidingWindowLimiter();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !RULES.containsKey(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        Rule rule = RULES.get(path);

        long retryAfter = limiter.tryAcquire(
                clientIp(request) + "|" + path,
                rule.maxRequests(),
                rule.windowMs(),
                System.currentTimeMillis());

        if (retryAfter > 0) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":\"RATE_LIMITED\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Behind Cloudflare the TCP peer is always cloudflared / nginx on this machine, so the real
     * client IP comes from a header. These headers can be forged by anyone who can reach port 8080
     * directly, so keep 8080 closed to the outside (only nginx / the tunnel should reach it).
     */
    private static String clientIp(HttpServletRequest request) {
        String cloudflare = request.getHeader("CF-Connecting-IP");
        if (cloudflare != null && !cloudflare.isBlank()) {
            return cloudflare.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
