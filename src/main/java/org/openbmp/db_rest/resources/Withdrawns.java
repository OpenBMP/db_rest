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

@Path("/withdrawns")
public class Withdrawns {
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

    /**
     *
     * @param searchPeer Filter the result with a peer hash
     * @param searchPrefix Filter the result with a prefix
     * @param groupBy Groups the result by Peer IP or by prefix and prefix_len
	 * @param joinWhoisPrefix To join the whois prefix information or not
     * @param limit Limit of the data
     * @param startTimestamp The beginning of the desired time period
     * @param endTimestamp The end of the desired time period
     * @return The top limited number of withdraws grouped by peer or prefix in before given hours of a given timestamp
     */
    @GET
    @Path("/top")
    @Produces("application/json")
    public Response getWithdrawsTop(@QueryParam("searchPeer") String searchPeer,
                                    @QueryParam("searchPrefix") String searchPrefix,
                                    @QueryParam("groupBy") String groupBy,
									@QueryParam("joinWhoisPrefix") Boolean joinWhoisPrefix,
									@QueryParam("limit") Integer limit,
									@QueryParam("startTs") String startTimestamp,
									@QueryParam("endTs") String endTimestamp) {

        if (searchPeer!=null && searchPeer.equals("null"))
            searchPeer = null;
        if (searchPrefix!=null && searchPrefix.equals("null"))
            searchPrefix = null;
		if (startTimestamp!=null && startTimestamp.equals("null"))
			startTimestamp = null;
		if (endTimestamp!=null && endTimestamp.equals("null"))
			endTimestamp = null;

        if (joinWhoisPrefix == null)
            joinWhoisPrefix = Boolean.FALSE;

		if ((groupBy == null || groupBy.isEmpty())||(groupBy!=null&&groupBy.equals("null")))
			groupBy = "peer";

        if (groupBy.toLowerCase().equals("peer"))
            groupBy = "log.peer_hash_id";
        else if (groupBy.toLowerCase().equals("prefix"))
            groupBy = "log.prefix,log.prefix_len";

        if (limit == null || limit > 100 || limit < 1)
            limit = 20;

		if (endTimestamp == null || (endTimestamp!=null&&endTimestamp.length() < 1))
			endTimestamp = "current_timestamp";
		else
			endTimestamp="'" + endTimestamp + "'";
		if (startTimestamp==null || (startTimestamp!=null&&startTimestamp.length() < 1))
			startTimestamp= "date_sub(" + endTimestamp + ", interval " + 2 + " hour)";
		else
			startTimestamp="'" + startTimestamp + "'";

		StringBuilder query = new StringBuilder();

		query.append("      SELECT l.Prefix,l.PrefixLen,p.name as PeerName,p.peer_addr as PeerAddr,l.Count,l.peer_hash_id,\n");
		query.append("      r.name as RouterName, r.ip_address as RouterAddr, c.name as CollectorName, c.ip_address as CollectorAddr, c.admin_id as CollectorAdminID\n");
		if(joinWhoisPrefix){
			query.append("      ,pfx.descr as PrefixDescr, pfx.origin_as as OriginAS\n");
		}
		query.append("      FROM (SELECT prefix as Prefix,prefix_len as PrefixLen, count(*) as Count, peer_hash_id\n");
		query.append("      FROM withdrawn_log log\n");
		query.append("      WHERE log.timestamp >= "+startTimestamp +" AND log.timestamp <= " + endTimestamp + "\n");
		if(searchPeer!=null && !searchPeer.isEmpty()) {
			query.append("                     AND (log.peer_hash_id = \"" + searchPeer + "\")\n");
		}
		if(searchPrefix!=null && !searchPrefix.isEmpty()) {
			String[] prefix = searchPrefix.split("/");
			query.append("                     AND (log.prefix = \"" + prefix[0] + "\")\n");
			query.append("                     AND (log.prefix_len = " + prefix[1] + ")\n");
		}
		query.append("      GROUP BY " + groupBy+"\n");
		query.append("      ORDER BY Count desc) l\n");
		query.append("      JOIN bgp_peers p ON (l.peer_hash_id = p.hash_id)\n");
		query.append("      JOIN routers r ON (p.router_hash_id = r.hash_id)\n");
		query.append("      JOIN collectors c ON (r.collector_hash_id = c.hash_id)\n");
		if(joinWhoisPrefix){
			query.append("      LEFT JOIN gen_whois_route pfx ON (inet6_aton(l.Prefix) = pfx.prefix AND l.PrefixLen = pfx.prefix_len)\n");
		}
		query.append("      ORDER BY Count desc\n");
		query.append("      LIMIT " + limit + "\n");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(
                DbUtils.select_DbToJson(mysql_ds, query.toString()));
    }
	
