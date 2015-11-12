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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/community")
public class Community {
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

    private Response getCommunities(String community, Integer p1, Boolean isForP2, Integer limit) {
        String limit_str = "";
        if (limit != null && limit >= 1 && limit <= 500000) {
            limit_str += " limit " + limit;
        }

        StringBuilder queryBuilder = new StringBuilder();

        if (community != null) {
            Pattern p = Pattern.compile("^[0-9]+:[0-9]+$");
            Matcher m = p.matcher(community);

            queryBuilder.append("SELECT peer_hash_id, path_attr_hash_id FROM community_analysis AS ca WHERE ");
            if (community.contains("%")){
                queryBuilder.append("ca.community LIKE '" + community + "'");
            } else if (m.matches()) {
                queryBuilder.append("ca.community = '" + community + "'");
            } else {
                queryBuilder.append("ca.community REGEXP '" + community + "'");
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
        AggRows = DbUtils.selectPartitions_DbToMap(mysql_ds, queryBuilder.toString(), limit, 0, 47, null);

        if (community != null) {
            Map<String, List<DbColumnDef>> resultMap = new HashMap();
            for (List<DbColumnDef> row : AggRows.values()) {
                queryBuilder = new StringBuilder();
                queryBuilder.append("SELECT Prefix, PrefixLen, RouterName, PeerName, AS_Path, Communities FROM v_routes WHERE ");
                queryBuilder.append("peer_hash_id = '" + row.get(0).getValue() + "' ");
                queryBuilder.append("AND path_hash_id = '" + row.get(1).getValue() + "'");
                if (limit != null)
                    queryBuilder.append(limit_str);
                Map<String, List<DbColumnDef>> dbResult = DbUtils.select_DbToMap(mysql_ds, queryBuilder.toString());
                if (!dbResult.isEmpty()) {
                    for (Map.Entry<String, List<DbColumnDef>> entry : dbResult.entrySet()) {
                        resultMap.put(entry.getKey(), entry.getValue());
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
    @Path("/{community}")
    @Produces("application/json")
    public Response getPrefixByComm(@PathParam("community") String community,
                                    @QueryParam("limit") Integer limit) {
        return getCommunities(community, null, null, limit);
    }
}
