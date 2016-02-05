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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;

@Path("/geoip")
public class GeoIp {
	@Context ServletContext ctx;
	@Context UriInfo uri;

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
	@Path("/{IP}")
	@Produces("application/json")
	public Response getRibByLookup(@PathParam("IP") String ip,
						   @QueryParam("where") String where) {

		StringBuilder query = new StringBuilder();

		String addrType = "ipv4";
		if (ip.indexOf(':') >= 0)
			addrType = "ipv6";

		// Query first for the prefix/len
		query.append("SELECT * FROM v_geo_ip\n");
		query.append("        WHERE ip_end_bin >= inet6_aton('" + ip + "')\n");
		query.append("               and ip_start_bin <= inet6_aton('" + ip + "') and addr_type = '" + addrType + "'\n ");

		if (where != null)
        	query.append(" AND " + where);

		query.append("        ORDER BY ip_end_bin limit 1\n ");


		System.out.println("QUERY: \n" + query.toString() + "\n");

		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

	@GET
    @Path("/get/{page}/{limit}")
    @Produces("application/json")
    public Response getGeoIPList(@PathParam("page") int page,
                                 @PathParam("limit") int limit,
                                 @QueryParam("where") String whereClause,
                                 @QueryParam("sort") String sort,
                                 @QueryParam("sortDirection") String sortDirection) {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT * FROM v_geo_ip\n");
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
    public Response getGeoIPCount(@QueryParam("where") String whereClause) {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT COUNT(*) as COUNT FROM v_geo_ip\n");
        if (whereClause != null && !whereClause.isEmpty())
            query.append(whereClause + "\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @POST
    @Path("/insert")
    @Produces("text/plain")
    public Response insertGeoIP(@QueryParam("ip_start") String ip_start,
                                @QueryParam("ip_end") String ip_end,
                                @QueryParam("country") String country,
                                @QueryParam("stateprov") String stateprov,
                                @QueryParam("city") String city,
                                @QueryParam("latitude") String latitude,
                                @QueryParam("longitude") String longitude,
                                @QueryParam("timezone_name") String timezone_name,
                                @QueryParam("timezone_offset") String timezone_offset,
                                @QueryParam("isp_name") String isp_name) {

        StringBuilder query = new StringBuilder();

        String addrType = "ipv4";
        if (ip_start.indexOf(':') >= 0)
            addrType = "ipv6";

        query.append("INSERT INTO geo_ip \n");
        query.append("    (ip_start,ip_end,country,stateprov,city,latitude,longitude,addr_type,timezone_name,timezone_offset,isp_name)\n");
        query.append("    VALUES (inet6_aton('" + ip_start + "'),inet6_aton('" + ip_end + "'),'" + country + "','" + stateprov + "','" + city + "'," + latitude + "," + longitude + ",'" + addrType + "','" + timezone_name + "'," + timezone_offset + ",'" + isp_name + "')\n");

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
    @Path("/update/{ip_start}/{col}/{value}")
    @Produces("text/plain")
    public Response updateGeoIP(@PathParam("ip_start") String ip_start,
                                 @PathParam("col") String column,
                                 @PathParam("value") String value) {

        StringBuilder query = new StringBuilder();

        boolean valueIsString = true;

        if(column=="latitude"||column=="longitude")
            valueIsString=false;

        query.append("UPDATE geo_ip \n");
        query.append("    SET " + column + "=" + (valueIsString ? ("'" + value + "'") : value) + "\n");
        query.append("    WHERE ip_start="+"inet6_aton('"+ip_start+"')\n");

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
    @Path("/delete/{ip_start}")
    @Produces("text/plain")
    public Response deleteGeoIP(@PathParam("ip_start") String ip_start){

        StringBuilder query = new StringBuilder();

        query.append("DELETE FROM geo_ip \n");
        query.append("    WHERE ip_start="+"inet6_aton('"+ip_start+"')\n");

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

        query.append("DESCRIBE geo_ip");

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

        String statement;
//        = "DELETE FROM geo_ip";
//        try {
//            System.out.println("Deleted: " + DbUtils.update_Db(mysql_ds, statement.toString()));
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return RestResponse.okWithBody(e.getMessage());
//        }

        String[] tempArray = indexes.split(",");

        for (int i = 0; i < tempArray.length; i++) {
            int value = Integer.valueOf(tempArray[i]);
            if (value > -1) {
                indexArray.add(value);
                fieldArray.add(fields.split(",")[i]);
                typeArray.add(types.split(",")[i]);
            }
        }

        String columns = fieldArray.toString().substring(1, fieldArray.toString().length() - 1)+",addr_type";

        String addrType = "ipv4";

        String line;
        statement = "INSERT IGNORE INTO geo_ip (" + columns + ") VALUES ";
        try {
            while ((line = reader.readLine()) != null) {
                if (pointer % INSERT_THRESHOLD == 0) {
                    statement = statement.substring(0, statement.length() - 1);
                    System.out.println("QUERY: \n" + statement.toString() + "\n");
                    affectedRows += DbUtils.update_Db(mysql_ds, statement.toString());
                    statement = "INSERT IGNORE INTO geo_ip (" + columns + ") VALUES ";
                }
                statement += "(";
                String[] values = line.split(delimiter);
                for (int i = 0; i < indexArray.size(); i++) {
                    if (typeArray.get(i).contains("char"))
                        statement += "\"" + values[indexArray.get(i)].replace('\"', '\'') + "\",";
                    else if(fieldArray.get(i).startsWith("ip")){
                        statement += "inet6_aton(\"" + values[indexArray.get(i)] + "\"),";
                        if (values[indexArray.get(i)].indexOf(':') >= 0)
                            addrType = "ipv6";
                        else
                            addrType = "ipv4";
                    }
                    else
                        statement += values[indexArray.get(i)] + ",";
                }
                statement += "\""+addrType+"\"),";
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

        return RestResponse.okWithBody("Success! Rows inserted: "+
                Integer.toString(affectedRows));
    }

}
