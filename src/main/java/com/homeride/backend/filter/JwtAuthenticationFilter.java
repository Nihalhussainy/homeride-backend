package com.homeride.backend.filter;

import com.homeride.backend.service.EmployeeService;
import com.homeride.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmployeeService employeeService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT validation for public endpoints
        String requestPath = request.getRequestURI();
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.employeeService.loadUserByUsername(username);
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestPath) {
        // Health check endpoints
        if (requestPath.startsWith("/health")) return true;
        if (requestPath.startsWith("/actuator")) return true;

        // WebSocket endpoints
        if (requestPath.startsWith("/ws")) return true;
        if (requestPath.startsWith("/app/")) return true;
        if (requestPath.startsWith("/topic/")) return true;
        if (requestPath.startsWith("/user/")) return true;

        // Public API endpoints
        if (requestPath.startsWith("/api/auth/")) return true;
        if (requestPath.startsWith("/api/contact/")) return true;
        if (requestPath.startsWith("/api/locations/cities")) return true;
        if (requestPath.startsWith("/api/places/")) return true;
        if (requestPath.startsWith("/api/proxy/")) return true;
        if (requestPath.startsWith("/uploads/")) return true;
        if (requestPath.startsWith("/api/test/public")) return true;
        if (requestPath.equals("/api/rides/travel-info")) return true;

        return false;
    }
}