package com.pfa.interviewai.security;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

@WebFilter("*.xhtml")
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PAGES = Set.of(
        "/landing.xhtml", "/login.xhtml", "/register.xhtml", "/error.xhtml"
    );

    @Inject
    private JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                          FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getServletPath();

        if (isPublic(path)) {
            chain.doFilter(req, res);
            return;
        }

        String token = extractTokenFromCookie(request);

        if (token != null && jwtUtil.isTokenValid(token)) {
            request.setAttribute("currentUserId",    jwtUtil.extractUserId(token));
            request.setAttribute("currentUserEmail", jwtUtil.extractEmail(token));
            request.setAttribute("currentUserRole",  jwtUtil.extractRole(token));

            if (path.startsWith("/admin/") &&
                !"ADMIN".equals(jwtUtil.extractRole(token))) {
                response.sendRedirect(request.getContextPath() + "/dashboard.xhtml");
                return;
            }

            chain.doFilter(req, res);
        } else {
            response.sendRedirect(request.getContextPath() + "/login.xhtml");
        }
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "interview_jwt".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PAGES.contains(path) ||
               path.startsWith("/jakarta.faces") ||
               path.contains("/resources/");
    }
}
