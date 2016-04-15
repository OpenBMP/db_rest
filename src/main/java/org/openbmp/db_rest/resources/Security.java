package org.openbmp.db_rest.resources;

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

/**
 * Created by junbao on 12/16/15.
 */
@Path("/security")
public class Security {
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
            mysql_ds = (DataSource) initctx.lookup("java:/comp/env/jdbc/MySQLDB");

        } catch (NamingException e) {
            System.err.println("ERROR: Cannot find resource configuration, check context.xml config");
            e.printStackTrace();
        }
    }

    private void processParas(int asn, String prefix, String whereClause, StringBuilder queryBuilder) {
        if (whereClause != null && !whereClause.isEmpty())
            queryBuilder.append(whereClause + ' ');
        if (asn != 0) {
            String conn = whereClause == null ? " WHERE " : " AND ";
            queryBuilder.append(conn + asn + " IN (recv_origin_as, rpki_origin_as, irr_origin_as) ");
        }
        if (prefix != null && !prefix.isEmpty()) {
            String conn;
            if (whereClause == null && asn == 0)
                conn = " WHERE ";
            else conn = " AND ";
            if (prefix.indexOf('/') == -1)
                queryBuilder.append(conn + " prefix = inet6_aton('" + prefix + "') ");
            else {
                int len = Integer.valueOf(prefix.split("/")[1]);
                prefix = prefix.split("/")[0];
                queryBuilder.append(conn + " prefix = inet6_aton('" + prefix + "') AND prefix_len = " + len);
            }
        }
    }

    /**
     * get total count
     * @return total count
     */
    @GET
    @Path("/total")
    @Produces("application/json")
    public Response getValidationTotal(@QueryParam("asn") int asn,
                                       @QueryParam("prefix") String prefix,
                                       @QueryParam("where") String whereClause) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT count(*) as total FROM gen_prefix_validation ");
        processParas(asn, prefix, whereClause, queryBuilder);
        String res = DbUtils.select_DbToJson(mysql_ds, null, queryBuilder.toString());
        return RestResponse.okWithBody(res);
    }

    /**
     * get mismatch prefixes based on pagination
     * @param page current page
     * @param limit page limit
     * @param whereClause
     * @param sort sort keyword
     * @param desc increasing or decreasing
     * @return
     */
    @GET
    @Path("/{page}/{limit}")
    @Produces("application/json")
    public Response getValidationPrefix(@PathParam("page") int page,
                                      @PathParam("limit") int limit,
                                      @QueryParam("where") String whereClause,
                                      @QueryParam("sort") String sort,
                                      @QueryParam("desc") Boolean desc,
                                      @QueryParam("asn") int asn,
                                      @QueryParam("prefix") String prefix) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT inet6_ntoa(prefix) AS prefix, prefix_len, recv_origin_as, rpki_origin_as, irr_origin_as, irr_source, timestamp FROM gen_prefix_validation ");
        processParas(asn, prefix, whereClause, queryBuilder);
        if (sort != null)
            queryBuilder.append(" ORDER BY " + sort);
        if (desc != null && desc)
            queryBuilder.append(" DESC ");
        queryBuilder.append(" LIMIT " + limit + " OFFSET " + (page - 1) * limit);
        String res = DbUtils.select_DbToJson(mysql_ds, queryBuilder.toString());
        return RestResponse.okWithBody(res);
    }

    @GET
    @Path("/stats")
    @Produces("application/json")
    public Response getStats(@QueryParam("asn") int asn,
                             @QueryParam("prefix") String prefix,
                             @QueryParam("where") String whereClause) {
        String[] whereArray = {
                " rpki_origin_as IS NOT NULL ",
                " irr_origin_as IS NOT NULL ",
                " irr_origin_as IS NULL and rpki_origin_as IS NULL ",
                " irr_origin_as IS NOT NULL and rpki_origin_as IS NOT NULL ",
                " (irr_origin_as IS NOT NULL and irr_origin_as != recv_origin_as) or (rpki_origin_as IS NOT null and rpki_origin_as != recv_origin_as) ",
                " rpki_origin_as IS NOT NULL AND rpki_origin_as IS NOT null and rpki_origin_as != recv_origin_as ",
                " irr_origin_as IS NOT NULL AND irr_origin_as IS NOT NULL and irr_origin_as != recv_origin_as ",
                " irr_origin_as IS NOT NULL and rpki_origin_as IS NOT NULL AND recv_origin_as != irr_origin_as or recv_origin_as != rpki_origin_as ",
        };

        StringBuilder queryBuilder = new StringBuilder(" SELECT count(*) total FROM gen_prefix_validation ");

        for (int i = 0; i < whereArray.length; i++) {
            queryBuilder.append(" UNION ALL SELECT count(*) total FROM gen_prefix_validation ");
            if (whereClause != null && !whereClause.isEmpty())
                processParas(asn, prefix, whereClause + whereArray[i], queryBuilder);
            else
                processParas(asn, prefix, " WHERE " + whereArray[i], queryBuilder);
        }

        String res = DbUtils.select_DbToJson(mysql_ds, queryBuilder.toString());

        return RestResponse.okWithBody(res);
    }
}