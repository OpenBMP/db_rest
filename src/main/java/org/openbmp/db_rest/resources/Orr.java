/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.net.util.SubnetUtils;

@Path("/orr")
public class Orr {
	@Context ServletContext ctx;
	@Context UriInfo uri;


    private DataSource mysql_ds;

    private enum THREAD_METHODS {
        GET_IGP_RIB, GET_PEER_ROUTER_ID
    }

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
	 * Run query to get the IGP for the given peer/routerId
	 *
	 * @param peerHashId		BGP peer hash id
	 * @param routerId			Router ID (IPv4 or IPv6) printed form
	 * @param protocol			Either 'ospf' or 'isis'
	 * @param max_age			Maximum allowable age for cached IGP SPF
	 *
	 *
	 * @returns Results are returned as a Map list of column definitions
	 * 		NULL will be returned if there was an error getting the RIB.
	 */
	Map<String,List<DbColumnDef>> getIGPRib(String peerHashId, String routerId, String protocol, int max_age) {

		String tableName = null;
		String spfProc = null;
        String route_type_field = "";

		if (protocol.equalsIgnoreCase("ospf")) {
			tableName = "igp_ospf_" + routerId.replace('.',  '_');
			spfProc = "{call ls_ospf_spf(?, ?, ?,?)}";
            route_type_field = "ospf_route_type";
		}
		else if (protocol.equalsIgnoreCase("isis")) {
			tableName = "igp_isis_" + routerId.replace('.',  '_');
			spfProc = "{call ls_isis_spf(?, ?, ?,?)}";
            route_type_field = "isis_type";
		}

		StringBuilder query = new StringBuilder();

		// first call stored procedure to generate the IGP/SPF table.
		Connection conn = null;
		CallableStatement cs = null;

        try{
			conn = mysql_ds.getConnection();
			cs = conn.prepareCall(spfProc);
			cs.setString(1, peerHashId);
			cs.setString(2, routerId);
			cs.setInt(3, max_age);
			cs.registerOutParameter(4, Types.INTEGER);

			cs.execute();

			System.out.println(" SPF iterations = " + cs.getInt(3));

            // WARNING: If changing the order or adding/removing columns to the below, make sure
            //    to update getIgpPrefixMetric() and mergeIgpWithBgp() - they expect column 6 to be the metric

			query.append("select igp.prefix as prefix,igp.prefix_len,\n");
            query.append("          concat('" + protocol + "') as protocol,\n");
            query.append("          l.neighbor_addr as NH,\n");
            query.append("          concat('') as ORR,\n");
			query.append("           "+ route_type_field + " as Type,igp.metric,n.igp_router_id as src_router_id,\n");
			query.append("           nei.igp_router_id as nei_router_id,\n");
			query.append("           igp.path_router_ids,igp.path_hash_ids,\n");
			query.append("           l.neighbor_addr as neighbor_addr,\n");
			query.append("           igp.peer_hash_id,p.router_hash_id\n");
			query.append("    FROM " + tableName + " igp JOIN ls_nodes n ON (igp.src_node_hash_id = n.hash_id)\n");
			query.append("            JOIN bgp_peers p ON (igp.peer_hash_id = p.hash_id)\n");
			query.append("            JOIN ls_nodes nei ON (igp.nh_node_hash_id = nei.hash_id and nei.peer_hash_id = '" + peerHashId +"')\n");
			query.append("            LEFT JOIN ls_links l ON (igp.nh_node_hash_id = l.remote_node_hash_id and\n");
			query.append("                           igp.root_node_hash_id = l.local_node_hash_id and l.peer_hash_id = '" + peerHashId  + "')\n");
			query.append("    WHERE best = TRUE ");
			query.append("    GROUP BY igp.prefix,igp.prefix_len,l.neighbor_addr\n");

			System.out.println("QUERY: \n" + query.toString() + "\n");

		} catch (SQLException e) {
			if (e.getSQLState().equals("45000")) {
				System.out.println("Procedure returned error " + e.getErrorCode() + " : " + e.getMessage());
			} else {
				System.out.println("Error in query " + e.getErrorCode() + " : " + e.getMessage());
				e.printStackTrace();
			}
		} finally {
			try {
				if (cs != null)
					cs.close();
				if (conn != null)
					conn.close();

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return DbUtils.select_DbToMap(mysql_ds, query.toString());
	}

    /**
     * Runs query to get the routers node routerID based on peer_hash_id
     *
     * @param peerHashId		BGP peer hash id
     *
     * @returns Results are returned as a Map list of column definitions
     * 		NULL will be returned if there was an error getting the RIB.
     */
    Map<String,List<DbColumnDef>> getPeerRouterId(String peerHashId) {
        StringBuilder query = new StringBuilder();

        query.append("SELECT  if(protocol like 'OSPF%', Local_IGP_RouterId, Local_RouterId) as RouterId \n");
        query.append("     FROM v_peers p JOIN v_ls_prefixes lp ON (p.peer_hash_id = lp.peer_hash_id and LocalIP = lp.prefix)\n");
        query.append("     WHERE p.peer_hash_id = '" + peerHashId + "' LIMIT 1");

        System.out.println("QUERY: " + query.toString());

        return DbUtils.select_DbToMap(mysql_ds, query.toString());
    }

    /**
     * Lookup the IGP metric for the prefix (e.g. next hop) passed
     *
     * @param prefix            IP in print form (this is the next-hop)
     * @param igpMap            IGP RIB map
     *
     * @return null if no match, otherwise the column def list that matches is returned
     */
    List<DbColumnDef> getIgpPrefixMetric(String prefix, Map<String, List<DbColumnDef>> igpMap) {
        List<DbColumnDef> found_cols = null;

        System.out.println("Lookup IGP metric cost for prefix " + prefix);

        for(Map.Entry<String, List<DbColumnDef>> row : igpMap.entrySet()) {
            // Prefix,PrefixLen,LocalPref,ASPath_Count,Origin,MED,NH
            List<DbColumnDef> cols = row.getValue();

            SubnetUtils subU = new SubnetUtils(cols.get(0).getValue() + '/' + cols.get(1).getValue());
            subU.setInclusiveHostCount(true);

            if (subU.getInfo().isInRange(prefix)) {
                System.out.println("    Found matching prefix, metric is " + cols.get(6).getValue());
                found_cols = cols;
                break;
            }
        }

        return found_cols;
    }

    /**
     * Merge BGP Map into IGP Map by doing a best path selection
     *
     * TODO: support addpaths/option for equal bgp paths
     *
     * NOTE: Assumed that always-compare-med is enabled
     *       Currently not checking if ibgp or ebgp - this will be added once the collector is updated
     *       to add this column to the bgp_peers table.  (it's not efficient to do this via v_peers)
     *
     * NOTE:  If the igpMap is for an RR (isRR_igp is true) then srcRtr_igpMap should be the source
     *      router IGP map.  bgpMap will be updated with the source router IGP next hop cost/path
     *      for RR entries. We do this because we want to see the path and metric from the source router
     *      using the non-optimized selection that the RR would choose.
     *
     * @param igpMap        IGP Map to the IGP RIB data
     * @param bgpMap        Map to the BGP RIB data
     * @param srcRtr_igpMap IGP Map for the source router, this is null/ignored if igpMap is the RR
     * @param isRR_igp      True if the IGP is from the route-reflector
     *
     * @returns a igp column MAP with BGP prefixes, caller should add this map as needed to existing maps
     */
    Map<String,List<DbColumnDef>> mergeIgpWithBgp(Map<String,List<DbColumnDef>> igpMap,
                                                  Map<String,List<DbColumnDef>> bgpMap,
                                                  Map<String,List<DbColumnDef>> srcRtr_igpMap,
                                                  Boolean isRR_igp) {

        Map<String,List<DbColumnDef>> mergeMap = new HashMap<String,List<DbColumnDef>>();

        String prev_key = null;
        List<DbColumnDef> prev_cols = null;
        List<DbColumnDef> prev_igp_nh_cols = null;

        DbColumnDef protoCol = new DbColumnDef();
        protoCol.setName("protocol");
        protoCol.setTableName("v_routes");
        protoCol.setType(Types.VARCHAR);
        protoCol.setValue("bgp");

        DbColumnDef orrCol = new DbColumnDef();
        orrCol.setName("ORR");
        orrCol.setTableName("orr");
        orrCol.setType(Types.VARCHAR);

        if (isRR_igp)
            orrCol.setValue("Not optimized, RR metric selection");
        else
            orrCol.setValue("Optimized - Local IGP metric selection");

        boolean merge;
        for(Map.Entry<String, List<DbColumnDef>> row : bgpMap.entrySet()) {
            merge = false;

            // Prefix,PrefixLen,LocalPref,ASPath_Count,Origin,MED,NH
            List<DbColumnDef> cols = row.getValue();

            if (prev_cols == null) {
                prev_key = row.getKey();
                prev_cols = cols;
                prev_igp_nh_cols = getIgpPrefixMetric(cols.get(6).getValue(), igpMap);
                continue;
            }

            // check if Prefix/len is the same as previous
            else if (prev_cols.get(0).getValue().equals(cols.get(0).getValue()) &&
                    Integer.parseInt(prev_cols.get(1).getValue()) == Integer.parseInt(cols.get(1).getValue()) ) {
                // Duplicate prefix, choose the best path

                // prefer higher local pref
                if (Integer.parseInt(prev_cols.get(2).getValue()) > Integer.parseInt(cols.get(2).getValue()))
                    merge = true;

                // Prefer shortest AS PATH (zero is locally originated)
                else if (Integer.parseInt(prev_cols.get(3).getValue()) < Integer.parseInt(cols.get(3).getValue()))
                    merge = true;

                // Prefer lower origin
                else if (! prev_cols.get(4).getValue().equals(cols.get(4).getValue()) &&
                        ( prev_cols.get(4).getValue().equals("igp") ||
                                (prev_cols.get(4).equals("egp") && ! cols.get(4).equals("igp"))) )
                    merge = true;

                // Prefer lower med (always compare med)
                else if (Integer.parseInt(prev_cols.get(5).getValue()) < Integer.parseInt(cols.get(5).getValue()))
                    merge = true;

                // TODO: Add check for iBGP vs eBGP when column is added to bgp_peers table

                // Tie breaker is going to be next hop IGP cost
                else {
                    List<DbColumnDef> igp_nh_cols = getIgpPrefixMetric(cols.get(6).getValue(), igpMap);

                    // If the current entry has a lower IGP metric, then set previous to current and move to next
                    if (igp_nh_cols != null &&
                            Integer.parseInt(prev_igp_nh_cols.get(6).getValue()) > Integer.parseInt(igp_nh_cols.get(6).getValue())) {
                        prev_cols = cols;
                        prev_igp_nh_cols = igp_nh_cols;
                        prev_key = row.getKey();
                    }
                 }
            }
            else {
                merge = true;
            }

            if (merge) {

                List<DbColumnDef> addPrefix = new ArrayList<DbColumnDef>();

                if (isRR_igp && srcRtr_igpMap != null) {
                    // Lookup the IGP next hop info from the source router instead of RR based on the RR selection
                    addPrefix.addAll(getIgpPrefixMetric(prev_cols.get(6).getValue(), srcRtr_igpMap));
                }
                else {
                    addPrefix.addAll(prev_igp_nh_cols);
                }

                addPrefix.set(0, prev_cols.get(0));                     // Prefix
                addPrefix.set(1, prev_cols.get(1));                     // Prefix len
                addPrefix.set(2, protoCol);                             // Protocol
                addPrefix.set(3, prev_cols.get(6));                     // NH
                addPrefix.set(4, orrCol);                               // ORR

                mergeMap.put(prev_key, addPrefix);               // Add prefix

                System.out.println("   Add prefix " + addPrefix.get(0).getValue() + "/" + addPrefix.get(1).getValue() +
                        " NH " + addPrefix.get(4).getValue() + " metric " + addPrefix.get(6).getValue());

                prev_key = row.getKey();
                prev_cols = cols;
                prev_igp_nh_cols = getIgpPrefixMetric(cols.get(6).getValue(), igpMap);
            }
        }

        // Last previous will need to be added
        if (prev_cols != null && prev_igp_nh_cols != null) {
            List<DbColumnDef> addPrefix = new ArrayList<DbColumnDef>();

            if (isRR_igp && srcRtr_igpMap != null ) {
                // Lookup the IGP next hop info from the source router instead of RR based on the RR selection
                addPrefix.addAll(getIgpPrefixMetric(prev_cols.get(6).getValue(), srcRtr_igpMap));
            }
            else {
                addPrefix.addAll(prev_igp_nh_cols);
            }


            addPrefix.set(0, prev_cols.get(0));                     // Prefix
            addPrefix.set(1, prev_cols.get(1));                     // Prefix len
            addPrefix.set(2, protoCol);                             // Protocol
            addPrefix.set(3, prev_cols.get(6));                     // NH
            addPrefix.set(4, orrCol);                               // ORR
            mergeMap.put(prev_key, addPrefix);                      // Add prefix

            System.out.println("   Add prefix " + addPrefix.get(0).getValue() + "/" + addPrefix.get(1).getValue() +
                    " NH " + addPrefix.get(3).getValue() + " metric " + addPrefix.get(6).getValue());
        }


        return mergeMap;
    }

    /**
     * Get IGP RIB with merged BGP RIB for given RouterId
     *
     * Merged BGP RIB will contain duplicate entries for selected BGP paths based
     *     on optimized next-hop selection and RR selection. For example:
     *
     *     prefix 10.0.0.0/8 is equal in attributes but has two possible next-hops
     *                  2.2.2.2 and 4.4.4.4, resulting tie breaker is IGP metric.
     *
     *        Local routerId IGP has metric 20 to 2.2.2.2 and metric 31 to 4.4.4.4
     *        RR IGP has metric 10 to 4.4.4.4 and 31 to 2.2.2.2.
     *
     *        Merged table would contain:
     *          10.0.0.0/8 via 2.2.2.2 metric 20 (metric 31 on rr) marked as preferred (orr selected)
     *          10.0.0.0/8 via 4.4.4.4 metric 31 (metric 10 on rr) marked as not preferred (rr selected w/o orr)
     *
     * NOTE:  The below method assumes (thus requires) that the bgp peer router
     *        advertising the link-state data is the route-reflector.  In other words,
     *        peerHashId (bgp peer hash) learned from bmp router_hash_id is the
     *        route-reflector.
     *
     *        We can change this to allow selection of BGP peers (one or more bmp routers)
     *        for the BGP RIB merge, but that requires more path/query params and is not needed right now.
     *
     *  @param peerHashId       Peer Hash ID of the BGP peer advertising link state information
     *  @param protocol         Either 'ospf' or 'isis'
     *  @param routerId         IPv4 or IPv6 print form router ID (use IPv4/IPv6 rid for ISIS)
     *  @param where            Advanced WHERE clause to filter the BGP merged prefixes
     */
    @GET
	@Path("/peer/{peerHashId}/{protocol}/{routerId}")
	@Produces("application/json")
	public Response getLsOspfIGP(@PathParam("peerHashId") String peerHashId,
                                 @PathParam("protocol") String protocol,
							     @PathParam("routerId") String routerId,
                                 @QueryParam("where") String where) {

		long startTime = System.currentTimeMillis();

        ScheduledExecutorService thrPool = Executors.newScheduledThreadPool(2);
        queryThread thrs[] = new queryThread[2];

        // Get IGP for requested router
		thrs[0] = new queryThread(this);
        thrs[0].setArgs(new String[]{peerHashId, routerId, protocol, "30"});
        thrPool.schedule(thrs[0], 0, TimeUnit.MILLISECONDS);

        // Get the router's local router id
        // TODO: Change to support better identification of route reflector
        Map<String,List<DbColumnDef>> rrMap = getPeerRouterId(peerHashId);

        String rr_routerId = null;
        if (rrMap.size() > 0)
            rr_routerId = rrMap.entrySet().iterator().next().getValue().get(0).getValue();

        if (rr_routerId == null) {
            System.out.println("Unable to get the routers routerID by peer hash " + peerHashId);
            return RestResponse.okWithBody("{}");
        }

        // Get the RR IGP
        thrs[1] = new queryThread(this);
        thrs[1].setArgs(new String[]{peerHashId, rr_routerId, protocol, "120"});
        thrPool.schedule(thrs[1], 0, TimeUnit.MILLISECONDS);


        // Wait for IGP query and store the results
        waitForThread(thrs[0]);
        Map<String,List<DbColumnDef>> igpMap = thrs[0].getResults();

        if (igpMap.size() <= 0) {
            return RestResponse.okWithBody("{}");
        }

        // Get the BGP RIB from RR router
        List<DbColumnDef> row = igpMap.entrySet().iterator().next().getValue();
        String routerHashId = row.get(row.size() - 1).getValue();

        String where_str = "router_hash_id = '" + routerHashId + "'";

        if (where != null)
            where_str += " and " + where;

        StringBuilder query = new StringBuilder();
        query.append("SELECT Prefix as prefix,PrefixLen as prefix_len,LocalPref,ASPath_Count,Origin,MED,NH\n");
        query.append("     FROM v_routes WHERE ");
        query.append(where_str);
        query.append(" ORDER BY prefix_bin,PrefixLen LIMIT 1000\n");

        Map<String,List<DbColumnDef>> bgpMap = DbUtils.select_DbToMap(mysql_ds, query.toString());


        // Wait for RR IGP query and store the results
        waitForThread(thrs[1]);
        Map<String,List<DbColumnDef>> rr_igpMap = thrs[1].getResults();

        //long queryTime = System.currentTimeMillis() - startTime;

        thrPool.shutdownNow();

        /*
         * Merge BGP RIB into IGP
         */

        Map<String,List<DbColumnDef>> mergeMap = new HashMap<String,List<DbColumnDef>>();
        mergeMap.putAll(igpMap);

        mergeMap.putAll(mergeIgpWithBgp(igpMap, bgpMap, null, false));

        mergeMap.putAll(mergeIgpWithBgp(rr_igpMap, bgpMap, igpMap, true));

        long queryTime = System.currentTimeMillis() - startTime;

		return RestResponse.okWithBody(
					DbUtils.DbMapToJson("orr", mergeMap, queryTime));
	}


    private void waitForThread(queryThread thr) {
        boolean done = false;
        int i = 0;
        while (! done && i < 200) {         // Wait up to 5 seconds
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (thr.isDone()) {
                done = true;
            }

            i++;
        }
    }

    // Query thread to run in a loop for each partition
    static public class queryThread implements Runnable {
        private boolean done = false;
        private long startTime = 0;
        private long runTime = 0;
        private THREAD_METHODS method;
        Orr parent;


        private Map<String,List<DbColumnDef>> results = null;
        private String args[];

        queryThread(Orr parent) {
            this.parent = parent;
            method = THREAD_METHODS.GET_IGP_RIB;
        }

        public void run() {
            startTime = System.currentTimeMillis();

            switch (method) {
                case GET_IGP_RIB:
                    if (args.length == 4) {
                        results = parent.getIGPRib(args[0], args[1], args[2], Integer.parseInt(args[3]));
                    }
                    break;

                case GET_PEER_ROUTER_ID:
                    if (args.length == 1) {
                        results = parent.getPeerRouterId(args[0]);
                    }
                    break;

                default:
                    break;
            }

            runTime = System.currentTimeMillis() - startTime;
            done = true;
        }

        public Map<String, List<DbColumnDef>> getResults() {
            return results;
        }

        public void setResults(Map<String, List<DbColumnDef>> results) {
            this.results = results;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getRunTime() {
            return runTime;
        }

        public void setRunTime(long runTime) {
            this.runTime = runTime;
        }

        public THREAD_METHODS getMethod() {
            return this.method;
        }

        public void setMethod(THREAD_METHODS method) {
            this.method = method;
        }

        public String[] getArgs() {
            return this.args;
        }

        public void setArgs(String args[]) {
            this.args = args;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }
}
