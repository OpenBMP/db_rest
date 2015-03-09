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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

@Path("/routers")
public class Routers {
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
	public Response getRouters(@QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		
		StringBuilder query = new StringBuilder();
		query.append("select name as RouterName, ip_address as RouterIP, router_AS as RouterAS, description,\n");
		query.append("           isConnected, isPassive, term_reason_code as LastTermCode,\n");
		query.append("           term_reason_text as LastTermReason, init_data as InitData\n");
		query.append("    FROM routers\n");
		
		String limit_st = " limit 1000";
		String where_st = "";
		String orderby_st = "";
		
		// Set the limit for the query 
		if (limit != null && limit < 40000)
			limit_st = " limit " + limit;
		
		if (where != null)
			where_st = " WHERE " + where;
		
		if (orderby != null)
			orderby_st = " ORDER BY " + orderby;

		query.append(where_st);
		query.append(orderby_st);
		query.append(limit_st);
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

	@GET
	@Path("/status/count")
	@Produces("application/json")
	public Response getRoutersStatusCount() {
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT if(isConnected, 'UP', 'DOWN') as StatusType,\n");
		query.append("           count(hash_id) as Count\n");
		query.append("    FROM routers GROUP BY StatusType\n");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

	@GET
	@Path("/status/up")
	@Produces("application/json")
	public Response getRoutersStatusUp(@QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		StringBuilder query = new StringBuilder();
		query.append("select name as RouterName, ip_address as RouterIP, router_AS as RouterAS, description,\n");
		query.append("           isConnected, isPassive, term_reason_code as LastTermCode,\n");
		query.append("           term_reason_text as LastTermReason, init_data as InitData\n");
		query.append("    FROM routers\n");
		
		String limit_st = " limit 1000";
		String where_st = " WHERE isConnected = 1";
		String orderby_st = "";
		
		// Set the limit for the query 
		if (limit != null && limit < 40000)
			limit_st = " limit " + limit;
		
		if (where != null)
			where_st += where;
		
		if (orderby != null)
			orderby_st = " ORDER BY " + orderby;

		query.append(where_st);
		query.append(orderby_st);
		query.append(limit_st);
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}


	@GET
	@Path("/status/down")
	@Produces("application/json")
	public Response getRoutersStatusDown(@QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		StringBuilder query = new StringBuilder();
		query.append("select name as RouterName, ip_address as RouterIP, router_AS as RouterAS, description,\n");
		query.append("           isConnected, isPassive, term_reason_code as LastTermCode,\n");
		query.append("           term_reason_text as LastTermReason, init_data as InitData\n");
		query.append("    FROM routers\n");
		
		String limit_st = " limit 1000";
		String where_st = " WHERE isConnected = 0";
		String orderby_st = "";
		
		// Set the limit for the query 
		if (limit != null && limit < 40000)
			limit_st = " limit " + limit;
		
		if (where != null)
			where_st += where;
		
		if (orderby != null)
			orderby_st = " ORDER BY " + orderby;

		query.append(where_st);
		query.append(orderby_st);
		query.append(limit_st);
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

}
