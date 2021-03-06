/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.webconsole.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.util.HealthCheckFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/** Webconsole plugin to execute health check services */
@Component
@Service(Servlet.class)
@SuppressWarnings("serial")
@Properties({
    @Property(name=org.osgi.framework.Constants.SERVICE_DESCRIPTION, value="Sling Health Check Web Console Plugin"),
    @Property(name="felix.webconsole.label", value=HealthCheckWebconsolePlugin.LABEL),
    @Property(name="felix.webconsole.title", value=HealthCheckWebconsolePlugin.TITLE),
    @Property(name="felix.webconsole.category", value=HealthCheckWebconsolePlugin.CATEGORY),
    @Property(name="felix.webconsole.css", value="/healthcheck/res/ui/healthcheck.css")
})
public class HealthCheckWebconsolePlugin extends HttpServlet {

    public static final String TITLE = "Sling Health Check";
    public static final String LABEL = "healthcheck";
    public static final String CATEGORY = "Sling";
    public static final String PARAM_TAGS = "tags";
    public static final String PARAM_DEBUG = "debug";
    public static final String PARAM_QUIET = "quiet";

    @Reference
    private HealthCheckExecutor healthCheckExecutor;

    private BundleContext bundleContext;

    @Activate
    protected void activate(final BundleContext bc) {
        this.bundleContext = bc;
    }

    @Deactivate
    protected void deactivate() {
        this.bundleContext = null;
    }

    /** Serve static resource if applicable, and return true in that case */
    private boolean getStaticResource(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String pathInfo = req.getPathInfo();
        if(pathInfo!= null && pathInfo.contains("res/ui")) {
            final String prefix = "/" + LABEL;
            final InputStream is = getClass().getResourceAsStream(pathInfo.substring(prefix.length()));
            if(is == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, pathInfo);
            }
            final byte [] buffer = new byte[16384];
            int n=0;
            while( (n = is.read(buffer, 0, buffer.length)) > 0) {
                resp.getOutputStream().write(buffer, 0, n);
            }
            resp.getOutputStream().flush();
            return true;
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(getStaticResource(req, resp)) {
            return;
        }

        final String tags = getParam(req, PARAM_TAGS, "");
        final boolean debug = Boolean.valueOf(getParam(req, PARAM_DEBUG, "false"));
        final boolean quiet = Boolean.valueOf(getParam(req, PARAM_QUIET, "false"));

        doForm(req, resp, tags, debug, quiet);

        // Execute health checks only if tags are specified (even if empty)
        if(req.getParameter(PARAM_TAGS) != null) {
            final HealthCheckFilter filter = new HealthCheckFilter(this.bundleContext);
            try {
                final ServiceReference[] refs = filter.getTaggedHealthCheckServiceReferences(tags.split(","));
                Collection<HealthCheckExecutionResult> results = healthCheckExecutor.execute(refs);

                final PrintWriter pw = resp.getWriter();
                pw.println("<table class='content healthcheck' cellpadding='0' cellspacing='0' width='100%'>");
                int total = 0;
                int failed = 0;
                for (final HealthCheckExecutionResult exR : results) {

                    final Result r = exR.getHealthCheckResult();
                    total++;
                    if (!r.isOk()) {
                        failed++;
                    }
                    if (!quiet || !r.isOk()) {
                        renderResult(resp, exR, debug);
                    }

                }
                final WebConsoleHelper c = new WebConsoleHelper(resp.getWriter());
                c.titleHtml("Summary", total + " HealthCheck executed, " + failed + " failures");
                pw.println("</table>");
            } finally {
                filter.dispose();
            }
        }
    }

    private void renderResult(HttpServletResponse resp, HealthCheckExecutionResult exResult, boolean debug) throws IOException {
        final Result result = exResult.getHealthCheckResult();
        final WebConsoleHelper c = new WebConsoleHelper(resp.getWriter());

        final StringBuilder status = new StringBuilder();

        status.append("Tags: ").append(exResult.getHealthCheckTags());
        c.titleHtml(exResult.getHealthCheckName(), null);

        c.tr();
        c.tdContent();
        c.writer().print(ResponseUtil.escapeXml(status.toString()));
        c.writer().print("<br/>Result: <span class='resultOk");
        c.writer().print(result.isOk());
        c.writer().print("'>");
        c.writer().print(result.getStatus().toString());
        c.writer().print("</span>");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdContent();
        for(ResultLog.Entry e : result) {
            if(!debug && e.getStatus().equals(Result.Status.DEBUG)) {
                continue;
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("<div class='log").append(e.getStatus()).append("'>");
            sb.append(e.getStatus())
                .append(" ")
                .append(ResponseUtil.escapeXml(e.getMessage()))
                .append("</div>");
            c.writer().println(sb.toString());
        }
        c.closeTd();
    }

    private void doForm(HttpServletRequest req, HttpServletResponse resp, String tags, boolean debug, boolean quiet)
            throws IOException {
        final PrintWriter pw = resp.getWriter();
        final WebConsoleHelper c = new WebConsoleHelper(pw);
        pw.print("<form method='get'>");
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        c.titleHtml(TITLE, "To execute health check services, enter "
                + " an optional list of tags, to select specific health checks, or no tags for all checks."
                + " Prefix a tag with a minus sign (-) to omit checks having that tag.");

        c.tr();
        c.tdLabel("Health Check tags (comma-separated)");
        c.tdContent();
        pw.println("<input type='text' name='" + PARAM_TAGS + "' value='" + tags + "' class='input' size='80'>");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdLabel("Show DEBUG logs");
        c.tdContent();
        pw.println("<input type='checkbox' name='" + PARAM_DEBUG + "' class='input' value='true'"
                + (debug ? " checked=true " : "") + ">");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdLabel("Show failed checks only");
        c.tdContent();
        pw.println("<input type='checkbox' name='" + PARAM_QUIET + "' class='input' value='true'"
                + (quiet ? " checked=true " : "") + ">");
        c.closeTd();
        c.closeTr();

        c.tr();
        c.tdContent();
        pw.println("<input type='submit' value='Execute selected health checks'/>");
        c.closeTd();
        c.closeTr();

        pw.println("</table></form>");
    }

    private String getParam(HttpServletRequest req, String name, String defaultValue) {
        String result = req.getParameter(name);
        if(result == null) {
            result = defaultValue;
        }
        return result;
    }
}
