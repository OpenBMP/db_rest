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
import java.util.List;
import java.util.Map;

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
            queryBuilder.append("SELECT prefix, prefix_len FROM rib AS r, community_analysis AS ca ");
            queryBuilder.append("WHERE r.path_attr_hash_id = ca.path_attr_hash_id AND r.peer_hash_id = ca.peer_hash_id ");
            queryBuilder.append("AND ca.community = '" + community + "'");
        } else if (p1 != null && isForP2) {
            queryBuilder.append("SELECT DISTINCT part2 FROM community_analysis WHERE part1 = '" + p1 + "'");
        } else if (p1 != null && !isForP2) {
            queryBuilder.append("SELECT DISTINCT part1 FROM community_analysis WHERE part1 LIKE '" + p1 + "%'");
        }

        if (limit != null)
            queryBuilder.append(limit_str);

        // Run query and get map results of query
        long startTime = System.currentTimeMillis();
        Map<String,List<DbColumnDef>> AggRows;
        AggRows = DbUtils.selectPartitions_DbToMap(mysql_ds, queryBuilder.toString(), limit, 0, 47, null);

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
    @Path("/getP2/{p1}")
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
    @Path("/getP1/{p1}")
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
