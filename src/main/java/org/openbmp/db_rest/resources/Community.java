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
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/community")
public class Community {
    @Context ServletContext ctx;
    @Context UriInfo uri;

    private DataSource mysql_ds;
    private static List<Thread> _threads = new ArrayList<Thread>(5);
    private List<SortedMap<String, List<DbColumnDef>>> subMapList = new ArrayList<SortedMap<String, java.util.List<DbColumnDef>>>(5);
    Map<String, List<DbColumnDef>> resultMap = new HashMap<String, List<DbColumnDef>>();

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

    private Response getCommunities(String community, Integer p1, Boolean isForP2, Integer limit) {
        String limit_str = "";
        if (limit != null && limit >= 1 && limit <= 500000) {
            limit_str += " limit " + limit;
        }

        StringBuilder queryBuilder = new StringBuilder();

        if (community != null) {
            Pattern p = Pattern.compile("^[0-9]+:[0-9]+$");
            Matcher m = p.matcher(community);

            queryBuilder.append("SELECT peer_hash_id, path_attr_hash_id FROM community_analysis  WHERE ");
            if (community.contains("%")){
                queryBuilder.append("community LIKE '" + community + "'");
            } else if (m.matches()) {
                queryBuilder.append("community = '" + community + "'");
            } else {
                queryBuilder.append("community REGEXP '" + community + "'");
            }

        } else if (p1 != null && isForP2) {
            queryBuilder.append("SELECT DISTINCT part2, community, count(*) as count FROM community_analysis WHERE part1 = '"
                    + p1
                    + "' group by community order by count desc");
        } else if (p1 != null && !isForP2) {
            queryBuilder.append("SELECT DISTINCT part1 FROM community_analysis WHERE part1 LIKE '" + p1 + "%'");
        }

        if (limit != null)
            queryBuilder.append(limit_str);

        // Run query and get map results of query
        long startTime = System.currentTimeMillis();
        Map<String,List<DbColumnDef>> AggRows;
        AggRows = DbUtils.select_DbToMap(mysql_ds, queryBuilder.toString());
        TreeMap<String, List<DbColumnDef>> treeMap = new TreeMap<String, List<DbColumnDef>>(AggRows);
        Collection<Future<?>> futures = new LinkedList<Future<?>>();

        if (community != null) {

            if (treeMap.size() > 5000) {
                int count = 0;
                List<String> ticks = new ArrayList<String>(6);
                // split the map into 5 submaps
                for (String key : treeMap.keySet()) {
                    if (count % (treeMap.size() / 5) == 0 ) {
                        ticks.add(key);
                    }
                    // reset the last tick to the final element
                    if (count == AggRows.size() - 1) {
                        ticks.set(5, key);
                    }
                    count ++;
                }
                for (int j = 0; j < 5; j++) {
                    subMapList.add(treeMap.subMap(ticks.get(j), ticks.get(j+1)));
                }
                // process submaps concurrently
                ExecutorService executorService = Executors.newFixedThreadPool(5);

                int threadNumber = 0;
                _threads.clear();
                for (; threadNumber < 5; threadNumber++) {
                    _threads.add(threadNumber, new ProcessThread(threadNumber));
                    futures.add(executorService.submit(_threads.get(threadNumber)));
                }
                try {
                    for (Future f : futures)
                        f.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                for (List<DbColumnDef> row : AggRows.values()) {
                    queryBuilder = new StringBuilder();
                    //select prefix, prefix_len, rtr.name, p.name, as_path, community_list, isWithdrawn from
                    // ((bgp_peers as p join rib as r on (r.peer_hash_id = p.hash_id)) join path_attrs as path
                    // on (path.hash_id = r.path_attr_hash_id)) join routers as rtr on (p.router_hash_id = rtr.hash_id) where
                    // r.peer_hash_id = 'da239dcc2bbafd9410921c98387f279b' and r.path_attr_hash_id = '52d56ee4a7bd2c062c8277c76932259a';
                    queryBuilder.append("SELECT prefix AS Prefix, prefix_len AS PrefixLen, rtr.name AS RouterName, p.name AS PeerName, as_path AS AS_Path, community_list AS Communities, isWithdrawn FROM ");
                    queryBuilder.append("((bgp_peers AS p JOIN rib AS r ON (r.peer_hash_id = p.hash_id)) JOIN path_attrs as path ");
                    queryBuilder.append("ON (path.hash_id = r.path_attr_hash_id)) JOIN routers AS rtr ON (p.router_hash_id = rtr.hash_id) WHERE ");
                    queryBuilder.append("r.peer_hash_id = '" + row.get(0).getValue() + "' ");
                    queryBuilder.append("AND r.path_attr_hash_id = '" + row.get(1).getValue() + "'");
                    if (limit != null)
                        queryBuilder.append(limit_str);
                    Map<String, List<DbColumnDef>> dbResult = DbUtils.select_DbToMap(mysql_ds, queryBuilder.toString());
                    if (!dbResult.isEmpty()) {
                        for (Map.Entry<String, List<DbColumnDef>> entry : dbResult.entrySet()) {
                            resultMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
            String result = DbUtils.DbMapToJson("Community", resultMap,  System.currentTimeMillis()-startTime);
            return RestResponse.okWithBody(result);
        }

        // Format map into json and return result
        String output = DbUtils.DbMapToJson("Community", AggRows, (System.currentTimeMillis() - startTime));

        return RestResponse.okWithBody(output);
    }

    private class ProcessThread extends Thread{
        private int threadNumber;

        public ProcessThread(int i) {
            this.threadNumber = i;
        }

        @Override
        public void run() {
            super.run();
            for (List<DbColumnDef> row : subMapList.get(threadNumber).values()) {
                StringBuilder queryBuilder = new StringBuilder();
                //select prefix, prefix_len, rtr.name, p.name, as_path, community_list, isWithdrawn from
                // ((bgp_peers as p join rib as r on (r.peer_hash_id = p.hash_id)) join path_attrs as path
                // on (path.hash_id = r.path_attr_hash_id)) join routers as rtr on (p.router_hash_id = rtr.hash_id) where
                // r.peer_hash_id = 'da239dcc2bbafd9410921c98387f279b' and r.path_attr_hash_id = '52d56ee4a7bd2c062c8277c76932259a';
                queryBuilder.append("SELECT prefix AS Prefix, prefix_len AS PrefixLen, rtr.name AS RouterName, p.name AS PeerName, as_path AS AS_Path, community_list AS Communities, isWithdrawn FROM ");
                queryBuilder.append("((bgp_peers AS p JOIN rib AS r ON (r.peer_hash_id = p.hash_id)) JOIN path_attrs as path ");
                queryBuilder.append("ON (path.hash_id = r.path_attr_hash_id)) JOIN routers AS rtr ON (p.router_hash_id = rtr.hash_id) WHERE ");
                queryBuilder.append("r.peer_hash_id = '" + row.get(0).getValue() + "' ");
                queryBuilder.append("AND r.path_attr_hash_id = '" + row.get(1).getValue() + "'");
                Map<String, List<DbColumnDef>> dbResult = DbUtils.select_DbToMap(mysql_ds, queryBuilder.toString());
                if (!dbResult.isEmpty()) {
                    for (Map.Entry<String, List<DbColumnDef>> entry : dbResult.entrySet()) {
                        Community.this.resultMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            System.out.print("it's done!");
        }
    }

    /**
     * Get the second part of the community starts with p1
     * @param p1 the first part of searched community
     * @param limit number of limit
     * @return a list of second part
     */
    @GET
    @Path("/part2/{p1}")
    @Produces("application/json")
    public Response getCommP2byP1(@PathParam("p1") Integer p1,
                                  @QueryParam("limit") Integer limit) {
        return getCommunities(null, p1, true, limit);
    }

    /**
     * Get suggestions for part1
     * @param p1 input part1
     * @param limit number of limit
     * @return a list of part1 starts with p1
     */
    @GET
    @Path("/part1/{p1}")
    @Produces("application/json")
    public Response getP1(@PathParam("p1") Integer p1,
                          @QueryParam("limit") Integer limit) {
        return getCommunities(null, p1, false, limit);
    }

    /**
     * Get prefixes that contain community
     * @param community target community
     * @param limit limit number
     * @return a list of prefixes
     */
    @GET
    @Produces("application/json")
    public Response getPrefixByComm(@QueryParam("community") String community,
                                    @QueryParam("limit") Integer limit) {
        return getCommunities(community, null, null, limit);
    }
}
