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

@Path("/peer")
public class Peers {
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
	public Response getPeers(@QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_peers", limit, where, orderby));
	}

	@GET
	@Path("/localip/{LocalIP}")
	@Produces("application/json")
	public Response getPeersByLocalIp(@PathParam("LocalIP") String localIP,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		String where_str = " LocalIP like '" + localIP + "%' ";
		
		if (where != null)
			where_str += " and " + where;
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_peers", limit, where_str, orderby));
	}
	
	@GET
	@Path("/remoteip/{peerIP}")
	@Produces("application/json")
	public Response getPeersByRemoteIp(@PathParam("peerIP") String peerIP,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		String where_str = " PeerIP like '" + peerIP + "%' ";
		
		if (where != null)
			where_str += " and " + where;
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_peers", limit, where_str, orderby));
	}
	
	@GET
	@Path("/router/{routerIP}")
	@Produces("application/json")
	public Response getPeersByRouterIp(@PathParam("routerIP") String routerIP,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("orderby") String orderby) {
		
		String where_str = " RouterName like '" + routerIP + "%' or RouterIP like '" + routerIP + "%' ";
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_peers", limit, where_str, orderby));
	}

	
	@GET
	@Path("/asn/{peerASN}")
	@Produces("application/json")
	public Response getPeersByIp(@PathParam("peerASN") Integer asn,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		String where_str = " PeerASN = " + asn + " ";
		
		if (where != null)
			where_str += " and " + where;
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_peers", limit, where_str, orderby));
	}
	
	@GET
	@Path("/type/count")
	@Produces("application/json")
	public Response getPeersTypeCount() {


		StringBuilder query = new StringBuilder();
		query.append("SELECT if(isL3VPNpeer, 'VPN', if(isIpv4, 'IPv4', 'IPv6')) as `IP-Type`,\n");
		query.append("           count(hash_id) as Count\n");
		query.append("    FROM bgp_peers\n");
		query.append("    GROUP BY `IP-Type`");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

	@GET
	@Path("/type/count/router")
	@Produces("application/json")
	public Response getRouterPeersTypeCount() {


		StringBuilder query = new StringBuilder();
		query.append("SELECT RouterName,RouterIP,if(isPeerVPN, 'VPN', if(isPeerIPv4, 'IPv4', 'IPv6')) as `IP-Type`,\n");
		query.append("           count(peer_hash_id) as Count\n");
		query.append("    FROM v_peers\n");
		query.append("    GROUP BY RouterName,`IP-Type`");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/type/v4")
	@Produces("application/json")
	public Response getPeersTypeV4() {

		StringBuilder query = new StringBuilder();
		query.append("SELECT * \n");
		query.append("    FROM v_peers\n");
		query.append("    WHERE isPeerIPv4 = 1");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

	@GET
	@Path("/type/v6")
	@Produces("application/json")
	public Response getPeersTypeV6() {


		StringBuilder query = new StringBuilder();
		query.append("SELECT * \n");
		query.append("    FROM v_peers\n");
		query.append("    WHERE isPeerIPv4 = 0");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/status/count")
	@Produces("application/json")
	public Response getPeersStatusCount() {


		StringBuilder query = new StringBuilder();
		query.append("SELECT if(isBMPConnected, if(isUp, 'UP', 'DOWN'), if (isUp, 'UP-BMPDown', 'DOWN-BMPDown')) as StatusType,\n");
		query.append("           count(PeerName) as Count\n");
		query.append("    FROM v_peers\n");
		query.append("    GROUP BY StatusType");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	
	@GET
	@Path("/status/down")
	@Produces("application/json")
	public Response getPeersStatusDown() {

		StringBuilder query = new StringBuilder();
		query.append("SELECT * \n");
		query.append("    FROM v_peers\n");
		query.append("    WHERE isUp = 0 or isBMPConnected = 0");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/status/up")
	@Produces("application/json")
	public Response getPeersStatusUp() {

		StringBuilder query = new StringBuilder();
		query.append("SELECT * \n");
		query.append("    FROM v_peers\n");
		query.append("    WHERE isUp = 1");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

	@GET
	@Path("/prefix")
	@Produces("application/json")
	public Response getPeersPrefixLast(@QueryParam("last") Integer limit) {
		
		String tableName = "v_peer_prefix_report_last";
		String orderBy = "RouterName,PeerName";

		if (limit != null) {
			tableName = "v_peer_prefix_report";
			orderBy = null;
		}
		
		
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, tableName, 
						limit, null, orderBy));
	}

	@GET
	@Path("/prefix/{peerIP}")
	@Produces("application/json")
	public Response getPeersPrefixLastByPeer(@PathParam("peerIP") String peerIP,
			                                 @QueryParam("last") Integer limit) {
		
		String tableName = "v_peer_prefix_report_last";
		String orderBy = "RouterName,PeerName";

		if (limit != null) {
			tableName = "v_peer_prefix_report";
			orderBy = null;
		}
				
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, tableName, 
						limit, "PeerName like '" + peerIP + "%'", orderBy));
	}
	
}
