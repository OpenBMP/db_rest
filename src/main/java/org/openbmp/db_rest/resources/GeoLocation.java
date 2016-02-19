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

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;

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
        query.append("        WHERE country_code = '" + countryCode + "'\n");
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
                                       @QueryParam("where") String whereClause,
                                       @QueryParam("sort") String sort,
                                       @QueryParam("sortDirection") String sortDirection) {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT * FROM geo_location\n");
        if (whereClause != null && !whereClause.isEmpty())
            query.append(whereClause + "\n");
        if (sort != null && sortDirection != null)
            query.append("     ORDER BY " + sort + " " + sortDirection + "\n");
        query.append("     LIMIT " + (page - 1) * limit + "," + limit + "   \n ");


        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @GET
    @Path("/getcount")
    @Produces("application/json")
    public Response getGeoLocationCount(@QueryParam("where") String whereClause) {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT COUNT(*) as COUNT FROM geo_location\n");
        if (whereClause != null && !whereClause.isEmpty())
            query.append(whereClause + "\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @POST
    @Path("/insert")
    @Produces("text/plain")
    public Response insertGeoIP(@QueryParam("country_code") String country_code,
                                @QueryParam("city") String city,
                                @QueryParam("latitude") String latitude,
                                @QueryParam("longitude") String longitude) {

        StringBuilder query = new StringBuilder();

        query.append("INSERT INTO geo_location \n");
        query.append("    (country_code,city,latitude,longitude)\n");
        query.append("    VALUES ('" + country_code + "',LOWER('" + city + "')," + latitude + "," + longitude + ")\n");

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
    @Path("/update/{country_code}/{city}/{col}/{value}")
    @Produces("text/plain")
    public Response updateGeoLocation(@PathParam("country_code") String country_code,
                                      @PathParam("city") String city,
                                      @PathParam("col") String column,
                                      @PathParam("value") String value) {

        StringBuilder query = new StringBuilder();

        boolean valueIsString = true;

        if (column == "latitude" || column == "longitude")
            valueIsString = false;

        query.append("UPDATE geo_location \n");
        query.append("    SET " + column + "=" + (valueIsString ? ("'" + value + "'") : value) + "\n");
        query.append("    WHERE country_code='" + country_code + "' AND city='" + city + "'\n");

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
    @Path("/delete/{country_code}/{city}")
    @Produces("text/plain")
    public Response deleteGeoLocation(@PathParam("country_code") String country_code,
                                      @PathParam("city") String city) {

        StringBuilder query = new StringBuilder();

        query.append("DELETE FROM geo_location \n");
        query.append("    WHERE country_code='" + country_code + "' AND city='" + city + "'\n");

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
    @Path("/describe")
    @Produces("application/json")
    public Response describe() {

        StringBuilder query = new StringBuilder();

        query.append("DESCRIBE geo_location");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));

    }

    @POST
    @Path("/import")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public Response fromFile(@FormDataParam("file") InputStream file,
                             @FormDataParam("indexes") String indexes,
                             @FormDataParam("fields") String fields,
                             @FormDataParam("types") String types,
                             @DefaultValue(",") @FormDataParam("delimiter") String delimiter) throws IOException {

        int INSERT_THRESHOLD = 1000;
        int pointer = 1, affectedRows = 0;

        BufferedReader reader = new BufferedReader(new InputStreamReader(file));

        ArrayList<Integer> indexArray = new ArrayList<Integer>();

        ArrayList<String> fieldArray = new ArrayList<String>();

        ArrayList<String> typeArray = new ArrayList<String>();

        String statement = "DELETE FROM geo_location";
        try {
            System.out.println("Deleted: " + DbUtils.update_Db(mysql_ds, statement.toString()));
        } catch (SQLException e) {
            e.printStackTrace();
            return RestResponse.okWithBody(e.getMessage());
        }
        String[] tempArray = indexes.split(",");

        for (int i = 0; i < tempArray.length; i++) {
            int value = Integer.valueOf(tempArray[i]);
            if (value > -1) {
                indexArray.add(value);
                fieldArray.add(fields.split(",")[i]);
                typeArray.add(types.split(",")[i]);
            }
        }

        String columns = fieldArray.toString().substring(1, fieldArray.toString().length() - 1);

        String line;
        statement = "INSERT IGNORE INTO geo_location (" + columns + ") VALUES ";
        try {
            while ((line = reader.readLine()) != null) {
                if (pointer % INSERT_THRESHOLD == 0) {
                    statement = statement.substring(0, statement.length() - 1);
                    System.out.println("QUERY: \n" + statement.toString() + "\n");
                    affectedRows += DbUtils.update_Db(mysql_ds, statement.toString());
                    statement = "INSERT IGNORE INTO geo_location (" + columns + ") VALUES ";
                }
                statement += "(";
                String[] values = line.split(delimiter);
                for (int i = 0; i < indexArray.size(); i++) {
                    if (typeArray.get(i).contains("char"))
                        statement += "\"" + values[indexArray.get(i)].replace('\"', '\'') + "\",";
                    else
                        statement += values[indexArray.get(i)] + ",";
                }
                statement = statement.substring(0, statement.length() - 1);
                statement += "),";
                pointer++;
            }

            if (pointer % INSERT_THRESHOLD != 0) {
                statement = statement.substring(0, statement.length() - 1);
                System.out.println("QUERY: \n" + statement.toString() + "\n");
                affectedRows += DbUtils.update_Db(mysql_ds, statement.toString());
            }
        } catch (SQLException e) {
            return RestResponse.okWithBody(
                    "Exception:" + e.getMessage());
        }

        return RestResponse.okWithBody("Success! Rows inserted: " +
                Integer.toString(affectedRows));
    }

}
