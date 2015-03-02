/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

@Path("/updates")
public class Updates {
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
	@Path("/top")
	@Produces("application/json")
	public Response getUpdatesTop(@QueryParam("limit") Integer limit, 
			                    @QueryParam("hours") Integer hours,
			                    @QueryParam("ts") String timestamp) {
		
		if (hours == null || hours >= 72 || hours < 1)
			hours = 2;
		
		if (limit == null || limit > 100 || limit < 1)
			limit = 25;
		
		if (timestamp == null || timestamp.length() < 1)
			timestamp = "current_timestamp";
		else
			timestamp = "'" + timestamp + "'";
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT u.count as Count, rib.prefix as Prefix,rib.prefix_len as PrefixLen,\n");
		query.append("         if (length(r.name) > 0, r.name, r.ip_address) as RouterName,p.peer_addr as PeerAddr\n");
		query.append("    FROM\n");
		query.append("         (SELECT count(rib_hash_id) as count, rib_hash_id,path_attr_hash_id,timestamp\n");
		query.append("                FROM path_attr_log path  FORCE INDEX (idx_ts)\n"); 
		query.append("                WHERE path.timestamp >= date_sub(" + timestamp + ", interval " + hours + " hour) and\n");
		query.append("                      path.timestamp <= " + timestamp + "\n");
		query.append("                GROUP BY rib_hash_id\n");
		query.append("                ORDER BY count DESC limit " + limit + "\n");
		query.append("         ) u\n");
		query.append("         JOIN rib ON (u.rib_hash_id = rib.hash_id)\n");
		query.append("         JOIN bgp_peers p ON (rib.peer_hash_id = p.hash_id)\n");
		query.append("         JOIN routers r on (r.hash_id = p.router_hash_id)\n");
		query.append("         ORDER BY u.count DESC\n");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/top/interval/{minutes}")
	@Produces("application/json")
	public Response getUpdatesTopInterval(@PathParam("minutes") Integer minutes,
										  @QueryParam("limit") Integer limit,
										  @QueryParam("ts") String timestamp) {

		if (limit == null || limit > 100 || limit < 1)
			limit = 25;
		
		Integer interval = 5;
		if (minutes >= 1 && minutes <= 300) {
			interval = minutes;
		}
		
		if (timestamp == null || timestamp.length() < 1)
			timestamp = "current_timestamp";
		else
			timestamp = "'" + timestamp + "'";
		
		StringBuilder query = new StringBuilder();
		query.append("select from_unixtime(unix_timestamp(timestamp) - unix_timestamp(timestamp) % " 
								+ (interval * 60) + ") as IntervalTime,\n");
		query.append("               count(rib_hash_id) as Count\n");
		query.append("      FROM path_attr_log\n"); 
		query.append("      WHERE timestamp >= date_sub(" + timestamp + ", interval " + 
		 						(interval * limit) + " minute) and timestamp <= " + timestamp + "\n");
		query.append("      GROUP BY IntervalTime\n");
		query.append("      ORDER BY timestamp desc");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	
}
