/*
 * Copyright (c) 2014-2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.helpers;

/**
 * Created by ALEX on 3/18/16.
 */

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

public class RestAuthenticationFilter implements javax.servlet.Filter {
    public static final String AUTHENTICATION_HEADER = "Authorization";

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain filter) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            if (((HttpServletRequest) request).getMethod().equals(HttpMethod.OPTIONS.toString())) {
                if (response instanceof HttpServletResponse) {
                    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                    httpServletResponse.addHeader("Access-Control-Allow-Credentials", "true");
                    httpServletResponse.addHeader("Access-Control-Allow-Headers", "Accept, Authorization");
                    httpServletResponse.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, HEAD");
                    httpServletResponse.addHeader("Access-Control-Allow-Origin", "*");
                    httpServletResponse.setStatus(200);
                }
            } else {
                String path = ((HttpServletRequest) request).getPathInfo();
                boolean authenticationStatus;
                System.out.println("Path = " + path);

                /*
                 * Ignore/skip paths that do not require auth
                 */
                if (path.startsWith("/events")) {
                    System.out.println("No auth required for path");
                    filter.doFilter(request, response);
                    return;
                }

                if (!path.contains("/auth/login")) {
                    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                    String authCredentials = httpServletRequest
                            .getHeader(AUTHENTICATION_HEADER);

                    // better injected
                    AuthenticationService authenticationService = new AuthenticationService();
                    authenticationStatus = authenticationService
                            .authenticate(authCredentials);
                } else {
                    authenticationStatus = true;
                }
                if (!authenticationStatus) {
                    if (response instanceof HttpServletResponse) {
                        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                        httpServletResponse.addHeader("Access-Control-Allow-Credentials", "true");
                        httpServletResponse.addHeader("Access-Control-Allow-Headers", "Accept, Authorization");
                        httpServletResponse.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, HEAD");
                        httpServletResponse.addHeader("Access-Control-Allow-Origin", "*");
                        httpServletResponse
                                .setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        // Allow the browser to prompt for user/pass using basic auth
                        httpServletResponse.setHeader("WWW-Authenticate", "Basic");
                    }
                } else {
                    filter.doFilter(request, response);
                }
            }
        }
    }

    public void destroy() {
    }

    public void init(FilterConfig arg0) throws ServletException {
    }
}