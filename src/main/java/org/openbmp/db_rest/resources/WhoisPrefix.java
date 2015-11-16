/*
 * Copyright (c) 2014-2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import org.openbmp.db_rest.DbUtils;
import org.openbmp.db_rest.RestResponse;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/whois/prefix")
public class WhoisPrefix {
    @Context
    ServletContext ctx;
    @Context
    UriInfo uri;

    private DataSource mysql_ds;

    /**
     * Initialize the class Sets the data source
     *
     * @throws
     */
    @PostConstruct
    public void init() {
        InitialContext initctx = null;
        try {

            initctx = new InitialContext();
            mysql_ds = (DataSource) initctx
                    .lookup("java:/comp/env/jdbc/MySQLDB");

        } catch (NamingException e) {
            System.err
                    .println("ERROR: Cannot find resource configuration, check context.xml config");
            e.printStackTrace();
        }
    }

    @GET
    @Produces("application/json")
    public Response getWhois(@QueryParam("where") String where,
                             @QueryParam("limit") Integer limit) {

        if (where == null) {
            System.out.println("Bad request, no where clause");
            return Response.status(400).entity("")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET")
                    .allow("OPTIONS")
                    .build();
        }

        String limit_str = "";
        if (limit != null && limit >= 1 && limit <= 500000)
            limit_str += " limit " + limit;

        StringBuilder query = new StringBuilder();
        query.append("SELECT inet6_ntoa(prefix) as prefix,prefix_len,descr,origin_as,source\n");
        query.append("    FROM gen_whois_route \n");
        query.append("    WHERE ");
        query.append(where);
        query.append("    GROUP BY prefix,prefix_len\n");
        query.append(limit_str);

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @GET
    @Path("/count")
    @Produces("application/json")
    public Response getWhoisCount(@QueryParam("where") String where) {

        if (where == null) {
            System.out.println("Bad request, no where clause");
            return Response.status(400).entity("")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET")
                    .allow("OPTIONS")
                    .build();
        }

        StringBuilder query = new StringBuilder();
        query.append("SELECT count(*) as count\n");
        query.append("    FROM gen_whois_route \n");
        query.append("    WHERE ");
        query.append(where);

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @GET
    @Path("/{prefix}/{prefix_len}")
    @Produces("application/json")
    public Response getWhoisAsn(@PathParam("prefix") String prefix,
                                @PathParam("prefix_len") Integer prefix_len,
                                @QueryParam("where") String where) {

        StringBuilder query = new StringBuilder();

        query.append("SELECT inet6_ntoa(prefix) as prefix,prefix_len,descr,origin_as,source\n");
        query.append("    FROM gen_whois_route\n");
        query.append("    WHERE prefix=inet6_aton('" + prefix + "')\n");
        query.append("    AND prefix_len=" + prefix_len + "\n");
        query.append("    GROUP BY prefix,prefix_len\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

}
