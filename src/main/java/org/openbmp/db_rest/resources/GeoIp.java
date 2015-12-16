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
                                 @QueryParam("sort") String sort,
                                 @QueryParam("sortDirection") String sortDirection) {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT * FROM v_geo_ip\n");
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
    public Response getGeoIPCount() {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT COUNT(*) as COUNT FROM v_geo_ip\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @POST
    @Path("/insert")
    @Produces("application/json")
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

        return RestResponse.okWithBody(
                Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));
    }

    @GET
    @Path("/update/{ip_start}/{col}/{value}")
    @Produces("application/json")
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

        return RestResponse.okWithBody(
                Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));
    }

    @GET
    @Path("/delete/{ip_start}")
    @Produces("application/json")
    public Response deleteGeoIP(@PathParam("ip_start") String ip_start){

        StringBuilder query = new StringBuilder();

        query.append("DELETE FROM geo_ip \n");
        query.append("    WHERE ip_start="+"inet6_aton('"+ip_start+"')\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                Integer.toString(DbUtils.update_Db(mysql_ds, query.toString())));

    }

}
