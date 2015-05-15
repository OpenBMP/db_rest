/*
 * Copyright (c) 2014-2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openbmp.db_rest.DbColumnDef;
import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

@Path("/whois/asn")
public class WhoisAsn {
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
		query.append("SELECT w.*,isTransit,isOrigin,transit_v4_prefixes,transit_v6_prefixes,origin_v4_prefixes,origin_v6_prefixes\n");
		query.append("    FROM gen_whois_asn w LEFT JOIN \n");
        query.append("         gen_asn_stats s ON (w.asn = s.asn) JOIN\n");
        query.append("            (select max(s.timestamp) as ts,s.asn FROM gen_asn_stats s JOIN gen_whois_asn w\n");
        query.append("                ON (s.asn = w.asn)\n");
        query.append("                WHERE ");
        query.append(where);
        query.append("                GROUP BY s.asn) s_last\n");
        query.append("        ON (s_last.asn = s.asn AND s.timestamp = s_last.ts)\n");
        query.append("    WHERE ");
		query.append(where);
		query.append("    group by w.asn\n");
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
		query.append("    FROM gen_whois_asn w \n");
        query.append("    WHERE ");
		query.append(where);
	
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
		
	@GET
	@Path("/{asn}")
	@Produces("application/json")
	public Response getWhoisAsn(@PathParam("asn") Integer asn,
							  @QueryParam("where") String where) {
	
		String where_str = " gen_whois_asn.asn = " + asn;
		if (where != null)
			where_str += " and " + where;
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT gen_whois_asn.*,isTransit,isOrigin,transit_v4_prefixes,transit_v6_prefixes,origin_v4_prefixes,origin_v6_prefixes\n");
		query.append("    FROM gen_whois_asn gen_whois_asn LEFT JOIN ");
		query.append("               (select * FROM gen_asn_stats WHERE asn = " + asn + " order by timestamp desc limit 1) s\n");
		query.append("               ON (gen_whois_asn.asn = s.asn)\n");
		query.append("    WHERE ");
		query.append(where_str);
		query.append("    group by gen_whois_asn.asn\n");
	
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
		
}
