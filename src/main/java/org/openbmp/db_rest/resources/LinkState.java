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


	private String buildQuery_v_ls_nodes(String where, String orderBy, Integer limit, Boolean withGeo) {
		StringBuilder query = new StringBuilder();

		if (withGeo == null) {
			query.append("SELECT * FROM v_ls_nodes\n");

		} else {
			query.append("SELECT v_ls_nodes.*,v_geo_ip.* \n");
			query.append("    FROM v_ls_nodes LEFT JOIN v_geo_ip ON (v_geo_ip.ip_start_bin = inet6_aton(v_ls_nodes.RouterId))\n");
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
	@Path("/nodes")
	@Produces("application/json")
	public Response getLsNodes(
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby,
							 @QueryParam("withGeo") Boolean withGeo) {

		String query = buildQuery_v_ls_nodes(where, orderby, limit, withGeo);

		return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query));
	}
	
	@GET
	@Path("/nodes/peer/{peerHashId}")
	@Produces("application/json")
	public Response getLsNodesByPeer(@PathParam("peerHashId") String peerHashId,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby,
							 @QueryParam("withGeo") Boolean withGeo) {
		
		if (where != null && peerHashId != null) {
			where = "peer_hash_id = '" + peerHashId + "' AND " + where;
		}
		else if (peerHashId != null)
			where = "peer_hash_id = '" + peerHashId + "'";

		String query = buildQuery_v_ls_nodes(where, orderby, limit, withGeo);

		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query));
	}

    @GET
    @Path("/nodes/peer/{peerHashId}/{mt_id}")
    @Produces("application/json")
    public Response getLsNodesByPeer(@PathParam("peerHashId") String peerHashId,
                                     @PathParam("mt_id") Long mt_id,
                                     @QueryParam("limit") Integer limit,
                                     @QueryParam("where") String where,
                                     @QueryParam("orderby") String orderby,
                                     @QueryParam("withGeo") Boolean withGeo) {

        if (where != null && peerHashId != null) {
            where = "peer_hash_id = '" + peerHashId + "' AND mt_id = " + mt_id + " AND " + where;
        }
        else if (peerHashId != null)
            where = "peer_hash_id = '" + peerHashId + "' AND mt_id = " + mt_id;


        String query = buildQuery_v_ls_nodes(where, orderby, limit, withGeo);

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query));
    }

    /*** not used
    @GET
	@Path("/nodes/peer/{peerHashId}/{nodeHashId}")
	@Produces("application/json")
	public Response getLsNodesByPeerAndNode(@PathParam("peerHashId") String peerHashId,
							 @PathParam("nodeHashId") String nodeHashId,
			                 @QueryParam("limit") Integer limit,
						     @QueryParam("where") String where,
						     @QueryParam("orderby") String orderby,
                             @QueryParam("withGeo") Boolean withGeo) {
		
		if (where != null && peerHashId != null) {
			where = "peer_hash_id = '" + peerHashId + "' AND " + where;
		}
		else if (peerHashId != null)
			where = "peer_hash_id = '" + peerHashId + "'";
		
		if (nodeHashId != null) {
			where += " AND hash_id = '" + nodeHashId + "'";
		}

        String query = buildQuery_v_ls_nodes(where, orderby, limit, withGeo);

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query));
	}
***/

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
	@Path("/links/peer/{peerHashId}/{mt_id}")
	@Produces("application/json")
	public Response getLsLinksByPeer(@PathParam("peerHashId") String peerHashId,
									 @PathParam("mt_id") Long mt_id,
									 @QueryParam("limit") Integer limit,
									 @QueryParam("where") String where,
									 @QueryParam("orderby") String orderby) {

	if (where != null && peerHashId != null) {
		where = "peer_hash_id = '" + peerHashId + " AND mt_id = " + mt_id + "' AND " + where;
	}
	else if (peerHashId != null)
	where = "peer_hash_id = '" + peerHashId + "' AND mt_id = " + mt_id;

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

	@GET
	@Path("/prefixes/peer/{peerHashId}/{mt_id}")
	@Produces("application/json")
	public Response getLsPrefixesByPeer(@PathParam("peerHashId") String peerHashId,
										@PathParam("mt_id") Long mt_id,
										@QueryParam("limit") Integer limit,
										@QueryParam("where") String where,
										@QueryParam("orderby") String orderby) {

		if (where != null && peerHashId != null) {
			where = "peer_hash_id = '" + peerHashId + "' AND " + " AND mt_id = " + mt_id + " AND " + where;
		}
		else if (peerHashId != null)
			where = "peer_hash_id = '" + peerHashId + "'" + " AND mt_id = " + mt_id;

		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, "v_ls_prefixes", limit, where, orderby));
	}

    @GET
    @Path("/peers")
    @Produces("application/json")
    public Response getLsPeers(@QueryParam("limit") Integer limit,
                               @QueryParam("orderby") String orderby,
                               @QueryParam("withGeo") Boolean withGeo) {


        StringBuilder query = new StringBuilder();

//			query.append("SELECT r.name as RouterName,r.ip_address as RouterIP,\n");
//			query.append("          p.name as PeerName, p.peer_addr as PeerIP,igp_router_id as IGP_RouterId,\n");
//			query.append("          ls_nodes.name as NodeName,\n");
//			query.append("          if (ls_nodes.protocol like 'OSPF%', igp_router_id, router_id) as RouterId,\n");
//			query.append("          ls_nodes.id, ls_nodes.bgp_ls_id as bgpls_id, ls_nodes.ospf_area_id as OspfAreaId,\n");
//			query.append("          ls_nodes.isis_area_id as ISISAreaId, ls_nodes.protocol, flags, ls_nodes.timestamp,\n");
//			query.append("          ls_nodes.asn,\n");
//			query.append("          path_attrs.as_path as AS_Path,path_attrs.local_pref as LocalPref,\n");
//			query.append("          path_attrs.med as MED,path_attrs.next_hop as NH,\n");
//			query.append("          links.mt_id,\n");
//			query.append("          ls_nodes.hash_id,ls_nodes.path_attr_hash_id,ls_nodes.peer_hash_id,r.hash_id as router_hash_id\n");
//			query.append("      FROM ls_nodes\n");
//			query.append("          LEFT JOIN path_attrs ON (ls_nodes.path_attr_hash_id = path_attrs.hash_id AND ls_nodes.peer_hash_id = path_attrs.peer_hash_id)\n");
//			query.append("          JOIN ls_links links ON (ls_nodes.hash_id = links.local_node_hash_id and links.isWithdrawn = False)\n");
//			query.append("          JOIN bgp_peers p on (p.hash_id = ls_nodes.peer_hash_id)\n");
//			query.append("          JOIN routers r on (p.router_hash_id = r.hash_id)\n");
//			query.append("      WHERE not ls_nodes.igp_router_id regexp '\\..[1-9A-F]00$' AND ls_nodes.igp_router_id not like '%]' and ls_nodes.iswithdrawn = False\n");
//			query.append("      GROUP BY ls_nodes.peer_hash_id,ls_nodes.protocol,links.mt_id;\n");

//            query.append("SELECT RouterName,RouterIP,PeerName,PeerIP,ls_peers.protocol,links.peer_hash_id,ls_peers.router_hash_id,links.mt_id,v_geo_ip.* \n");
//            query.append("     FROM ls_links links JOIN v_ls_nodes ls_peers ON (links.peer_hash_id = ls_peers.peer_hash_id)\n");
//            query.append("           LEFT JOIN v_geo_ip ON (v_geo_ip.ip_start_bin = inet6_aton(ls_peers.PeerIP))\n");
//            query.append("     GROUP BY RouterIP,PeerIP,ls_peers.protocol,mt_id\n");

        query.append("SELECT r.name as RouterName,r.ip_address as RouterIP,\n");
        query.append("          p.name as PeerName, p.peer_addr as PeerIP,ls_nodes.protocol,ls_nodes.peer_hash_id,\n");
        query.append("          r.hash_id,links.mt_id\n");

        if (withGeo != null) {
            query.append("          ,v_geo_ip.*\n");
        }

        query.append("      FROM ls_nodes\n");
        query.append("          LEFT JOIN path_attrs ON (ls_nodes.path_attr_hash_id = path_attrs.hash_id AND ls_nodes.peer_hash_id = path_attrs.peer_hash_id)\n");
        query.append("          JOIN ls_links links ON (ls_nodes.hash_id = links.local_node_hash_id and links.isWithdrawn = False)\n");
        query.append("          JOIN bgp_peers p on (p.hash_id = ls_nodes.peer_hash_id)\n");
        query.append("          JOIN routers r on (p.router_hash_id = r.hash_id)\n");

        if (withGeo != null) {
            query.append("          LEFT JOIN v_geo_ip ON (v_geo_ip.ip_start_bin = inet6_aton(p.peer_addr))\n");
        }

        query.append("      WHERE not ls_nodes.igp_router_id regexp '\\..[1-9A-F]00$' AND ls_nodes.igp_router_id not like '%]' and ls_nodes.iswithdrawn = False\n");
        query.append("      GROUP BY ls_nodes.peer_hash_id,ls_nodes.protocol,links.mt_id;\n");


        if (orderby != null) {
            query.append(" ORDER BY ");
            query.append(orderby);
        }

        if (limit != null) {
            query.append(" LIMIT ");
            query.append(limit);
        }

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, "ls_peers", query.toString()));
    }
	
}
