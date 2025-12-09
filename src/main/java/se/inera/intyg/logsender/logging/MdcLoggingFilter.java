/*
 * Copyright (C) 2025 Inera AB (http://www.inera.se)
 *
 * This file is part of sklintyg (https://github.com/sklintyg).
 *
 * sklintyg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * sklintyg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.inera.intyg.logsender.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that adds MDC (Mapped Diagnostic Context) values for distributed tracing.
 * Generates trace and span IDs for each HTTP request to enable correlation across logs.
 *
 * This filter runs early in the filter chain to ensure MDC context is available for all
 * downstream processing and logging.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter implements Filter {

    private final MdcHelper mdcHelper;

    public MdcLoggingFilter(MdcHelper mdcHelper) {
        this.mdcHelper = mdcHelper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // Set MDC context with trace and span IDs
            MDC.put(MdcLogConstants.TRACE_ID_KEY, mdcHelper.traceId());
            MDC.put(MdcLogConstants.SPAN_ID_KEY, mdcHelper.spanId());

            // Add request URI for debugging
            MDC.put("request.uri", httpRequest.getRequestURI());
            MDC.put("request.method", httpRequest.getMethod());

            // Continue the filter chain with MDC context in place
            chain.doFilter(request, response);

        } finally {
            // Always clean up MDC to prevent memory leaks and context pollution
            MDC.remove(MdcLogConstants.TRACE_ID_KEY);
            MDC.remove(MdcLogConstants.SPAN_ID_KEY);
            MDC.remove("request.uri");
            MDC.remove("request.method");
            MDC.clear();
        }
    }
}

