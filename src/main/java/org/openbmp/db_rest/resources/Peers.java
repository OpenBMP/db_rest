/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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

	private String buildQuery_v_peers(String where, String orderBy, Integer limit, Boolean withGeo) {
		StringBuilder query = new StringBuilder();
		
		if (withGeo == null) {
			query.append("SELECT * FROM v_peers\n");
			
		} else {
			query.append("SELECT v_peers.*,v_geo_ip.* \n");
			query.append("    FROM v_peers LEFT JOIN v_geo_ip ON (v_geo_ip.ip_start_bin = v_peers.geo_ip_start)\n");
		}
		
		if (where != null) { 
			query.append(" WHERE ");
			query.append(where);
		}
		
		if (orderBy != null) {
			query.append(" ORDER BY ");
			query.append(orderBy);
		}
		
		if (limit != null) {
			query.append(" LIMIT ");
			query.append(limit);
		}
		
		
		System.out.printf("QUERY:\n%s\n", query.toString());
		
		return query.toString();
	}
	
	@GET
	@Produces("application/json")
	public Response getPeers(@QueryParam("limit") Integer limit,
							 @QueryParam("withgeo") Boolean withGeo,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		String query = buildQuery_v_peers(where, orderby, limit, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));
	}

	@GET
	@Path("/{peerHashId}")
	@Produces("application/json")
	public Response getPeersByPeerHashId(@PathParam("peerHashId") String peerHashId,
									  @QueryParam("limit") Integer limit,
									  @QueryParam("withgeo") Boolean withGeo,
									  @QueryParam("where") String where,
									  @QueryParam("orderby") String orderby) {

		String where_str = " peer_hash_id = '" + peerHashId + "' ";

		if (where != null)
			where_str += " and " + where;

		String query = buildQuery_v_peers(where_str, orderby, limit, withGeo);

		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));
	}

	@GET
	@Path("/localip/{LocalIP}")
	@Produces("application/json")
	public Response getPeersByLocalIp(@PathParam("LocalIP") String localIP,
			                 @QueryParam("limit") Integer limit,
			                 @QueryParam("withgeo") Boolean withGeo,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		String where_str = " LocalIP like '" + localIP + "%' ";
		
		if (where != null)
			where_str += " and " + where;
		
		String query = buildQuery_v_peers(where_str, orderby, limit, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));
	}
	
	@GET
	@Path("/remoteip/{peerIP}")
	@Produces("application/json")
	public Response getPeersByRemoteIp(@PathParam("peerIP") String peerIP,
			                 @QueryParam("limit") Integer limit,
			                 @QueryParam("withgeo") Boolean withGeo,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		String where_str = " PeerIP like '" + peerIP + "%' ";
		
		if (where != null)
			where_str += " and " + where;

		String query = buildQuery_v_peers(where_str, orderby, limit, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));		
	}
	
	@GET
	@Path("/router/{routerIP}")
	@Produces("application/json")
	public Response getPeersByRouterIp(@PathParam("routerIP") String routerIP,
			                 @QueryParam("limit") Integer limit,
			                 @QueryParam("withgeo") Boolean withGeo,
						     @QueryParam("orderby") String orderby) {
		
		String where_str = " RouterName like '" + routerIP + "%' or RouterIP like '" + routerIP + "%' ";

		String query = buildQuery_v_peers(where_str, orderby, limit, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));		
	}

	
	@GET
	@Path("/asn/{peerASN}")
	@Produces("application/json")
	public Response getPeersByIp(@PathParam("peerASN") Integer asn,
			                 @QueryParam("limit") Integer limit,
			                 @QueryParam("withgeo") Boolean withGeo,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		String where_str = " PeerASN = " + asn + " ";
		
		if (where != null)
			where_str += " and " + where;

		String query = buildQuery_v_peers(where_str, orderby, limit, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));		
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
	public Response getPeersTypeV4(@QueryParam("withgeo") Boolean withGeo) {

		String query = buildQuery_v_peers("isPeerIPv4 = 1", null, null, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));		
	}
	
	@GET
	@Path("/router/{routerIP}/type/v4")
	@Produces("application/json")
	public Response getPeersTypeV4ByRouter(@PathParam("routerIP") String routerIP,
											@QueryParam("withgeo") Boolean withGeo) {

		String where_str = "isPeerIPv4 = 1 and ( RouterName like '" + routerIP + "%' or RouterIP like '" + routerIP + "%' )";

		String query = buildQuery_v_peers(where_str, null, null, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));		
	}

	@GET
	@Path("/type/v6")
	@Produces("application/json")
	public Response getPeersTypeV6(@QueryParam("withgeo") Boolean withGeo) {

		String query = buildQuery_v_peers("isPeerIPv4 = 0", null, null, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));		
	}
	
	
	@GET
	@Path("/router/{routerIP}/type/v6")
	@Produces("application/json")
	public Response getPeersTypeV6ByRouter(@PathParam("routerIP") String routerIP,
										   @QueryParam("withgeo") Boolean withGeo) {

		String where_str = "isPeerIPv4 = 0 and ( RouterName like '" + routerIP + "%' or RouterIP like '" + routerIP + "%' )";
		
		String query = buildQuery_v_peers(where_str, null, null, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));		
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
	public Response getPeersStatusDown(@QueryParam("withgeo") Boolean withGeo) {

		String query = buildQuery_v_peers("isUp = 0 or isBMPConnected = 0",  null, null, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));		
	}
	
	@GET
	@Path("/status/up")
	@Produces("application/json")
	public Response getPeersStatusUp(@QueryParam("withgeo") Boolean withGeo) {

		String query = buildQuery_v_peers("isUp = 1 and isBMPConnected = 1",  null, null, withGeo);
		
		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));
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
	@Path("/prefix/{peerHashId}")
	@Produces("application/json")
	public Response getPeersPrefixLastByPeer(@PathParam("peerHashId") String peerHashId,
			                                 @QueryParam("last") Integer limit) {
		
		String tableName = "v_peer_prefix_report_last";
		String orderBy = "RouterName,PeerName";

		if (limit != null) {
			tableName = "v_peer_prefix_report";
			orderBy = null;
		}
				
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, tableName, 
						limit, "peer_hash_id like '" + peerHashId + "'", orderBy));
	}

	@GET
	@Path("/events/down")
	@Produces("application/json")
	public Response getPeerDownEvents(@QueryParam("peerHashId") String peerHashId,
									  @QueryParam("limit") Integer limit,
									  @QueryParam("ts") String timestamp,
									  @QueryParam("id") BigInteger id,
									  @QueryParam("orderBy") String orderBy) {
		String tableName = "peer_down_events";
		StringBuilder whereBuilder = new StringBuilder();

		if (limit == null) {
			limit = 1000;
		}

		List<String> paramList = new ArrayList<String>();

		if (peerHashId != null) {
			paramList.add(" peer_hash_id = '" + peerHashId + "'");
 		}

 		if (timestamp != null) {
 			paramList.add(" timestamp >= " + timestamp);
 		}

		if (id != null) {
			paramList.add(" id >= " + id);
		}

		for (int i = 0; i < paramList.size() - 1; i++) {
			whereBuilder.append(paramList.get(i));
			whereBuilder.append(" AND ");
		}

		if (paramList.size() >= 1) {
			whereBuilder.append(paramList.get(paramList.size() - 1));
			return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, tableName, limit, whereBuilder.toString(), orderBy)
			);
		}
		else {
			return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, tableName, limit, null, orderBy)
			);
		}
	}
	
}
