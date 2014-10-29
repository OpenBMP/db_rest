package org.openbmp.db_rest.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.DefaultValue;
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

@Path("/aspath")
public class Aspath {
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
	public Response getAspath(@QueryParam("limit") Integer limit,
			                  @QueryParam("trim") String trim) {
		String limit_str = "";
		
		if (limit != null && limit >= 1 && limit <= 500000)
			limit_str += " limit " + limit;
		
		StringBuilder query = new StringBuilder();
		if (trim == null) {
			query.append("select as_path\n");
			query.append("    from path_attrs PARTITION p\n");
			query.append("    where (peer_hash_id,hash_id) in (select peer_hash_id,path_attr_hash_id from rib PARTITION )\n");
			query.append("    group by as_path order by null\n");
			query.append(limit_str);
		} else {
			query.append("select trim(substr(trim(trailing origin_as from as_path), LOCATE(' ', as_path, 2))) as as_path_trimed\n");
			query.append("    from path_attrs PARTITION p\n");
			query.append("    where length(trim(substr(trim(trailing origin_as from as_path), LOCATE(' ', as_path, 2)))) > 1\n");
			query.append("        AND (peer_hash_id,hash_id) in (select peer_hash_id,path_attr_hash_id from rib )\n");
			query.append("    group by as_path_trimed order by null\n");
			query.append(limit_str);
		}
			
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		// Run query and get map results of query
		long startTime = System.currentTimeMillis();
		Map<String,List<DbColumnDef>> AggRows;
		AggRows = DbUtils.selectPartitions_DbToMap(mysql_ds, query.toString(), limit, 0, 47, null);

		/*
		 * Reformat the asn path to be an array of paths
		 */
		for(Map.Entry<String, List<DbColumnDef>> row : AggRows.entrySet()) {
			List<DbColumnDef> cols = row.getValue();
			
		}
		
		// Format map into json and return result
		String output = DbUtils.DbMapToJson("AsPath", AggRows, (System.currentTimeMillis() - startTime));
		
		return RestResponse.okWithBody(output);
	}

	@GET
	@Path("/{ASN}")
	@Produces("application/json")
	public Response getAspathByAsn(@PathParam("ASN") Integer asn, 
								   @QueryParam("limit") Integer limit,
								   @QueryParam("trim") String trim) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();
		
		String limit_str = "";
		
		if (limit != null && limit >= 1 && limit <= 500000)
			limit_str += " limit " + limit;
	
		StringBuilder query = new StringBuilder();
		if (trim == null) {
			query.append("select as_path\n");
			query.append("    from path_attrs PARTITION \n");
			query.append("    where as_path regexp '.* " + asn + "([ ]|$)+'\n");
			query.append("         and hash_id in (select path_attr_hash_id from rib PARTITION )\n");
			query.append("    group by as_path order by null\n");
			
		} else {
			query.append("select trim(substr(trim(trailing origin_as from as_path), LOCATE(' ', as_path, 2))) as as_path_trimed\n");
			query.append("    from path_attrs PARTITION \n");
			query.append("    where as_path regexp '.* " + asn + "([ ]|$)+' \n");
			query.append("         and length(trim(substr(trim(trailing origin_as from as_path), LOCATE(' ', as_path, 2)))) > 1\n");
			query.append("         and hash_id in (select path_attr_hash_id from rib)\n");
			query.append("    group by as_path_trimed order by null\n");
		}
			
		System.out.println("QUERY: \n" + query.toString() + "\n");

		// Run query and get map results of query
		long startTime = System.currentTimeMillis();
		Map<String,List<DbColumnDef>> AggRows;
		AggRows = DbUtils.selectPartitions_DbToMap(mysql_ds, query.toString(), limit, 0, 47, null);
		
		// Format map into json and return result
		String output = DbUtils.DbMapToJson("AsPathByASN", AggRows, (System.currentTimeMillis() - startTime));
		
		return RestResponse.okWithBody(output);
	}
	
}