/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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

@Path("/linkstate")
public class LinkState {
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
	@Path("/nodes")
	@Produces("application/json")
	public Response getLsNodes(
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_ls_nodes", limit, where, orderby));
	}
	
	@GET
	@Path("/nodes/peer/{peerHashId}")
	@Produces("application/json")
	public Response getLsNodesByPeer(@PathParam("peerHashId") String peerHashId,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		if (where != null && peerHashId != null) {
			where = "peer_hash_id = '" + peerHashId + "' AND " + where;
		}
		else if (peerHashId != null)
			where = "peer_hash_id = '" + peerHashId + "'";
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_ls_nodes", limit, where, orderby));
	}

	@GET
	@Path("/nodes/peer/{peerHashId}/{nodeHashId}")
	@Produces("application/json")
	public Response getLsNodesByPeerAndNode(@PathParam("peerHashId") String peerHashId,
							 @PathParam("nodeHashId") String nodeHashId,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		if (where != null && peerHashId != null) {
			where = "peer_hash_id = '" + peerHashId + "' AND " + where;
		}
		else if (peerHashId != null)
			where = "peer_hash_id = '" + peerHashId + "'";
		
		if (nodeHashId != null) {
			where += " AND hash_id = '" + nodeHashId + "'";
		}
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_ls_nodes", limit, where, orderby));
	}


	@GET
	@Path("/links")
	@Produces("application/json")
	public Response getLsLinks(
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_ls_links", limit, where, orderby));
	}

	@GET
	@Path("/links/peer/{peerHashId}")
	@Produces("application/json")
	public Response getLsLinksByPeer(@PathParam("peerHashId") String peerHashId,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		if (where != null && peerHashId != null) {
			where = "peer_hash_id = '" + peerHashId + "' AND " + where;
		}
		else if (peerHashId != null)
			where = "peer_hash_id = '" + peerHashId + "'";
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_ls_links", limit, where, orderby));
	}

	
	@GET
	@Path("/prefixes")
	@Produces("application/json")
	public Response getLsPrefixes(
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_ls_prefixes", limit, where, orderby));
	}
	
	@GET
	@Path("/prefixes/peer/{peerHashId}")
	@Produces("application/json")
	public Response getLsPrefixesByPeer(@PathParam("peerHashId") String peerHashId,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby) {
		
		if (where != null && peerHashId != null) {
			where = "peer_hash_id = '" + peerHashId + "' AND " + where;
		}
		else if (peerHashId != null)
			where = "peer_hash_id = '" + peerHashId + "'";
		
		return RestResponse.okWithBody(
					DbUtils.selectStar_DbToJson(mysql_ds, "v_ls_prefixes", limit, where, orderby));
	}

	
}