	@GET
	@Path("/peer/{peerHashId}/top")
	@Produces("application/json")
	public Response getWithdrawnTopByPeer(@PathParam("peerHashId") String peerHashId,
			 				    @QueryParam("limit") Integer limit, 
			                    @QueryParam("hours") Integer hours, 
			                    @QueryParam("ts") String timestamp) {
		
		if (hours == null || hours >= 72 || hours < 1)
			hours = 2;
		
		if (limit == null || limit > 100 || limit < 1)
			limit = 25;
		
		if (timestamp == null || timestamp.length() < 1)
			timestamp = "current_timestamp";
		else
			timestamp = "'" + timestamp + "'";
		
		StringBuilder query = new StringBuilder();
//		query.append("SELECT log.count as Count, log.prefix as Prefix,log.prefix_len as PrefixLen,\n");
//		query.append("          if (length(r.name) > 0, r.name, r.ip_address) as RouterName,p.peer_addr as PeerAddr\n");
//		query.append("    FROM\n");
//		query.append("         (SELECT count(prefix) as count,prefix,prefix_len,peer_hash_id, timestamp\n");
//		query.append("             FROM withdrawn_log log  FORCE INDEX (idx_ts)\n");
//		query.append("             WHERE log.timestamp >= date_sub(current_timestamp, interval 2 hour) and\n");
//		query.append("                 log.timestamp <= current_timestamp\n");
//		query.append("                 AND log.peer_hash_id = '" + peerHashId + "'\n");
//      query.append("             GROUP BY prefix,prefix_len\n");
//      query.append("             ORDER BY count DESC limit 25\n");
//      query.append("         ) log\n");
//      query.append("    JOIN bgp_peers p ON (log.peer_hash_id = p.hash_id)\n");
//      query.append("    JOIN routers r on (r.hash_id = p.router_hash_id)\n");
//      query.append("    ORDER BY log.count DESC\n");

        query.append("SELECT log.prefix as Prefix,log.prefix_len as PrefixLen, p.name as PeerName, p.peer_addr as PeerAddr, count(*) as Count, log.peer_hash_id as peer_hash_id,\n");
        query.append("         if(length(r.name)>0 , r.name, r.ip_address) as RouterName\n");
        query.append("       FROM withdrawn_log log\n");
        query.append("       JOIN bgp_peers p ON (log.peer_hash_id = p.hash_id)\n");
        query.append("       JOIN routers r ON (r.hash_id = p.router_hash_id)\n");
        query.append("       WHERE log.timestamp >= date_sub(" + timestamp + ", interval " + hours + " hour) AND log.timestamp <= " + timestamp + "\n");
        query.append("             AND log.peer_hash_id = '" + peerHashId + "'\n");
        query.append("       GROUP BY prefix,prefix_len\n");
        query.append("       ORDER BY Count desc\n");
        query.append("       LIMIT " + limit + "\n");

		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/top/interval/{minutes}")
	@Produces("application/json")
	public Response getWithdrawnTopInterval(@PathParam("minutes") Integer minutes,
										  @QueryParam("limit") Integer limit,
										  @QueryParam("ts") String timestamp) {

		if (limit == null || limit > 100 || limit < 1)
			limit = 25;
		
		Integer interval = 5;
		if (minutes >= 1 && minutes <= 300) {
			interval = minutes;
		}

		if (timestamp == null || timestamp.length() < 1)
			timestamp = "current_timestamp";
		else
			timestamp = "'" + timestamp + "'";

		
		StringBuilder query = new StringBuilder();
		query.append("select from_unixtime(unix_timestamp(timestamp) - unix_timestamp(timestamp) % " + 
							(interval * 60) + ") as IntervalTime,\n");
		query.append("                   count(peer_hash_id) as Count\n");
		query.append("     FROM withdrawn_log\n"); 
		query.append("     WHERE timestamp >= date_sub(" + timestamp + ", interval " + (interval * limit) + " minute)\n");
		query.append("             and timestamp <= " + timestamp + "\n");
		query.append("     GROUP BY IntervalTime\n");
		query.append("     ORDER BY timestamp desc");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/peer/{peerHashId}/top/interval/{minutes}")
	@Produces("application/json")
	public Response getWithdrawnTopInterval(@PathParam("peerHashId") String peerHashId,
										  @PathParam("minutes") Integer minutes,
										  @QueryParam("limit") Integer limit,
										  @QueryParam("ts") String timestamp) {

		if (limit == null || limit > 100 || limit < 1)
			limit = 25;
		
		Integer interval = 5;
		if (minutes >= 1 && minutes <= 300) {
			interval = minutes;
		}

		if (timestamp == null || timestamp.length() < 1)
			timestamp = "current_timestamp";
		else
			timestamp = "'" + timestamp + "'";

		StringBuilder query = new StringBuilder();
		query.append("select from_unixtime(unix_timestamp(timestamp) - unix_timestamp(timestamp) % " + 
							(interval * 60) + ") as IntervalTime,\n");
		query.append("                   count(peer_hash_id) as Count\n");
		query.append("     FROM withdrawn_log\n"); 
		query.append("     WHERE timestamp >= date_sub(" + timestamp + ", interval " + (interval * limit) + " minute)\n");
		query.append("             and timestamp <= " + timestamp + "\n");
		query.append("             AND peer_hash_id = '" + peerHashId + "'\n");
		query.append("     GROUP BY IntervalTime\n");
		query.append("     ORDER BY timestamp desc");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}

	@GET
	@Path("/trend/interval/{seconds}")
	@Produces("application/json")
	public Response getWithdrawsOverTime(@QueryParam("searchPeer") String searchPeer,
									   @QueryParam("searchPrefix") String searchPrefix,
									   @PathParam("seconds") Float seconds,
									   @QueryParam("startTs") String startTimestamp,
									   @QueryParam("endTs") String endTimestamp) {

		if (searchPeer!=null&&searchPeer.equals("null"))
			searchPeer = null;
		if (searchPrefix!=null&&searchPrefix.equals("null"))
			searchPrefix = null;
		if (startTimestamp!=null&&startTimestamp.equals("null"))
			startTimestamp = null;
		if (endTimestamp!=null&&endTimestamp.equals("null"))
			endTimestamp = null;

		Float interval = 5 * 60f;
		if (seconds >= 1 && seconds <= 300*60) {
			interval = seconds;
		}


		if (endTimestamp == null || (endTimestamp!=null&&endTimestamp.length() < 1))
			endTimestamp = "current_timestamp";
		else
			endTimestamp="'" + endTimestamp + "'";
		if (startTimestamp==null || (startTimestamp!=null&&startTimestamp.length() < 1))
			startTimestamp= "date_sub(" + endTimestamp + ", interval " + 2 + " hour)";
		else
			startTimestamp="'" + startTimestamp + "'";

		StringBuilder query = new StringBuilder();

		if (searchPeer == null && searchPrefix == null) {
			// Use aggregated stats for large query
            query.append("SELECT from_unixtime(unix_timestamp(interval_time) - unix_timestamp(interval_time) % "
                    + interval + ") as IntervalTime,\n");
            query.append("               cast(sum(withdraws) as unsigned) as Count\n");
            query.append("      FROM gen_chg_stats_bypeer\n");
            query.append("      WHERE interval_time >= " + startTimestamp +
                    " AND interval_time <= " + endTimestamp + "\n");
            query.append("      GROUP BY IntervalTime ORDER BY IntervalTime\n");
		}
		else {
            // Below is the original query which is too slow when querying all peers/prefixes.
            //    This works fine when the dataset is smaller/filtered by peer/prefix.

			query.append("SELECT from_unixtime(unix_timestamp(l.timestamp) - unix_timestamp(l.timestamp) % " +
					interval + ") as IntervalTime,\n");
			query.append("               count(*) as Count\n");
			query.append("      FROM withdrawn_log l\n");
			//		query.append("      JOIN bgp_peers p ON (l.peer_hash_id = p.hash_id)\n");
			//		query.append("      JOIN routers r ON (p.router_hash_id = r.hash_id)\n");
			//		query.append("      JOIN collectors c ON (r.collector_hash_id = c.hash_id)\n");
			query.append("      WHERE l.timestamp >= " + startTimestamp + " AND l.timestamp <= " + endTimestamp + "\n");
			if (searchPeer != null && !searchPeer.isEmpty()) {
				query.append("                     AND (peer_hash_id = \"" + searchPeer + "\")\n");
			}
			if (searchPrefix != null && !searchPrefix.isEmpty()) {
				String[] prefix = searchPrefix.split("/");
				query.append("                     AND (prefix = \"" + prefix[0] + "\")\n");
				query.append("                     AND (prefix_len = " + prefix[1] + ")\n");
			}
			query.append("      GROUP BY IntervalTime\n");
			query.append("      ORDER BY l.timestamp");
		}

		System.out.println("QUERY: \n" + query.toString() + "\n");

		return RestResponse.okWithBody(
				DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
}
