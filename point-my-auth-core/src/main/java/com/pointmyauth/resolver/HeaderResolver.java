package com.pointmyauth.resolver;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;

/**
 * Resolves HTTP header values using the {@code #header:} prefix.
 * <p>
 * A parameter name such as {@code "#header:X-Tenant-Id"} instructs this resolver
 * to extract the {@code X-Tenant-Id} header from the current {@link HttpServletRequest}.
 * <p>
 * <strong>Requirements:</strong> the consuming application must have an
 * {@link HttpServletRequest} available — typically in a Spring Web (Servlet) environment.
 * <p>
 * <strong>Example:</strong>
 * <pre>{@code
 * @AuthorizeEntity(ids = {"#header:X-Tenant-Id"}, ...)
 * public List<OrderDto> listOrders() { ... }
 * }</pre>
 */
public class HeaderResolver implements ParameterResolver {

    static final String HEADER_PREFIX = "#header:";

    private final HttpServletRequest httpServletRequest;

    /**
     * Creates a new header resolver backed by the given request.
     *
     * @param httpServletRequest the current HTTP servlet request
     */
    public HeaderResolver(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public Object resolve(String paramName, Method method, Object[] args) {
        if (!supports(paramName)) {
            return null;
        }
        String headerName = paramName.substring(HEADER_PREFIX.length());
        return httpServletRequest.getHeader(headerName);
    }

    @Override
    public boolean supports(String paramName) {
        return paramName != null && paramName.startsWith(HEADER_PREFIX);
    }
}
