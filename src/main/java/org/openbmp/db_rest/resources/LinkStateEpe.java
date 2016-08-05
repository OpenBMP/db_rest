/*
 * Copyright (c) 2015-2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import org.openbmp.db_rest.DbColumnDef;
import org.openbmp.db_rest.DbUtils;
import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.helpers.IpAddr;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/linkstate/epe")
public class LinkStateEpe {
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
	public Response getLsLinksEpe(
			@QueryParam("limit") Integer limit,
			@QueryParam("where") String where,
			@QueryParam("orderby") String orderby) {

		StringBuilder query = new StringBuilder();

        query.append("SELECT ln.local_router_id as Local_BGPID, ln.local_asn as Local_ASN,\n");
        query.append("       ln.remote_router_id as Remote_BGPID, ln.remote_asn as Remote_ASN,\n");
        query.append("       ln.interface_addr as Local_IP, ln.neighbor_addr as Remote_IP,\n");
        query.append("       ln.peer_node_sid as Peer_Node_SID,if(ln.isWithdrawn, 'Withdrawn', 'Active') as State,\n");
        query.append("       ln.path_attr_hash_id as path_attr_hash_id, ln.peer_hash_id as peer_hash_id\n");
        query.append("   FROM ls_links ln\n");
        query.append("   WHERE protocol = 'EPE'\n");


        if (where != null) {
            query.append(" AND ");
            query.append(where);
        }


        System.out.print("QUERY=" + query.toString());

        return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, "ls_links_epe", query.toString()));
	}

	@GET
	@Path("/lookup/destip/{IP}")
	@Produces("application/json")
	public Response getLookupByDestIp(@PathParam("IP") String ip) {

		StringBuilder query = new StringBuilder();
        String prefix;
        Integer prefix_len;
        String router_name;
        String router_bgp_id;
        String router_label;
        String peer_name;
        String peer_address;
        String peer_labels;
        String peer_node_sid;

		long startTime = System.currentTimeMillis();

        /*
         * Stage 1a: Find the longest matched prefix/len based on IP address (most specific)
         */
        Map<String, String> results;

        results = queryLookupIP(ip);

        if (results != null) {
            prefix = results.get("prefix");
            prefix_len = Integer.valueOf(results.get("prefix_len"));

            /*
		     * Stage 1b: Get the prefix/len originating router bgp_id, labels, and peer address
		     */
            results = queryOriginatingPeer(prefix, prefix_len, true);

            if (results == null) {
                return RestResponse.okWithBody("{ \"error\": \"Cannot find prefix\" }");
            }
            else {
                router_name = results.get("router_name");
                router_bgp_id = results.get("router_bgp_id");
                peer_name = results.get("peer_name");
                peer_address = results.get("peer_address");
                peer_labels = results.get("peer_labels");

                /*
                 * Stage 2: Get the EPE peer node SID for the next-hop on the discovered router
                 */
                results = queryEpePeerSid(router_bgp_id,peer_address);

                if (results == null) {
                    return RestResponse.okWithBody("{ \"error\": \"Cannot find EPE peer node sid\" }");
                }
                else {
                    peer_node_sid = results.get("peer_node_sid");

                    /*
                     * Stage 3: Get the BGP-ID label for the router (ToR) by looking up the BGP-ID of the router
                     */
                    results = queryOriginatingPeer(router_bgp_id, (router_bgp_id.contains(":") ? 128 : 32), true);

                    if (results == null) {
                        return RestResponse.okWithBody("{ \"error\": \"Cannot find router bgp-id prefix\" }");
                    }
                    else {
                        router_label = results.get("peer_labels");


                        StringBuilder json = new StringBuilder();

                        json.append("{");
                        json.append("   \"prefix\": \"" + prefix + "\", ");
                        json.append("   \"prefix_len\": " + prefix_len + ", ");
                        json.append("   \"router_name\": \"" + router_name + "\", ");
                        json.append("   \"router_bgp_id\": \"" + router_bgp_id + "\", ");
                        json.append("   \"router_label\": \"" + router_label + "\", ");
                        json.append("   \"peer_name\": \"" + peer_name + "\", ");
                        json.append("   \"peer_address\": \"" + peer_address + "\", ");
                        json.append("   \"peer_labels\": \"" + peer_labels + "\", ");
                        json.append("   \"peer_node_sid\": \"" + peer_node_sid + "\"");
                        json.append("}");

                        return RestResponse.okWithBody(json.toString());
                    }
                }
            }
		}

        return RestResponse.okWithBody("{ \"error\": \"could not find prefix\" }");
	}

    /**
     * Finds the longest matched prefix by the specific IP address
     *
     * Results will have the following keys:
     *      prefix, prefix_len, prefix_bits
     *
     * @param ip           IP address to find (can be IPv4 or IPv6)
     *
     * @return Null if no matches, otherwise map of resutls
     */
    public Map<String, String> queryLookupIP(String ip) {

        StringBuilder query = new StringBuilder();

        String ip_bits = IpAddr.getIpBits(ip);

        query.append("SELECT prefix,prefix_len,prefix_bits \n");
        query.append("        FROM rib\n");
        query.append("        WHERE isWithdrawn = False AND prefix_bits IN (\n");

        for (int len = ip_bits.length(); len > 0; len--) {
            query.append("'" + ip_bits.substring(0, len) + "'");
            if (len > 1) {
                query.append(',');
            }
        }
        query.append(") ");
        query.append("        ORDER BY prefix_bin desc, prefix_len desc limit 1\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        Map<String, List<DbColumnDef>> ResultsMap;
        ResultsMap = DbUtils.select_DbToMap(mysql_ds, query.toString());

        if (ResultsMap.size() > 0) {
            Map<String, String> results = new HashMap<String, String>();
            results.put("prefix", ResultsMap.entrySet().iterator().next().getValue().get(0).getValue());
            results.put("prefix_len", ResultsMap.entrySet().iterator().next().getValue().get(1).getValue());
            results.put("prefix_bits", ResultsMap.entrySet().iterator().next().getValue().get(2).getValue());

            return results;
        }

        return null;
    }

    /**
     * Finds the best matched peer for prefix/len
     *      Performs lightweight best path selection (eg. shortest as path, lowest med)
     *
     *  TODO: Add Prefix SID
     *
     * Results will have the following keys:
     *      router_bgp_id,peer_address,peer_labels,router_name,peer_name
     *
     * @param prefix                Prefix to find
     * @param prefix_len            Prefix length to find
     * @param eBGPOnly              If true, only eBGP peers will be considered
     *
     * @return Null if no matches, otherwise map of resutls
     */
    public Map<String, String> queryOriginatingPeer(String prefix, Integer prefix_len,
                                                    boolean eBGPOnly) {

        StringBuilder query = new StringBuilder();

        query = new StringBuilder();
        query.append("SELECT p.local_bgp_id as router_bgp_id,p.peer_addr as peer_address,r.labels,\n");
        query.append("       rtr.name,p.name\n");
        query.append("     FROM bgp_peers p JOIN rib r ON (r.peer_hash_id = p.hash_id)\n");
        query.append("          JOIN path_attrs path ON (path.hash_id = r.path_attr_hash_id and path.peer_hash_id = r.peer_hash_id)\n");

        //NOTE: Can use routers bgp id instead of using local_bgp_id
        query.append("          JOIN routers rtr ON (p.router_hash_id = rtr.hash_id)\n");


        query.append("     WHERE prefix = \"" + prefix + "\"");
        query.append("           AND prefix_len = " + prefix_len);
        query.append("           AND isWithdrawn = False\n");               // only active prefixes

        if (eBGPOnly) {
            query.append("           AND p.peer_as != p.local_asn\n");          // only eBGP
            query.append("     ORDER BY path.as_path_count, path.med\n");

        } else {
            query.append("     ORDER BY path.local_pref desc,path.as_path_count, path.med\n");
        }

        query.append("     LIMIT 1\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        Map<String, List<DbColumnDef>> ResultsMap;
        ResultsMap = DbUtils.select_DbToMap(mysql_ds, query.toString());

        if (ResultsMap.size() > 0) {
            Map<String, String> results = new HashMap<String, String>();

            results.put("router_bgp_id", ResultsMap.entrySet().iterator().next().getValue().get(0).getValue());
            results.put("peer_address", ResultsMap.entrySet().iterator().next().getValue().get(1).getValue());
            results.put("peer_labels", ResultsMap.entrySet().iterator().next().getValue().get(2).getValue());
            results.put("router_name", ResultsMap.entrySet().iterator().next().getValue().get(3).getValue());
            results.put("peer_name", ResultsMap.entrySet().iterator().next().getValue().get(4).getValue());

            return results;
        }

        return null;
    }

    /**
     * Finds the EPE peer sid based on local router bgp-id and neighbor address
     *
     * Results will have the following keys:
     *      router_bgp_id,peer_address,peer_labels
     *
     * @param router_bgp_id         Router/local bgp id to find
     * @param peer_address          Peer neighbor address to find (peer ip/next-hop)
     *
     * @return Null if no matches, otherwise map of resutls
     */
    public Map<String, String> queryEpePeerSid(String router_bgp_id, String peer_address) {

        StringBuilder query = new StringBuilder();

        query = new StringBuilder();

        query = new StringBuilder();
        query.append("SELECT ln.peer_node_sid as Peer_Node_SID\n");
        query.append("   FROM ls_links ln\n");
        query.append("   WHERE protocol = 'EPE'\n");

        query.append("         AND local_router_id = \"" + router_bgp_id + "\"\n");
        query.append("         AND neighbor_addr = \"" + peer_address + "\"\n");
        query.append("         AND ln.isWithdrawn = False");
        query.append("   LIMIT 1");

        System.out.print("QUERY=" + query.toString());

        Map<String, List<DbColumnDef>> ResultsMap;
        ResultsMap = DbUtils.select_DbToMap(mysql_ds, query.toString());

        if (ResultsMap.size() > 0) {
            Map<String, String> results = new HashMap<String, String>();

            results.put("peer_node_sid", ResultsMap.entrySet().iterator().next().getValue().get(0).getValue());

            return results;
        }

        return null;
    }
}