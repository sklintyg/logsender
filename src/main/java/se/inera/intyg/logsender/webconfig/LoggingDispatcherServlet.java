/*
 * Copyright (C) 2020 Inera AB (http://www.inera.se)
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

package se.inera.intyg.logsender.webconfig;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

public class LoggingDispatcherServlet extends DispatcherServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingDispatcherServlet.class);
    private static final int PAYLOAD_LOG_SIZE = 50;

    public LoggingDispatcherServlet(AnnotationConfigWebApplicationContext webAppContext) {
        super(webAppContext);
    }

    @Override
    protected void doDispatch(HttpServletRequest request, @Nonnull HttpServletResponse response) throws Exception {

        HttpServletRequest req = request;
        HttpServletResponse resp = response;
        if (!(request instanceof ContentCachingRequestWrapper)) {
            req = new ContentCachingRequestWrapper(request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            resp = new ContentCachingResponseWrapper(response);
        }

        HandlerExecutionChain handler = getHandler(req);
        try {
            super.doDispatch(req, resp);
        } finally {
            if (handler != null) {
                log(req, resp, handler);
                updateResponse(resp);
            }
        }
    }

    private void log(HttpServletRequest requestToCache, HttpServletResponse responseToCache, HandlerExecutionChain handler) {
        LogMessage logMessage = new LogMessage();
        logMessage.setHttpStatus(responseToCache.getStatus());
        logMessage.setHttpMethod(requestToCache.getMethod());
        logMessage.setPath(requestToCache.getRequestURI());
        logMessage.setClientIp(requestToCache.getRemoteAddr());
        logMessage.setJavaMethod(handler.toString());
        logMessage.setResponse(getResponseHasPayload(responseToCache));
        LOG.info(logMessage.toString());
    }

    private String getResponseHasPayload(HttpServletResponse resp) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(resp, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            int length = Math.min(buf.length, PAYLOAD_LOG_SIZE);
            try {
                return new String(buf, 0, length, wrapper.getCharacterEncoding())
                    + (buf.length > PAYLOAD_LOG_SIZE ? "'...(truncated by Logger)" : "'");
            } catch (UnsupportedEncodingException ex) {
                // NOOP
            }
        }
        return "[unknown]";
    }

    private void updateResponse(HttpServletResponse resp) throws IOException {
        ContentCachingResponseWrapper responseWrapper =
            WebUtils.getNativeResponse(resp, ContentCachingResponseWrapper.class);
        if (responseWrapper != null) {
            responseWrapper.copyBodyToResponse();
        }
    }

    private static class LogMessage {
        private int httpStatus;
        private String responsePayload;
        private String javaMethod;
        private String clientIp;
        private String path;
        private String method;

        public void setHttpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
        }
        public void setResponse(String responsePayload) {
            this.responsePayload = responsePayload;
        }
        public void setJavaMethod(String javaMethod) {
            this.javaMethod = javaMethod;
        }
        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }
        public void setPath(String path) {
            this.path = path;
        }
        public void setHttpMethod(String method) {
            this.method = method;
        }

        @Override
        public String toString() {
            return "{"
                + "httpStatus=" + httpStatus
                + ", responsePayload='" + responsePayload
                    .replaceAll("(\r\n|\n)", " ")
                    .replaceAll("  +", "")
                + ", javaMethod='" + javaMethod + '\''
                + ", clientIp='" + clientIp + '\''
                + ", path='" + path + '\''
                + ", method='" + method + '\''
                + '}';
        }
    }

}
