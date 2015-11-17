/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

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

@Path("/linkstate/spf")
public class LinkStateSPF {
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
	@Path("/peer/{peerHashId}/ospf/{routerId}")
	@Produces("application/json")
	public Response getLsOspfIGP(@PathParam("peerHashId") String peerHashId,
							 @PathParam("routerId") String routerId,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		StringBuilder query = new StringBuilder();
		String tableName = "igp_ospf_" + routerId.replace('.',  '_');
		long startTime = System.currentTimeMillis();
		
		// first call stored procedure to generate the IGP/SPF table.
		Connection conn = null;
		CallableStatement cs = null;
		
		try{
			conn = mysql_ds.getConnection();
			cs = conn.prepareCall("{call ls_ospf_spf(?, ?, ?, ?)}");
			cs.setString(1, peerHashId);
			cs.setString(2, routerId);
			cs.setInt(3, 15);
			cs.registerOutParameter(4, Types.INTEGER);
			
			cs.execute();
			
			System.out.println(" SPF iterations = " + cs.getInt(4));
            
			String limit_str = "";
			if (limit != null && limit >= 1 && limit <= 500000)
				limit_str = " limit " + limit;
			
			if (where != null) { 
				where = " AND " + where + "\n";
			} else { 
				where = "";
			}
			
			if (orderby == null) {
				orderby = "ORDER BY igp_ospf.prefix,igp_ospf.prefix_len,igp_ospf.metric";
			} else {
				orderby = "ORDER BY " + orderby;
			}
				

			query.append("select igp_ospf.prefix as prefix,igp_ospf.prefix_len,\n");
			query.append("                ospf_route_type as Type,igp_ospf.metric,n.igp_router_id as src_router_id,\n");
			query.append("                nei.igp_router_id as nei_router_id,\n");
			query.append("                igp_ospf.path_router_ids,igp_ospf.path_hash_ids,\n");
			query.append("                l.neighbor_addr as neighbor_addr,\n");
			query.append("                igp_ospf.peer_hash_id\n");
			query.append("    FROM " + tableName + " igp_ospf JOIN ls_nodes n ON (igp_ospf.src_node_hash_id = n.hash_id)\n");
			query.append("            JOIN ls_nodes nei ON (igp_ospf.nh_node_hash_id = nei.hash_id and nei.peer_hash_id = '" + peerHashId +"')\n");
			query.append("            LEFT JOIN ls_links l ON (igp_ospf.nh_node_hash_id = l.remote_node_hash_id and\n");
			query.append("                           igp_ospf.root_node_hash_id = l.local_node_hash_id and l.peer_hash_id = '" + peerHashId  + "')\n");
			query.append("    WHERE best = TRUE ");
			query.append(where);
			query.append("    GROUP BY igp_ospf.prefix,igp_ospf.prefix_len,l.neighbor_addr\n");
			query.append(orderby);
			query.append(limit_str);
		
			System.out.println("QUERY: \n" + query.toString() + "\n");

		} catch (SQLException e) {
			if (e.getSQLState().equals("45000")) {
				System.out.println("Procedure returned error: " + e.getMessage());
				return Response.status(400).entity(e.getMessage())
						.header("Access-Control-Allow-Origin", "*")
						.header("Access-Control-Allow-Methods", "GET")
						.allow("OPTIONS")
						.build();
			} else {
				e.printStackTrace();
				return Response.status(400).entity("error in query " + e.getErrorCode() + " / " + e.getSQLState())
						.header("Access-Control-Allow-Origin", "*")
						.header("Access-Control-Allow-Methods", "GET")
						.allow("OPTIONS")
						.build();
			}
		} finally {
			try {
				if (cs != null)
					cs.close();
				if (conn != null)
					conn.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
			
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, null, query.toString(), (System.currentTimeMillis() - startTime)));
	}
	
	@GET
	@Path("/peer/{peerHashId}/isis/{routerId}")
	@Produces("application/json")
	public Response getLsIsisIGP(@PathParam("peerHashId") String peerHashId,
							 @PathParam("routerId") String routerId,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		StringBuilder query = new StringBuilder();
		String tableName = "igp_isis_" + routerId.replace('.',  '_');
		long startTime = System.currentTimeMillis();
		
		// first call stored procedure to generate the IGP/SPF table.
		Connection conn = null;
		CallableStatement cs = null;
		try{
			conn = mysql_ds.getConnection();
			cs = conn.prepareCall("{call ls_isis_spf(?, ?, ?, ?)}");
			cs.setString(1, peerHashId);
			cs.setString(2, routerId);
			cs.setInt(3, 15);
			cs.registerOutParameter(4, Types.INTEGER);
			
			cs.execute();
			
			System.out.println(" SPF iterations = " + cs.getInt(3));
            
			String limit_str = "";
			if (limit != null && limit >= 1 && limit <= 500000)
				limit_str = " limit " + limit;
			
			if (where != null) { 
				where = " AND " + where + "\n";
			} else { 
				where = "";
			}
			
			if (orderby == null) {
				orderby = "ORDER BY igp_isis.prefix,igp_isis.prefix_len,igp_isis.metric";
			} else {
				orderby = "ORDER BY " + orderby;
			}
				

			query.append("select igp_isis.prefix as prefix,igp_isis.prefix_len,\n");
			query.append("                isis_type as Type,igp_isis.metric,n.router_id src_router_id,\n");
			query.append("                nei.router_id as nei_router_id,\n");
			query.append("                igp_isis.path_router_ids,igp_isis.path_hash_ids,\n");
			query.append("                l.neighbor_addr as neighbor_addr,\n");
			query.append("                igp_isis.peer_hash_id\n");
			query.append("    FROM " + tableName + " igp_isis JOIN ls_nodes n ON (igp_isis.src_node_hash_id = n.hash_id)\n");
			query.append("            JOIN ls_nodes nei ON (igp_isis.nh_node_hash_id = nei.hash_id and nei.peer_hash_id = '" + peerHashId +"')\n");
			query.append("            LEFT JOIN ls_links l ON (igp_isis.nh_node_hash_id = l.remote_node_hash_id and\n");
			query.append("                           igp_isis.root_node_hash_id = l.local_node_hash_id and l.peer_hash_id = '" + peerHashId  + "')\n");
			query.append("    WHERE best = TRUE ");
			query.append(where);
			query.append("    GROUP BY igp_isis.prefix,igp_isis.prefix_len,nei.router_id\n");
			query.append(orderby);
			query.append(limit_str);
		
			System.out.println("QUERY: \n" + query.toString() + "\n");

		} catch (SQLException e) {
			if (e.getSQLState().equals("45000")) {
				System.out.println("Procedure returned error: " + e.getMessage());
				return Response.status(400).entity(e.getMessage())
						.header("Access-Control-Allow-Origin", "*")
						.header("Access-Control-Allow-Methods", "GET")
						.allow("OPTIONS")
						.build();
			} else {
				e.printStackTrace();
				return Response.status(400).entity("error in query " + e.getErrorCode() + " / " + e.getSQLState())
						.header("Access-Control-Allow-Origin", "*")
						.header("Access-Control-Allow-Methods", "GET")
						.allow("OPTIONS")
						.build();
			}
		} finally {
			try {
				if (cs != null)
					cs.close();
				if (conn != null)
					conn.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
			
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, null, query.toString(), (System.currentTimeMillis() - startTime)));
	}
	
}
