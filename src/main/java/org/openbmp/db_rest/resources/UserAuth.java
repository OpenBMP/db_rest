package org.openbmp.db_rest.resources;

/**
 * Created by ALEX on 3/18/16.
 */
/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

import java.sql.SQLException;

@Path("/auth")
public class UserAuth {
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

    @POST
    @Path("/login")
    @Produces("text/plain")
    public Response login(@FormDataParam("username") String username,
                          @FormDataParam("password") String password) {
        if (username == null)
            return Response.status(400).entity(
                    "{ \"error\": \"Missing required path parameter\" }").build();

        StringBuilder query = new StringBuilder();

        query.append("  SELECT * FROM users\n");
        query.append("      WHERE username=\"" + username + "\" AND password=PASSWORD(\"" + password + "\")\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @GET
    @Path("/getAllUsers")
    @Produces("text/plain")
    public Response getAllUsers() {
        StringBuilder query = new StringBuilder();

        query.append("  SELECT * FROM users\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @POST
    @Path("/update")
    @Produces("text/plain")
    public Response updateUser(@FormDataParam("username") String username,
                               @FormDataParam("column") String column,
                               @FormDataParam("value") String value) {

        StringBuilder query = new StringBuilder();

        query.append("UPDATE users \n");
        query.append("    SET " + column + "=");
        if (column.toLowerCase().equals("password"))
            query.append("PASSWORD('" + value + "')\n");
        else
            query.append("'" + value + "'\n");
        query.append("    WHERE username='" + username + "'\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        try {
            return RestResponse.okWithBody(
                    Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));
        } catch (SQLException e) {
            e.printStackTrace();
            return RestResponse.okWithBody(e.getMessage());
        }
    }

    @POST
    @Path("/insert")
    @Produces("text/plain")
    public Response insertUser(@QueryParam("username") String username,
                               @QueryParam("password") String password,
                               @QueryParam("usertype") String usertype) {

        StringBuilder query = new StringBuilder();

        query.append("INSERT INTO users \n");
        query.append("    (username,password,type)\n");
        query.append("    VALUES ('" + username + "',PASSWORD('" + password + "'),'" + usertype + "')\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        try {
            return RestResponse.okWithBody(
                    Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));
        } catch (SQLException e) {
            e.printStackTrace();
            return RestResponse.okWithBody(e.getMessage());
        }
    }

    @GET
    @Path("/delete/{username}")
    @Produces("text/plain")
    public Response deleteUser(@PathParam("username") String username) {

        StringBuilder query = new StringBuilder();

        query.append("DELETE FROM users \n");
        query.append("    WHERE username='" + username + "'\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        try {
            return RestResponse.okWithBody(
                    Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));
        } catch (SQLException e) {
            e.printStackTrace();
            return RestResponse.okWithBody(e.getMessage());
        }

    }
}
