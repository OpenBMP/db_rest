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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

@Path("/downstream")
public class Downstream {
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
	public Response getDownstream(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();
		
		StringBuilder query = new StringBuilder();

		query.append("SELECT downstreamASN.asn,as_name,city,state_prov,country,org_name\n");
		query.append("    FROM (select distinct asn_right as asn\n");
		query.append("         from as_path_analysis where asn = " + asn +  " and asn_right != 0) downstreamASN\n");
		query.append("    LEFT JOIN gen_whois_asn w ON (downstreamASN.asn = w.asn)\n");
		query.append("    GROUP BY downstreamASN.asn ORDER BY downstreamASN.asn\n");

		System.out.println("QUERY: \n" + query.toString() + "\n");

		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/{ASN}/count")
	@Produces("application/json")
	public Response getDownstreamCount(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();

        StringBuilder query = new StringBuilder();

        query.append("SELECT downstreamASN.asn,count(distinct prefix_bin,prefix_len) as Prefixes_Learned,\n");
        query.append("               as_name,city,state_prov,country,org_name\n");
        query.append("    FROM (select distinct asn_right as asn,path_attr_hash_id\n");
        query.append("         from as_path_analysis where asn = " + asn +  " and asn_right != 0) downstreamASN\n");
        query.append("    JOIN rib on (downstreamASN.path_attr_hash_id = rib.path_attr_hash_id)\n");
        query.append("    LEFT JOIN gen_whois_asn w ON (downstreamASN.asn = w.asn)\n");
        query.append("    GROUP BY downstreamASN.asn ORDER BY downstreamASN.asn\n");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/{ASN}/peer/count")
	@Produces("application/json")
	public Response getDownstreamPeerCount(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();

        StringBuilder query = new StringBuilder();

        query.append("SELECT downstreamASN.asn,count(distinct prefix_bin,prefix_len) as Prefixes_Learned,\n");
        query.append("               p.peer_addr as PeerAddr,p.hash_id as peer_hash_id,\n");
        query.append("               as_name,city,state_prov,country,org_name\n");
        query.append("    FROM (select distinct asn_right as asn,path_attr_hash_id\n");
        query.append("         from as_path_analysis where asn = " + asn +  " and asn_right != 0) downstreamASN\n");
        query.append("    JOIN rib on (downstreamASN.path_attr_hash_id = rib.path_attr_hash_id)\n");
        query.append("    JOIN bgp_peers p ON (rib.peer_hash_id = p.hash_id)\n");
        query.append("    LEFT JOIN gen_whois_asn w ON (downstreamASN.asn = w.asn)\n");
        query.append("    GROUP BY downstreamASN.asn,peer_hash_id ORDER BY downstreamASN.asn\n");


        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

	@GET
	@Path("/peer/{peerHashId}")
	@Produces("application/json")
	public Response getDownstreamPeer(@PathParam("peerHashId") String peerHashId,
			 					  @QueryParam("limit") Integer limit) {
		if (peerHashId == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required paramter of peerHashId\" }").build();

		StringBuilder query = new StringBuilder();
		query.append("SELECT downstreamASN.asn,as_name,city,state_prov,country,org_name,p.peer_as as PeerASN\n");
		query.append("FROM (select distinct asn_right as asn,peer_hash_id\n");
		query.append("		from as_path_analysis a join bgp_peers p ON (a.peer_hash_id = p.hash_id and a.asn = p.peer_as)\n");
        query.append("             where a.peer_hash_id = '"+peerHashId + "' and asn_right != 0) downstreamASN\n");
		query.append("JOIN bgp_peers p ON (downstreamASN.peer_hash_id = p.hash_id)\n");
		query.append("LEFT JOIN gen_whois_asn w ON (downstreamASN.asn = w.asn)\n");
		query.append("GROUP BY downstreamASN.asn ORDER BY downstreamASN.asn\n");

	    if (limit != null)
	    	query.append(" LIMIT " + limit);
        else
            query.append(" LIMIT 1000");

		System.out.println("QUERY: \n" + query.toString() + "\n");

		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
}
