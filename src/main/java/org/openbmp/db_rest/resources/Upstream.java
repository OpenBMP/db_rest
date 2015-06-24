/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

@Path("/upstream")
public class Upstream {
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
	@Path("/{ASN}")
	@Produces("application/json")
	public Response getUpstream(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();
		
		StringBuilder query = new StringBuilder();

		query.append("SELECT upstreamASN.asn,as_name,city,state_prov,country,org_name\n");
		query.append("    FROM (select distinct asn_left as asn\n");
		query.append("         from as_path_analysis where asn = " + asn +  " and asn_left != 0) upstreamASN\n");
		query.append("    LEFT JOIN gen_whois_asn w ON (upstreamASN.asn = w.asn)\n");
		query.append("    GROUP BY upstreamASN.asn ORDER BY upstreamASN.asn\n");

		System.out.println("QUERY: \n" + query.toString() + "\n");

		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/{ASN}/count")
	@Produces("application/json")
	public Response getUpstreamCount(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();

		StringBuilder query = new StringBuilder();

		query.append("SELECT upstreamASN.asn,count(distinct prefix_bin,prefix_len) as Prefixes_Learned,\n");
		query.append("               as_name,city,state_prov,country,org_name\n");
		query.append("    FROM (select distinct asn_left as asn,path_attr_hash_id\n");
		query.append("         from as_path_analysis where asn = " + asn +  " and asn_left != 0) upstreamASN\n");
		query.append("    JOIN rib on (upstreamASN.path_attr_hash_id = rib.path_attr_hash_id)\n");
		query.append("    LEFT JOIN gen_whois_asn w ON (upstreamASN.asn = w.asn)\n");
		query.append("    GROUP BY upstreamASN.asn ORDER BY upstreamASN.asn\n");

		System.out.println("QUERY: \n" + query.toString() + "\n");

		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/{ASN}/peer/count")
	@Produces("application/json")
	public Response getUpstreamPeerCount(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();

		StringBuilder query = new StringBuilder();

		query.append("SELECT upstreamASN.asn,count(distinct prefix_bin,prefix_len) as Prefixes_Learned,\n");
		query.append("               p.peer_addr as PeerAddr,p.hash_id as peer_hash_id,\n");
		query.append("               as_name,city,state_prov,country,org_name\n");
		query.append("    FROM (select distinct asn_left as asn,path_attr_hash_id\n");
		query.append("         from as_path_analysis where asn = " + asn +  " and asn_left != 0) upstreamASN\n");
		query.append("    JOIN rib on (upstreamASN.path_attr_hash_id = rib.path_attr_hash_id)\n");
		query.append("    JOIN bgp_peers p ON (rib.peer_hash_id = p.hash_id)\n");
		query.append("    LEFT JOIN gen_whois_asn w ON (upstreamASN.asn = w.asn)\n");
		query.append("    GROUP BY upstreamASN.asn,peer_hash_id ORDER BY upstreamASN.asn\n");


		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
}
