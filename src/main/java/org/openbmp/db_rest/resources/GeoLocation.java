/*
 * Copyright (c) 2014-2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

@Path("/geolocation")
public class GeoLocation {
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
    @Path("/{countryCode}/{city}")
    @Produces("application/json")
    public Response getCoordinates(@PathParam("countryCode") String countryCode,
                                   @PathParam("city") String city) {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("    SELECT latitude,longitude FROM geo_location\n");
        query.append("        WHERE country = '" + countryCode + "'\n");
        query.append("        AND city = LOWER('" + city + "')\n ");
        query.append("        LIMIT 1\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @GET
    @Path("/get/{page}/{limit}")
    @Produces("application/json")
    public Response getGeoLocationList(@PathParam("page") int page,
                                       @PathParam("limit") int limit,
                                       @QueryParam("sort") String sort,
                                       @QueryParam("sortDirection") String sortDirection) {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT * FROM geo_location\n");
        if (sort != null && sortDirection != null)
            query.append("     ORDER BY " + sort + " " + sortDirection + "\n");
        query.append("     LIMIT " + (page - 1) * 1000 + "," + limit + "   \n ");


        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @GET
    @Path("/getcount")
    @Produces("application/json")
    public Response getGeoLocationCount() {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT COUNT(*) as COUNT FROM geo_location\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @POST
    @Path("/insert")
    @Produces("application/json")
    public Response insertGeoIP(@QueryParam("country") String country,
                                @QueryParam("city") String city,
                                @QueryParam("latitude") String latitude,
                                @QueryParam("longitude") String longitude) {

        StringBuilder query = new StringBuilder();

        query.append("INSERT INTO geo_location \n");
        query.append("    (country,city,latitude,longitude)\n");
        query.append("    VALUES ('" + country + "',LOWER('" + city + "')," + latitude + "," + longitude +")\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));
    }

    @GET
    @Path("/update/{country}/{city}/{col}/{value}")
    @Produces("application/json")
    public Response updateGeoLocation(@PathParam("country") String country,
                                      @PathParam("city") String city,
                                      @PathParam("col") String column,
                                      @PathParam("value") String value) {

        StringBuilder query = new StringBuilder();

        boolean valueIsString = true;

        if (column == "latitude" || column == "longitude")
            valueIsString = false;

        query.append("UPDATE geo_location \n");
        query.append("    SET " + column + "=" + (valueIsString ? ("'" + value + "'") : value) + "\n");
        query.append("    WHERE country='" + country + "' AND city='" + city + "'\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));
    }

    @GET
    @Path("/delete/{country}/{city}")
    @Produces("application/json")
    public Response deleteGeoLocation(@PathParam("country") String country,
                                      @PathParam("city") String city) {

        StringBuilder query = new StringBuilder();

        query.append("DELETE FROM geo_location \n");
        query.append("    WHERE country='" + country + "' AND city='" + city + "'\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));

    }

}
