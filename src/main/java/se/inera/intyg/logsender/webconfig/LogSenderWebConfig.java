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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebInitParam;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import se.inera.intyg.common.util.logging.LogbackConfiguratorContextListener;
import se.inera.intyg.logsender.config.LogSenderAppConfig;

@WebInitParam(name = "logbackConfigParameter", value = "logback.file")
public class LogSenderWebConfig implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

        AnnotationConfigWebApplicationContext webAppContext = new AnnotationConfigWebApplicationContext();
        webAppContext.setDisplayName("Logsender Web Application");
        webAppContext.register(LogSenderAppConfig.class);

        ServletRegistration.Dynamic cxfServlet = servletContext
            .addServlet("ws", new CXFServlet());
        cxfServlet.setLoadOnStartup(1);
        cxfServlet.addMapping("/*");

        ServletRegistration.Dynamic versionServlet = servletContext
            .addJspFile("version", "/webapp/version.jsp");
        versionServlet.addMapping("/version");

        servletContext.addListener(new LogbackConfiguratorContextListener());
        servletContext.addListener(new ContextLoaderListener());
    }
}
