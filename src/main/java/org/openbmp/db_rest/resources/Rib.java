/*
 * Copyright (c) 2014-2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import java.util.List;
import java.util.Map;

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

import org.openbmp.db_rest.DbColumnDef;
import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;
import org.openbmp.db_rest.helpers.IpAddr;

@Path("/rib")
public class Rib {
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
	public Response getRib(@QueryParam("limit") Integer limit,
						   @QueryParam("where") String where,
						   @QueryParam("orderby") String orderby) {
		
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit, where, orderby));
	}
	
	@GET
	@Path("/peer/{peerHashId}")
	@Produces("application/json")
	public Response getRibByPeer( @PathParam("peerHashId") String peerHashId,
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
	public Response getRibByRouter( @PathParam("routerHashId") String routerHashId,
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
            query.append("    r.timestamp AS LastModified, r.db_timestamp as DBLastModified,r.prefix_bin as prefix_bin,\n");
        }

        query.append("    r.hash_id as rib_hash_id,\n");
        query.append("    r.path_attr_hash_id as path_hash_id, r.peer_hash_id, rtr.hash_id as router_hash_id\n");
        query.append("    FROM bgp_peers p JOIN rib r force index (idx_origin_as)ON (r.peer_hash_id = p.hash_id)\n");
		query.append("    JOIN path_attrs path ON (path.hash_id = r.path_attr_hash_id)\n");
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
            query.append("    r.timestamp AS LastModified, r.db_timestamp as DBLastModified,r.prefix_bin as prefix_bin,\n");
        }

        query.append("    r.hash_id as rib_hash_id,\n");
        query.append("    r.path_attr_hash_id as path_hash_id, r.peer_hash_id, rtr.hash_id as router_hash_id\n");
        query.append("    FROM bgp_peers p JOIN rib r force index (idx_origin_as)ON (r.peer_hash_id = p.hash_id)\n");
        query.append("    JOIN path_attrs path ON (path.hash_id = r.path_attr_hash_id)\n");
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
	@Path("/prefix/{prefix}")
	@Produces("application/json")
	public Response getRibByPrefix(@PathParam("prefix") String prefix,
						   @QueryParam("limit") Integer limit,
						   @QueryParam("where") String where,
						   @QueryParam("orderby") String orderby) {
		
		String where_str = "Prefix like '" + prefix + "%' ";
		
		if (where != null)
			where_str += " and " + where;
		
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit, where_str, orderby));
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
						   			     @QueryParam("where") String where,
						   			     @QueryParam("orderby") String orderby) {
		
		if (length < 1 || length > 128)
			length = 32;
		
		String where_str = "Prefix = '" + prefix + "' and PrefixLen = " + length;
		
		if (where != null)
			where_str += " and " + where;
		
		
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, "v_routes", limit, where_str, orderby));
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
	
	@GET
	@Path("/lookup/{IP}")
	@Produces("application/json")
	public Response getRibByLookup(@PathParam("IP") String ip,
						           @QueryParam("limit") Integer limit,
						           @QueryParam("where") String where,
                                   @QueryParam("aggregates") Boolean agg,
                                   @QueryParam("distinct") Boolean distinct,
						           @QueryParam("orderby") String orderby) {
		
		StringBuilder query = new StringBuilder();


		// Query first for the prefix/len
		query.append("SELECT distinct prefix,prefix_len,prefix_bits \n");
		query.append("        FROM rib\n");
		query.append("        WHERE isWithdrawn = False and prefix_bcast_bin >= inet6_aton('" + ip + "')\n");
		query.append("               and prefix_bin <= inet6_aton('" + ip + "')\n");
        query.append("        ORDER BY prefix_bin desc limit 1\n");
		
		long startTime = System.currentTimeMillis();
		System.out.println("QUERY: \n" + query.toString() + "\n");
        
     	Map<String,List<DbColumnDef>> ResultsMap;
     	ResultsMap = DbUtils.select_DbToMap(mysql_ds, query.toString());
		//ResultsMap = DbUtils.selectPartitions_DbToMap(mysql_ds, query.toString(), 2, 0, 47, null);
        
     	if (ResultsMap.size() <= 0) {
     		// No results to return
     		return RestResponse.okWithBody("{}");
     	} 
     	
     	else {
     		// Query v_routes for the prefix found
     		String prefix = ResultsMap.entrySet().iterator().next().getValue().get(0).getValue();
     		String prefix_len = ResultsMap.entrySet().iterator().next().getValue().get(1).getValue();
            String prefix_bits = ResultsMap.entrySet().iterator().next().getValue().get(2).getValue();
     		
     		query = new StringBuilder();

/*            query.append("SELECT if (length(rtr.name) > 0, rtr.name, rtr.ip_address) AS RouterName, \n");
            query.append("       if(length(p.name) > 0, p.name, p.peer_addr) AS PeerName,\n");
            query.append("       r.prefix AS Prefix,r.prefix_len AS PrefixLen,\n");
            query.append("       path.origin AS Origin,r.origin_as AS Origin_AS,path.med AS MED,\n");
            query.append("       path.local_pref AS LocalPref,path.next_hop AS NH,path.as_path AS AS_Path,\n");
            query.append("       path.as_path_count AS ASPath_Count,path.community_list AS Communities,\n");
            query.append("       path.ext_community_list AS ExtCommunities,path.cluster_list AS ClusterList,\n");
            query.append("       path.aggregator AS Aggregator,p.peer_addr AS PeerAddress, p.peer_as AS PeerASN,\n");
            query.append("       r.isIPv4 as isIPv4,p.isIPv4 as isPeerIPv4, p.isL3VPNpeer as isPeerVPN,\n");
            query.append("       r.timestamp` AS `LastModified`, r.db_timestamp as DBLastModified,r.prefix_bin as prefix_bin,\n");
            query.append("       r.hash_id as rib_hash_id,\n");
            query.append("       r.path_attr_hash_id as path_hash_id, r.peer_hash_id, rtr.hash_id as router_hash_id,r.isWithdrawn\n\n");

            query.append("   FROM bgp_peers p JOIN rib r ON (r.peer_hash_id = p.hash_id) \n");
            query.append("       JOIN path_attrs path ON (path.hash_id = r.path_attr_hash_id)\n");
            query.append("       JOIN routers rtr ON (p.router_hash_id = rtr.hash_id)\n");

            query.append("   WHERE r.isWithdrawn = False\n");*/

            if (agg == null) {
                query.append(" prefix = \"" + prefix + "\"");
                query.append(" AND prefixlen = " + prefix_len);

            } else {


                String ip_bits = IpAddr.getIpBits(prefix);

                query.append(" prefix_bits IN (");
                for (int len = ip_bits.length(); len > 0; len-- ) {
                    query.append("'" + ip_bits.substring(0,len) + "'");

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
		query.append("               and peer_hash_id = '"+ peerHashId + "'\n");
        query.append("         ORDER BY prefix_bin desc limit 1\n");
		
		long startTime = System.currentTimeMillis();
		System.out.println("QUERY: \n" + query.toString() + "\n");
        
     	Map<String,List<DbColumnDef>> ResultsMap;
     	ResultsMap = DbUtils.select_DbToMap(mysql_ds, query.toString());
        
     	if (ResultsMap.size() <= 0) {
     		// No results to return
     		return RestResponse.okWithBody("{}");
     	} 
     	
     	else {
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


                String ip_bits = IpAddr.getIpBits(prefix);

                query.append(" prefix_bits IN (");
                for (int len = ip_bits.length(); len > 0; len-- ) {
                    query.append("'" + ip_bits.substring(0,len) + "'");

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
	public Response getRibHistory(@PathParam("prefix") String prefix,
								  @PathParam("length") Integer length,
								  @QueryParam("limit") Integer limit,
								  @QueryParam("days") Integer days,
								  @QueryParam("where") String where,
								  @QueryParam("orderby") String orderby) {
		
		if (length < 1 || length > 128)
			length = 32;
		
		String	where_str = "Prefix = '" + prefix + "' and PrefixLen = " + length;
		
		if (days != null && days >= 15)
			where_str += " and LastModified >= date_sub(current_timestamp, interval " + days + " day)";		
		else
			where_str += " and LastModified >= date_sub(current_timestamp, interval 2 day)";
		
		where_str += " and LastModified <= current_timestamp ";
		
		if (where != null)
			where_str += " and " + where;
		
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, "v_routes_history", limit, where_str, orderby));
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
		return getRibTypeByPeer(prefix, peerHashId, length, limit, hours, timestamp, where, orderby, "history");
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
		return getRibTypeByPeer(prefix, peerHashId, length, limit, hours, timestamp, where, orderby, "withdraws");
	}

	private Response getRibTypeByPeer(String prefix, String peerHashId, Integer length,
									  Integer limit, Integer hours, String timestamp, String where, String orderby,
									  String type) {
		if (length < 1 || length > 128)
			length = 32;

		String	where_str = "peer_hash_id = '"+ peerHashId + "' and Prefix = '" + prefix + "' and PrefixLen = " + length;

        if (timestamp == null) {
            if (hours != null && hours >= 2)
                where_str += " and LastModified >= date_sub(current_timestamp, interval " + hours + " hour)";
            else
                where_str += " and LastModified >= date_sub(current_timestamp, interval 2 hour)";
			where_str += " and LastModified <= current_timestamp ";
        } else {  // replace default current_timestamp
            if (hours != null && hours >= 2)
                where_str += " and LastModified >= date_sub('" + timestamp +"', interval " + hours + " hour)";
            else
                where_str += " and LastModified >= date_sub('" + timestamp + "', interval 2 hour)";
			where_str += " and LastModified <= '" + timestamp + "' ";
        }

		if (where != null)
			where_str += " and " + where;

		String tableName = "v_routes_" + type;
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, tableName, limit, where_str, orderby)
		);
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
