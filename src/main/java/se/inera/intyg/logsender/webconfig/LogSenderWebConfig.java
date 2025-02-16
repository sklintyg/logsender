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
package se.inera.intyg.logsender.webconfig;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import se.inera.intyg.infra.monitoring.logging.LogbackConfiguratorContextListener;
import se.inera.intyg.logsender.config.LogSenderAppConfig;

public class LogSenderWebConfig implements WebApplicationInitializer {

    @Override
    public void onStartup(@Nonnull ServletContext servletContext) {

        AnnotationConfigWebApplicationContext webAppContext = new AnnotationConfigWebApplicationContext();
        webAppContext.setDisplayName("Logsender Web Application");
        webAppContext.register(LogSenderAppConfig.class);
        webAppContext.setServletContext(servletContext);
        servletContext.setInitParameter("logbackConfigParameter", "logback.file");
        servletContext.addListener(new LogbackConfiguratorContextListener());
        servletContext.addListener(new ContextLoaderListener(webAppContext));

        ServletRegistration.Dynamic cxfServlet = servletContext
            .addServlet("ws", new CXFServlet());
        cxfServlet.setLoadOnStartup(1);
        cxfServlet.addMapping("/*");

        ServletRegistration.Dynamic versionServlet = servletContext
            .addJspFile("version", "/version.jsp");
        versionServlet.addMapping("/version");
    }
}
