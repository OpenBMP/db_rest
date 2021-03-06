/*
 * Copyright (c) 2014-2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

@Path("/rib")
public class Rib {
    @Context
    ServletContext ctx;
    @Context
    UriInfo uri;

    private DataSource mysql_ds;

    private static enum HISTORY_TYPE { UPDATES, WITHDRAWS };

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
    public Response getRib(@QueryParam("limit") Integer limit,
                           @QueryParam("where") String where,
                           @QueryParam("orderby") String orderby) {

        return RestResponse.okWithBody(
                DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit, where, orderby));
    }

    @GET
    @Path("/peer/{peerHashId}")
    @Produces("application/json")
    public Response getRibByPeer(@PathParam("peerHashId") String peerHashId,
                                 @QueryParam("limit") Integer limit,
                                 @QueryParam("where") String where,
                                 @QueryParam("orderby") String orderby) {

        String where_str = "peer_hash_id = '" + peerHashId + "'";

        if (where != null)
            where_str += " and " + where;

        return RestResponse.okWithBody(
                DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit, where_str, orderby));
    }

    @GET
    @Path("/router/{routerHashId}")
    @Produces("application/json")
    public Response getRibByRouter(@PathParam("routerHashId") String routerHashId,
                                   @QueryParam("limit") Integer limit,
                                   @QueryParam("where") String where,
                                   @QueryParam("orderby") String orderby) {

        String where_str = "router_hash_id = '" + routerHashId + "'";

        if (where != null)
            where_str += " and " + where;

        return RestResponse.okWithBody(
                DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit, where_str, orderby));
    }

    @GET
    @Path("/asn/{ASN}")
    @Produces("application/json")
    public Response getRibAsn(@PathParam("ASN") Integer asn,
                              @QueryParam("limit") Integer limit,
                              @QueryParam("where") String where,
                              @QueryParam("orderby") String orderby,
                              @QueryParam("brief") Boolean brief,
                              @QueryParam("distinct") Boolean distinct) {


        StringBuilder query = new StringBuilder();

        query.append("    SELECT  if (length(rtr.name) > 0, rtr.name,rtr.ip_address) AS RouterName,\n");
        query.append("    if(length(p.name) > 0, p.name, p.peer_addr) AS PeerName,\n");
        query.append("    r.prefix AS Prefix,r.prefix_len AS PrefixLen,r.isIPv4 as isIPv4,\n");

        if (brief == null) {
            query.append("    path.origin AS Origin,r.origin_as AS Origin_AS,path.med AS MED,\n");
            query.append("    		path.local_pref AS LocalPref,path.next_hop AS NH,path.as_path AS AS_Path,\n");
            query.append("    path.as_path_count AS ASPath_Count,path.community_list AS Communities,\n");
            query.append("    path.ext_community_list AS ExtCommunities,\n");
            query.append("    		path.cluster_list AS ClusterList,path.aggregator AS Aggregator,p.peer_addr AS PeerAddress, p.peer_as AS PeerASN,\n");
            query.append("    p.isIPv4 as isPeerIPv4, p.isL3VPNpeer as isPeerVPN,\n");
            query.append("    r.hash_id as rib_hash_id,\n");
            query.append("    r.path_attr_hash_id as path_hash_id, r.peer_hash_id, rtr.hash_id as router_hash_id,r.isWithdrawn,\n");
            query.append("    r.timestamp AS LastModified, r.first_added_timestamp as DBLastModified,r.prefix_bin as prefix_bin,\n");
        }

        query.append("    r.hash_id as rib_hash_id,\n");
        query.append("    r.path_attr_hash_id as path_hash_id, r.peer_hash_id, rtr.hash_id as router_hash_id\n");
        query.append("    FROM bgp_peers p JOIN rib r force index (idx_origin_as) ON (r.peer_hash_id = p.hash_id)\n");

        if (brief == null)
            query.append("    JOIN path_attrs path ON (path.hash_id = r.path_attr_hash_id and path.peer_hash_id = r.peer_hash_id)\n");

        query.append("    JOIN routers rtr ON (p.router_hash_id = rtr.hash_id)\n");
        query.append("    WHERE r.isWithdrawn = False and r.origin_as = " + asn + "\n");

        if (where != null)
            query.append(" AND " + where);

        if (orderby != null)
            query.append(" ORDER BY " + orderby);

        if (distinct != null)
            query.append("    GROUP BY prefix_bin,prefixlen");

        if (limit != null)
            query.append(" LIMIT " + limit);
        else
            query.append(" LIMIT 1000");

        System.out.println("QUERY= " + query.toString());
        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, "v_routes", query.toString()));
    }

    @GET
    @Path("/peer/{peerHashId}/asn/{ASN}")
    @Produces("application/json")
    public Response getRibAsnByPeer(@PathParam("ASN") Integer asn,
                                    @PathParam("peerHashId") String peerHashId,
                                    @QueryParam("limit") Integer limit,
                                    @QueryParam("where") String where,
                                    @QueryParam("brief") Boolean brief,
                                    @QueryParam("distinct") Boolean distinct,
                                    @QueryParam("orderby") String orderby) {

        StringBuilder query = new StringBuilder();

        query.append("    SELECT  if (length(rtr.name) > 0, rtr.name,rtr.ip_address) AS RouterName,\n");
        query.append("    if(length(p.name) > 0, p.name, p.peer_addr) AS PeerName,\n");
        query.append("    r.prefix AS Prefix,r.prefix_len AS PrefixLen,r.isIPv4 as isIPv4,\n");

        if (brief == null) {
            query.append("    path.origin AS Origin,r.origin_as AS Origin_AS,path.med AS MED,\n");
            query.append("    		path.local_pref AS LocalPref,path.next_hop AS NH,path.as_path AS AS_Path,\n");
            query.append("    path.as_path_count AS ASPath_Count,path.community_list AS Communities,\n");
            query.append("    path.ext_community_list AS ExtCommunities,\n");
            query.append("    		path.cluster_list AS ClusterList,path.aggregator AS Aggregator,p.peer_addr AS PeerAddress, p.peer_as AS PeerASN,\n");
            query.append("    p.isIPv4 as isPeerIPv4, p.isL3VPNpeer as isPeerVPN,\n");
            query.append("    r.hash_id as rib_hash_id,\n");
            query.append("    r.path_attr_hash_id as path_hash_id, r.peer_hash_id, rtr.hash_id as router_hash_id,r.isWithdrawn,\n");
            query.append("    r.timestamp AS LastModified, r.first_added_timestamp as DBLastModified,r.prefix_bin as prefix_bin,\n");
        }

        query.append("    r.hash_id as rib_hash_id,\n");
        query.append("    r.path_attr_hash_id as path_hash_id, r.peer_hash_id, rtr.hash_id as router_hash_id\n");
        query.append("    FROM bgp_peers p JOIN rib r force index (idx_origin_as)ON (r.peer_hash_id = p.hash_id)\n");
        query.append("    JOIN path_attrs path ON (path.hash_id = r.path_attr_hash_id and path.peer_hash_id = r.peer_hash_id)\n");
        query.append("    JOIN routers rtr ON (p.router_hash_id = rtr.hash_id)\n");
        query.append("    WHERE r.isWithdrawn = False and r.origin_as = " + asn + " and r.peer_hash_id = '" + peerHashId + "'\n");

        if (where != null)
            query.append(" AND " + where);

        if (orderby != null)
            query.append(" ORDER BY " + orderby);

        if (distinct != null)
            query.append("    GROUP BY prefix_bin,prefixlen");

        if (limit != null)
            query.append(" LIMIT " + limit);
        else
            query.append(" LIMIT 1000");

        System.out.println("QUERY= " + query.toString());
        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, "v_routes", query.toString()));
    }

    @GET
    @Path("/asn/{ASN}/count")
    @Produces("application/json")
    public Response getRibOriginASCount(@PathParam("ASN") Integer asn) {
        if (asn == null)
            return Response.status(400).entity(
                    "{ \"error\": \"Missing required path parameter\" }").build();

        StringBuilder query = new StringBuilder();
        query.append("select count(distinct prefix,prefix_len) as PrefixCount from rib where origin_as = " + asn + " and isWithdrawn = False");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @GET
    @Path("peer/{peerHashId}/asn/{ASN}/count")
    @Produces("application/json")
    public Response getRibOriginASCountByPeer(@PathParam("ASN") Integer asn,
                                              @PathParam("peerHashId") String peerHashId) {
        if (asn == null)
            return Response.status(400).entity(
                    "{ \"error\": \"Missing required path parameter\" }").build();

        StringBuilder query = new StringBuilder();
        query.append("select count(Prefix) as PrefixCount FROM v_routes where Origin_AS = " + asn);
        query.append(" and peer_hash_id = '" + peerHashId + "'");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }

    @GET
    @Path("/prefix/suggestion/{prefix}/{prefix_len}")
    @Produces("application/json")
    public Response getRibPrefixSuggestions(@PathParam("prefix") String prefix,
                                            @PathParam("prefix_len") Integer prefix_len,
                                            @QueryParam("limit") Integer limit) {

        StringBuilder query = new StringBuilder();

        query.append("SELECT DISTINCT CONCAT(prefix,'/',prefix_len) as complete_prefix FROM rib ");

        query.append(" WHERE prefix LIKE '" + prefix + "%' ");

        if(prefix_len != -1)
            query.append(" AND prefix_len = " + prefix_len);
        else
            query.append(" ORDER BY prefix_len ");

        // Limit the number of results.
        if (limit != null)
            query.append(" LIMIT " + limit);
        else
            query.append(" LIMIT 10");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, "v_all_routes", query.toString()));
    }


    @GET
    @Path("/prefix/{prefix}")
    @Produces("application/json")
    public Response getRibByPrefix(@PathParam("prefix") String prefix,
                                   @QueryParam("limit") Integer limit,
                                   @QueryParam("aggregates") String aggregate,
                                   @QueryParam("distinc") String distinct,
                                   @QueryParam("where") String where,
                                   @QueryParam("orderby") String orderby) {

        StringBuilder query = new StringBuilder();

        if (aggregate != null) {
            if (prefix.contains(".")) {
                query.append(" isIPv4 = 1 and ");
            } else {
                query.append(" isIPv4 = 0 and ");
            }

            String ip_bits = IpAddr.getIpBits(prefix);

            query.append(" prefix_bits IN (");
            for (int len = ip_bits.length(); len > 0; len--) {
                query.append("'" + ip_bits.substring(0, len) + "'");

                if (len > 1) {
                    query.append(',');
                }
            }

            query.append(") ");
        } else {
            query.append("Prefix like '" + prefix + "%' ");
        }

        if (distinct != null) {
            query.append(" GROUP BY prefix, prefixlen");
        }

        if (where != null)
            query.append(" and " + where);

        return RestResponse.okWithBody(
                DbUtils.selectStar_DbToJson(mysql_ds, "v_all_routes", limit, query.toString(), orderby));
    }

    @GET
    @Path("/peer/{peerHashId}/prefix/{prefix}")
    @Produces("application/json")
    public Response getRibByPrefixByPeer(@PathParam("prefix") String prefix,
                                         @PathParam("peerHashId") String peerHashId,
                                         @QueryParam("limit") Integer limit,
                                         @QueryParam("where") String where,
                                         @QueryParam("orderby") String orderby) {

        String where_str = "peer_hash_id = '" + peerHashId + "' and Prefix like '" + prefix + "%' ";

        if (where != null)
            where_str += " and " + where;

        return RestResponse.okWithBody(
                DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit, where_str, orderby));
    }


    @GET
    @Path("/prefix/{prefix}/{length}")
    @Produces("application/json")
    public Response getRibByPrefixLength(@PathParam("prefix") String prefix,
                                         @PathParam("length") Integer length,
                                         @QueryParam("limit") Integer limit,
                                         @QueryParam("aggregates") String aggregate,
                                         @QueryParam("distinct") String distinct,
                                         @QueryParam("specific") String specific,
                                         @QueryParam("where") String where,
                                         @QueryParam("orderby") String orderby) {

        if (length < 1 || length > 128)
            length = 32;

        StringBuilder query = new StringBuilder();

        if (prefix.contains(".")) {
            query.append(" isIPv4 = 1 and ");
        } else {
            query.append(" isIPv4 = 0 and ");
        }

        String ip_bits = IpAddr.getIpBits(prefix);

        if (aggregate != null) {
            query.append(" prefix_bits IN (");
            for (int len = ip_bits.length(); len > 0; len--) {
                query.append("'" + ip_bits.substring(0, len) + "'");

                if (len > 1) {
                    query.append(',');
                }
            }
            query.append(") ");
        } else if (specific != null) {
            query.append(" prefix_bits LIKE '");
            query.append(ip_bits.substring(0, length));
            query.append("%'");
        } else {
            query.append("Prefix = '" + prefix + "' and PrefixLen = " + length);
        }

        if (distinct != null) {
            query.append(" GROUP BY prefix, prefixlen");
        }

        if (where != null)
            query.append(" and " + where);


        return RestResponse.okWithBody(
                DbUtils.selectStar_DbToJson(mysql_ds, "v_all_routes", limit, query.toString(), orderby));
    }

    @GET
    @Path("/peer/{peerHashId}/prefix/{prefix}/{length}")
    @Produces("application/json")
    public Response getRibByPrefixLengthByPeer(@PathParam("prefix") String prefix,
                                               @PathParam("peerHashId") String peerHashId,
                                               @PathParam("length") Integer length,
                                               @QueryParam("limit") Integer limit,
                                               @QueryParam("where") String where,
                                               @QueryParam("orderby") String orderby) {

        if (length < 1 || length > 128)
            length = 32;

        String where_str = "peer_hash_id = '" + peerHashId + "' and Prefix = '" + prefix + "' and PrefixLen = " + length;

        if (where != null)
            where_str += " and " + where;


        return RestResponse.okWithBody(
                DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit, where_str, orderby));
    }

    private PrefixEntry findIP(String ip) {
        PrefixEntry pEntry = new PrefixEntry();

        StringBuilder query = new StringBuilder();

        // Query for the best match based on prefix_bits (range query is too slow)
        String ip_bits = IpAddr.getIpBits(ip);


        query.append("SELECT prefix,prefix_len,prefix_bits,origin_as,if(as_name is null, org_id, as_name) as as_name\n");
        query.append("    FROM rib\n");
        query.append("         LEFT JOIN gen_whois_asn w ON (rib.origin_as = w.asn)\n");
        query.append("    WHERE isWithdrawn = False AND \n");

        if (ip.contains(":")) {
            pEntry.isIPv4 = Boolean.FALSE;
            query.append(" isIPv4 = 0 AND ");
        } else {
            pEntry.isIPv4 = Boolean.TRUE;
            query.append(" isIPv4 = 1 AND ");
        }

        query.append("          prefix_bits IN (\n");

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

        if (ResultsMap.size() <= 0) {
            // No results to return
            return null;

        } else {
            pEntry.prefix = ResultsMap.entrySet().iterator().next().getValue().get(0).getValue();
            pEntry.prefix_len = Integer.valueOf(ResultsMap.entrySet().iterator().next().getValue().get(1).getValue());
            pEntry.prefix_bits = ResultsMap.entrySet().iterator().next().getValue().get(2).getValue();
            pEntry.origin_asn = Long.valueOf(ResultsMap.entrySet().iterator().next().getValue().get(3).getValue());
            pEntry.origin_name = ResultsMap.entrySet().iterator().next().getValue().get(4).getValue();
        }

        return pEntry;
    }

    @GET
    @Path("/as/trace/ip/{SRC_IP}/{DST_IP}")
    @Produces("text/plain")
    public Response getIpAsTrace(@PathParam("SRC_IP") String src_ip,
                                   @PathParam("DST_IP") String dst_ip) {

        long startTime = System.currentTimeMillis();

        PrefixEntry srcPrefix = findIP(src_ip);
        PrefixEntry dstPrefix = findIP(dst_ip);

        if (src_ip.equals(dst_ip)) {
            return RestResponse.okWithBody("graph LR\n" +
                                "    Error(<b>Error:</b> SOURCE and DEST cannot be the same)");
        }

        if (srcPrefix == null || dstPrefix == null) {
            StringBuilder sb = new StringBuilder();

            sb.append("graph LR\n");
            if (srcPrefix == null)
                sb.append("     Error(<b>Error:</b> Source prefix not found!)\n");

            if (dstPrefix == null)
                sb.append("     Error(<b>Error:</b> Destination prefix not found!)\n");

            // No results to return
            return RestResponse.okWithBody(sb.toString());

        } else {
            StringBuilder query = new StringBuilder();

            query.append("SELECT DISTINCT as_path,if(prefix = \"" + srcPrefix.prefix + "\", 'A', 'B') as dir\n");
            query.append("    FROM rib\n");
            query.append("    JOIN path_attrs p ON (rib.path_attr_hash_id = p.hash_id and rib.peer_hash_id = p.peer_hash_id)\n");
            query.append("    WHERE rib.isWithdrawn = False AND \n");

            if (srcPrefix.isIPv4) {
                query.append(" isIPv4 = 1 and ");
            } else {
                query.append(" isIPv4 = 0 and ");
            }

            query.append("( prefix_bits = \"" + srcPrefix.prefix_bits + "\" OR ");
            query.append(" prefix_bits = \"" + dstPrefix.prefix_bits + "\")");

            query.append("  ORDER BY dir");

            System.out.println("QUERY: " + query.toString());

            Map<String, List<DbColumnDef>> ResultsMap;

            ResultsMap = DbUtils.select_DbToMap(mysql_ds, query.toString());

            if (ResultsMap.size() <= 0) {
                // No results to return
                return RestResponse.okWithBody("");

            } else {
                boolean finishedSrc = false;

                /*
                 * Generate a source and dest unique asn list, this will be checked when adding links
                 */
                Set<Long> srcAsns = new TreeSet<>();
                Set<Long> dstAsns = new TreeSet<>();

                for (List<DbColumnDef> row : ResultsMap.values()) {
                    String[] strs = row.get(0).getValue()
                            .trim()
                            .replaceAll("\\{.*\\}", "")
                            .split("\\s+");

                    if (row.get(1).getValue().equals("B"))
                        finishedSrc = true;

                        // add ASN to list
                        for (int i = 0; i < strs.length; i++) {

                            if (!finishedSrc) {
                                srcAsns.add(Long.valueOf(strs[i]));
                            } else {
                                dstAsns.add(Long.valueOf(strs[i]));
                            }
                    }
                }

                /*
                 * Render mermaid (https://mermaidjs.github.io/) result text for simple diagrams
                 */
                StringBuilder result = new StringBuilder();

                result.append("graph LR\n");

                /*
                 * Build unique set of linked objects (remove the duplicates)
                 */
                Set<String> links = new LinkedHashSet<String>();
                finishedSrc = false;

                for (List<DbColumnDef> row: ResultsMap.values()) {
                    boolean pathConnects = false;
                    Set<String> pathLinks = new LinkedHashSet<>();

                    String[] strs = row.get(0).getValue()
                                            .trim()
                                            .replaceAll("\\{.*\\}", "")
                                            .split("\\s+");

                    // Reverse the path if it's the destination
                    if (row.get(1).getValue().equals("B")) {
//                        String[] r_strs = new String[strs.length];
//                        int i2 = 0;
//                        for (int i=strs.length - 1; i >= 0; i--) {
//                            r_strs[i2++] = strs[i];
//                        }
//                        strs = r_strs;

                        finishedSrc = true;
                    }

                    /* Filter by path containing ASN */
//                    if (!row.get(0).getValue().contains("16150"))
//                        continue;

                    // iterate over the path and add linked nodes
                    String A = null;
                    for (int i=0; i < strs.length; i++) {
                        String asn = strs[i];

                        // Path only connects if A & B contain at least one intersecting ASN
                        if (!finishedSrc) {

                            if (i == 0 && ! dstAsns.contains(Long.valueOf(asn))) {
                                System.out.println("Peer ASN doesn't exist in destination, skipping: " + asn);
                                continue;
                            }

                            if (dstAsns.contains(Long.valueOf(asn)))
                                pathConnects = true;

                        } else {

                            if (srcAsns.contains(Long.valueOf(asn)))
                                pathConnects = true;
                        }

                        if (A != null && ! A.equals(asn)) {
                            StringBuilder sb = new StringBuilder();

                            if (!finishedSrc) {
                                sb.append("AS");
                                sb.append(asn);
                                sb.append(" --> AS");
                                sb.append(A);
                                sb.append(";\n");

                                pathLinks.add(sb.toString());
                            }
                            else { // Reverse for destination

                                sb.append("AS");
                                sb.append(A);
                                sb.append(" --> AS");
                                sb.append(asn);
                                sb.append(";\n");

                                StringBuilder sb2 = new StringBuilder();
                                sb2.append("AS");
                                sb2.append(asn);
                                sb2.append(" --> AS");
                                sb2.append(A);
                                sb2.append(";\n");

                                if (links.contains(sb2.toString())) {
                                    System.out.println("Path removing recursive path: " + sb.toString());
                                    links.remove(sb2.toString());

                                } else {
                                    pathLinks.add(sb.toString());
                                }
                            }
                        }

                        A = asn;
                    }

                    if (pathConnects)
                        links.addAll(pathLinks);
                }

                // SRC
                result.append("SRC[");
                result.append(srcPrefix.prefix);
                result.append('/');
                result.append(srcPrefix.prefix_len);
                result.append("] --> \n");

                result.append("AS");
                result.append(srcPrefix.origin_asn);

                if (srcPrefix.origin_name.length() > 0) {
                    result.append("[");
                    result.append("AS");
                    result.append(srcPrefix.origin_asn);
                    result.append(' ');
                    result.append(srcPrefix.origin_name);
                    result.append("];\n");
                }

                // DST
                result.append("AS");
                result.append(dstPrefix.origin_asn);

                if (dstPrefix.origin_name.length() > 0) {
                    result.append("[");
                    result.append("AS");
                    result.append(dstPrefix.origin_asn);
                    result.append(' ');
                    result.append(dstPrefix.origin_name);
                    result.append("] --> \n");
                }

                result.append("DST[");
                result.append(dstPrefix.prefix);
                result.append('/');
                result.append(dstPrefix.prefix_len);
                result.append("];\n");


                // Add unique set of links
                for (String link: links) {
                    result.append(link);
                }


                result.append("style SRC fill:#66ccff,stroke:#e0e0d1,stroke-width:2px,stroke-dasharray: 5, 5\n" +
                              "style DST fill:#66ccff,stroke:#e0e0d1,stroke-width:2px,stroke-dasharray: 5, 5");
                return RestResponse.okWithBody(result.toString());
            }
        }
    }

    @GET
    @Path("/lookup/{IP}")
    @Produces("application/json")
    public Response getRibByLookup(@PathParam("IP") String ip,
                                   @QueryParam("limit") Integer limit,
                                   @QueryParam("where") String where,
                                   @QueryParam("aggregates") Boolean agg,
                                   @QueryParam("distinct") Boolean distinct,
                                   @QueryParam("specific") Boolean specific,
                                   @QueryParam("orderby") String orderby) {

        StringBuilder query = new StringBuilder();

        long startTime = System.currentTimeMillis();

        // Query for the best match based on prefix_bits (range query is too slow)
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
        //ResultsMap = DbUtils.selectPartitions_DbToMap(mysql_ds, query.toString(), 2, 0, 47, null);

        if (ResultsMap.size() <= 0) {
            // No results to return
            return RestResponse.okWithBody("{}");
        } else {
            // Query v_routes for the prefix found
            String prefix = ResultsMap.entrySet().iterator().next().getValue().get(0).getValue();
            String prefix_len = ResultsMap.entrySet().iterator().next().getValue().get(1).getValue();
            String prefix_bits = ResultsMap.entrySet().iterator().next().getValue().get(2).getValue();

            query = new StringBuilder();

            if (ip.contains(".")) {
                query.append(" isIPv4 = 1 and ");
            } else {
                query.append(" isIPv4 = 0 and ");
            }

            if (agg != null) {
                ip_bits = IpAddr.getIpBits(prefix);

                query.append(" prefix_bits IN (");
                for (int len = ip_bits.length(); len > 0; len--) {
                    query.append("'" + ip_bits.substring(0, len) + "'");

                    if (len > 1) {
                        query.append(',');
                    }
                }

                query.append(") ");

            } else {
                query.append(" prefix = \"" + prefix + "\"");
                query.append(" AND prefixlen = " + prefix_len);
            }

            if (where != null) {
                query.append(" and " + where);
            }

            if (distinct != null) {
                query.append(" GROUP BY prefix,prefixlen");
            }

            if (orderby == null && agg != null) {
                orderby = " prefixlen desc";
            }

            return RestResponse.okWithBody(
                    DbUtils.selectStar_DbToJson(mysql_ds, "v_all_routes", limit,
                            query.toString() /* where str without the WHERE statement */, orderby,
                            (System.currentTimeMillis() - startTime)));

           /* return RestResponse.okWithBody(
                    DbUtils.select_DbToJson(mysql_ds, query.toString()));*/

        }
    }

    @GET
    @Path("/peer/{peerHashId}/lookup/{IP}")
    @Produces("application/json")
    public Response getRibByLookupByPeer(@PathParam("IP") String ip,
                                         @PathParam("peerHashId") String peerHashId,
                                         @QueryParam("limit") Integer limit,
                                         @QueryParam("where") String where,
                                         @QueryParam("aggregates") Boolean agg,
                                         @QueryParam("distinct") Boolean distinct,
                                         @QueryParam("orderby") String orderby) {

        StringBuilder query = new StringBuilder();

        // Query first for the prefix/len
        query.append("SELECT distinct prefix,prefix_len,prefix_bits FROM rib\n");
        query.append("        WHERE isWithdrawn = False and prefix_bcast_bin >= inet6_aton('" + ip + "')\n");
        query.append("               and prefix_bin <= inet6_aton('" + ip + "')\n");
        query.append("               and peer_hash_id = '" + peerHashId + "'\n");
        query.append("         ORDER BY prefix_bin desc limit 1\n");

        long startTime = System.currentTimeMillis();
        System.out.println("QUERY: \n" + query.toString() + "\n");

        Map<String, List<DbColumnDef>> ResultsMap;
        ResultsMap = DbUtils.select_DbToMap(mysql_ds, query.toString());

        if (ResultsMap.size() <= 0) {
            // No results to return
            return RestResponse.okWithBody("{}");
        } else {
            // Query v_routes for the prefix found
            String prefix = ResultsMap.entrySet().iterator().next().getValue().get(0).getValue();
            String prefix_len = ResultsMap.entrySet().iterator().next().getValue().get(1).getValue();
            String prefix_bits = ResultsMap.entrySet().iterator().next().getValue().get(2).getValue();

            query = new StringBuilder();
            query.append(" peer_hash_id = '" + peerHashId + "' AND \n");

            if (agg == null) {
                query.append(" prefix = \"" + prefix + "\"");
                query.append(" AND prefixlen = " + prefix_len);

            } else {

                if (ip.contains(".")) {
                    query.append(" isIPv4 = 1 and ");
                } else {
                    query.append(" isIPv4 = 0 and ");
                }

                String ip_bits = IpAddr.getIpBits(prefix);

                query.append(" prefix_bits IN (");
                for (int len = ip_bits.length(); len > 0; len--) {
                    query.append("'" + ip_bits.substring(0, len) + "'");

                    if (len > 1) {
                        query.append(',');
                    }
                }

                query.append(") ");
            }

            if (where != null) {
                query.append(" and " + where);
            }

            if (distinct != null) {
                query.append(" GROUP BY prefix,prefixlen");
            }

            if (orderby == null && agg != null) {
                orderby = " prefixlen desc";
            }

            return RestResponse.okWithBody(
                    DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit,
                            query.toString() /* where str without the WHERE statement */, orderby,
                            (System.currentTimeMillis() - startTime)));
        }
    }

    @GET
    @Path("/history/{prefix}/{length}")
    @Produces("application/json")
    public Response getRibHistoryUpdates(@PathParam("prefix") String prefix,
                                  @PathParam("length") Integer length,
                                  @QueryParam("limit") Integer limit,
                                  @QueryParam("hours") Integer hours,
                                  @QueryParam("ts") String timestamp,
                                  @QueryParam("where") String where,
                                  @QueryParam("orderby") String orderby) {

        return getRibHistory(prefix, length, limit, hours, timestamp, where, orderby, HISTORY_TYPE.UPDATES, null);
    }

    @GET
    @Path("/withdraws/{prefix}/{length}")
    @Produces("application/json")
    public Response getRibHistoryWithdraws(@PathParam("prefix") String prefix,
                                  @PathParam("length") Integer length,
                                  @QueryParam("limit") Integer limit,
                                  @QueryParam("hours") Integer hours,
                                  @QueryParam("ts") String timestamp,
                                  @QueryParam("where") String where,
                                  @QueryParam("orderby") String orderby) {

        return getRibHistory(prefix, length, limit, hours, timestamp, where, orderby, HISTORY_TYPE.WITHDRAWS, null);
    }

    @GET
    @Path("/peer/{peerHashId}/history/{prefix}/{length}")
    @Produces("application/json")
    public Response getRibHistoryByPeer(@PathParam("prefix") String prefix,
                                        @PathParam("peerHashId") String peerHashId,
                                        @PathParam("length") Integer length,
                                        @QueryParam("limit") Integer limit,
                                        @QueryParam("hours") Integer hours,
                                        @QueryParam("ts") String timestamp,
                                        @QueryParam("where") String where,
                                        @QueryParam("orderby") String orderby) {

        return getRibHistory(prefix, length, limit, hours, timestamp, where, orderby, HISTORY_TYPE.UPDATES, peerHashId);
    }


    @GET
    @Path("/peer/{peerHashId}/withdraws/{prefix}/{length}")
    @Produces("application/json")
    public Response getRibWithdrawnsByPeer(@PathParam("prefix") String prefix,
                                           @PathParam("peerHashId") String peerHashId,
                                           @PathParam("length") Integer length,
                                           @QueryParam("limit") Integer limit,
                                           @QueryParam("hours") Integer hours,
                                           @QueryParam("ts") String timestamp,
                                           @QueryParam("where") String where,
                                           @QueryParam("orderby") String orderby) {

        return getRibHistory(prefix, length, limit, hours, timestamp, where, orderby, HISTORY_TYPE.WITHDRAWS, peerHashId);
    }

    private Response getRibHistory(String prefix, Integer length,
                                   Integer limit, Integer hours, String timestamp, String where, String orderby,
                                   HISTORY_TYPE type, String peerHashId) {

        // build a new query, based on view, force using index on timestamp
        if (timestamp != null && timestamp.equals("lastupdate")) {
            StringBuilder lastUpdateQueryBuilder = new StringBuilder();
            lastUpdateQueryBuilder.append("SELECT rtr.name AS RouterName, rtr.ip_address as RouterAddress,\n");
            lastUpdateQueryBuilder.append("         log.Prefix,log.PrefixLen,\n");
            lastUpdateQueryBuilder.append("         path.origin AS Origin,path.origin_as AS Origin_AS,\n");
            lastUpdateQueryBuilder.append("         path.med AS MED,path.local_pref AS LocalPref,path.next_hop AS NH,\n");
            lastUpdateQueryBuilder.append("         path.as_path AS AS_Path,path.as_path_count AS ASPath_Count,path.community_list AS Communities,\n");
            lastUpdateQueryBuilder.append("         path. ext_community_list AS ExtCommunities,\n");
            lastUpdateQueryBuilder.append("         path.cluster_list AS ClusterList,path.aggregator AS Aggregator,\n");
            lastUpdateQueryBuilder.append("         p.peer_addr AS PeerAddress,\n");
            lastUpdateQueryBuilder.append("         p.peer_as AS PeerASN, p.isIPv4 as isPeerIPv4, p.isL3VPNpeer as isPeerVPN,\n");
            lastUpdateQueryBuilder.append("         log.id,log.timestamp as LastModified,\n");
            lastUpdateQueryBuilder.append("         log.path_attr_hash_id, log.peer_hash_id,\n");
            lastUpdateQueryBuilder.append("         rtr.hash_id as router_hash_id\n");
            lastUpdateQueryBuilder.append("     FROM (SELECT\n");
            lastUpdateQueryBuilder.append("                prefix as Prefix,prefix_len as PrefixLen,id,timestamp,path_attr_hash_id,peer_hash_id\n");
            String tableName = "";
            int currentLimit = 1000;
            if (limit != null) {
                currentLimit = limit;
            }

            if (type.equals(HISTORY_TYPE.UPDATES)) {
                lastUpdateQueryBuilder.append("            FROM path_attr_log \n");  // with index (idx_ts)
                tableName = "v_routes_history";
            } else {
                tableName = "v_routes_withdraws";
                lastUpdateQueryBuilder.append("            FROM withdrawn_log \n");  // with index (idx_ts)
            }

            lastUpdateQueryBuilder.append("  WHERE prefix = '" + prefix);
            lastUpdateQueryBuilder.append("' AND prefix_len = " + length);
            if (peerHashId != null) {
                lastUpdateQueryBuilder.append(" AND peer_hash_id = '" + peerHashId + "' \n");
            }
            if (where != null) {
                lastUpdateQueryBuilder.append(" AND " + where );
            }
            lastUpdateQueryBuilder.append("\n   ORDER BY timestamp DESC LIMIT " + currentLimit + "\n");
            lastUpdateQueryBuilder.append("     ) log\n");
            lastUpdateQueryBuilder.append("     STRAIGHT_JOIN path_attrs path\n");
            lastUpdateQueryBuilder.append("          ON (log.path_attr_hash_id = path.hash_id AND \n");
            lastUpdateQueryBuilder.append("            log.peer_hash_id = path.peer_hash_id)\n");
            lastUpdateQueryBuilder.append("     STRAIGHT_JOIN bgp_peers p ON (log.peer_hash_id = p.hash_id)\n");
            lastUpdateQueryBuilder.append("     STRAIGHT_JOIN routers rtr ON (p.router_hash_id = rtr.hash_id)\n");
            System.out.println(lastUpdateQueryBuilder.toString());

            return RestResponse.okWithBody(DbUtils.select_DbToJson(mysql_ds, tableName, lastUpdateQueryBuilder.toString()));
        }

        if (length < 1 || length > 128)
            length = 32;

        StringBuilder where_str = new StringBuilder();

        where_str.append("Prefix = '" + prefix + "' ");
        where_str.append(" AND PrefixLen = " + length);

        if (peerHashId != null) {
            where_str.append(" AND peer_hash_id = '");
            where_str.append(peerHashId);
            where_str.append("'");
        }

        if (timestamp == null) {
            if (hours != null && hours >= 2)
                where_str.append(" and LastModified >= date_sub(current_timestamp, interval " + hours + " hour)");
            else
                where_str.append(" and LastModified >= date_sub(current_timestamp, interval 2 hour)");
            where_str.append(" and LastModified <= current_timestamp ");
        } else {
            if (hours != null && hours >= 2)
                where_str.append(" and LastModified >= date_sub('" + timestamp + "', interval " + hours + " hour)");
            else
                where_str.append(" and LastModified >= date_sub('" + timestamp + "', interval 2 hour)");
            where_str.append(" and LastModified <= '" + timestamp +"'");
        }

		if (where != null)
			where_str.append(" and " + where);

		String tableName = "v_routes_history";

		switch (type) {
			case UPDATES:
				tableName = "v_routes_history";
				break;

			case WITHDRAWS:
				tableName = "v_routes_withdraws";
		}

		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, tableName, limit, where_str.toString(), orderby)
		);
	}

    @GET
    @Path("/asn/{ASN}/specifics")
    @Produces("application/json")
    public Response getRibAsnSpecifics(@PathParam("ASN")BigInteger asn,
                                       @QueryParam("hours") Integer hours,
                                       @QueryParam("ts") String timestamp) {

        StringBuilder query = new StringBuilder();

        long startTime = System.currentTimeMillis();

        StringBuilder where_str = new StringBuilder();
        where_str.append("    WHERE ");

        String tsval = timestamp != null ? "'" + timestamp + "'" : "current_timestamp";

        if (hours != null && hours >= 2)
            where_str.append(" l.timestamp >= date_sub(" + tsval + ", interval " + hours + " hour)");
        else
            where_str.append(" l.timestamp >= date_sub(" + tsval + ", interval 2 hour)");

        where_str.append(" and l.timestamp <= " + tsval + "\n");


        /*
         * Get distinct prefixes for given ASN
         */
        query.append("SELECT distinct prefix,prefix_len,Origin_AS,prefix_bits\n");
        query.append("    FROM rib \n");
        query.append("    WHERE origin_as = " + asn);
        query.append("        AND isWithdrawn = False");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        Map<String, List<DbColumnDef>> ResultsMap;
        ResultsMap = DbUtils.select_DbToMap(mysql_ds, query.toString());

        System.out.println("Finished query, results " + ResultsMap.size());


        /*
         * Iterate over the list of distinct prefixes and run a check on each
         */
        Map<String, List<DbColumnDef>> ResultsMap_specifics;
        StringBuilder prefix_query = new StringBuilder();

        prefix_query.append(" (");

        for (Map.Entry<String, List<DbColumnDef>> entry : ResultsMap.entrySet()) {

            String prefix = entry.getValue().get(0).getValue();
            Integer prefix_len = Integer.valueOf(entry.getValue().get(1).getValue());
            String origin_asn = entry.getValue().get(2).getValue();

            String ip_bits = IpAddr.getIpBits(prefix);
            ip_bits = ip_bits.substring(0, prefix_len);

            //System.out.printf("entry: %s/%d %s %s\n", prefix, prefix_len, origin_asn, ip_bits);

            if (prefix_query.length() > 2) {
                prefix_query.append(" OR ");
            }

            prefix_query.append("r.prefix_bits like '");
            prefix_query.append(ip_bits);
            prefix_query.append("%'");
        }

        prefix_query.append(") ");

        StringBuilder querySpecifics = new StringBuilder();
        querySpecifics.append("SELECT r.prefix,r.prefix_len, r.origin_as OriginASN,\n");
        querySpecifics.append("          a.as_path,max(r.timestamp) as LastUpdate\n");

        querySpecifics.append("   FROM rib r \n");
        querySpecifics.append("         STRAIGHT_JOIN path_attrs a ON (r.path_attr_hash_id = a.hash_id)\n");

        querySpecifics.append("   WHERE ");
        querySpecifics.append(prefix_query);
        querySpecifics.append("            AND r.isWithdrawn = False\n");
        querySpecifics.append("            AND " + tsval + "\n");
        querySpecifics.append("   GROUP BY r.prefix_bits ORDER BY r.prefix_bin,r.prefix_len");

        System.out.println(" specifics query: " + querySpecifics.toString());

        ResultsMap_specifics = DbUtils.select_DbToMap(mysql_ds, querySpecifics.toString());

        return RestResponse.okWithBody(DbUtils.DbMapToJson("asn_specifics", ResultsMap_specifics, System.currentTimeMillis() - startTime));
    }

    private class PrefixEntry {
        public String prefix;
        public Integer prefix_len;
        public String prefix_bits;
        public Long origin_asn;
        public String origin_name;
        public Boolean isIPv4;

        PrefixEntry() {
            prefix      = null;
            prefix_len  = 0;
            prefix_bits = null;
            origin_asn  = 0L;
            origin_name = "";
            isIPv4      = Boolean.TRUE;
        }
    }

    /**
     * Cleanup
     */
	/**
    @PreDestroy
    void destory() {
        System.err.println("DESTORY");
    }
    */
}
