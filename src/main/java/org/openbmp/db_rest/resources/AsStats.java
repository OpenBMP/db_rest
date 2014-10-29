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

@Path("/as_stats")
public class AsStats {
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
	public Response getAsStats(@QueryParam("limit") Integer limit) {
		String limit_str = "";
		
		if (limit != null && limit >= 1 && limit <= 500000)
			limit_str += " limit " + limit;
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT s.*,as_name,country,org_name,max(s.timestamp) as timestamp\n");
		query.append("    FROM gen_asn_stats s left join gen_whois_asn w on (s.asn = w.asn)\n");
		query.append("    group by asn\n");
		query.append(limit_str);
	
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/{asn}")
	@Produces("application/json")
	public Response getAsStatsAsnHistory(@PathParam("asn") Integer asn,
										 @QueryParam("limit") Integer limit) {
		String limit_str = "";
		
		if (limit != null && limit >= 1 && limit <= 500000)
			limit_str += " limit " + limit;
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT s.*,as_name,country,org_name,max(s.timestamp) as timestamp\n");
		query.append("    FROM gen_asn_stats s left join gen_whois_asn w on (s.asn = w.asn)\n");
		query.append("    where s.asn = " + asn + "\n");
		query.append("    group by asn\n");
		query.append("    order by s.timestamp desc\n");
		query.append(limit_str);
	
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	
	@GET
	@Path("/ipv6")
	@Produces("application/json")
	public Response getAsStatsIpv6(@QueryParam("limit") Integer limit,
							  @QueryParam("topTransit") Integer topTransit,
							  @QueryParam("topOrigin") Integer topOrigin) {
		
		
		String limit_str = "";
		String orderby_str = "";
		
		if (limit != null && limit >= 1 && limit <= 500000)
			limit_str += " limit " + limit;
		
		if (topTransit != null) {
			orderby_str = " ORDER BY transit_v6_prefixes DESC";
			limit_str = " limit " + topTransit;
		}
		
		if (topOrigin != null) {
			orderby_str = " ORDER BY origin_v6_prefixes DESC";
			limit_str = " limit " + topOrigin;
		}
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT s.asn,transit_v6_prefixes,origin_v6_prefixes,\n");
		query.append("                as_name,country,org_name,max(s.timestamp) as timestamp \n");
		query.append("    FROM gen_asn_stats s left join gen_whois_asn w on (s.asn = w.asn)\n");
		query.append("    WHERE transit_v6_prefixes > 0 or origin_v6_prefixes > 0");
		query.append("    group by asn\n");
		query.append(orderby_str);
		query.append(limit_str);
	
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
	
	@GET
	@Path("/ipv4")
	@Produces("application/json")
	public Response getAsStatsIpv4(@QueryParam("limit") Integer limit,
			 				  @QueryParam("topTransit") Integer topTransit,
			 				  @QueryParam("topOrigin") Integer topOrigin) {
		
		String limit_str = "";
		String orderby_str = "";
		
		if (limit != null && limit >= 1 && limit <= 500000)
			limit_str += " limit " + limit;
		

		
		if (limit != null && limit >= 1 && limit <= 500000)
			limit_str += " limit " + limit;
		
		if (topTransit != null) {
			orderby_str = " ORDER BY transit_v4_prefixes DESC";
			limit_str = " limit " + topTransit;
		}
		
		if (topOrigin != null) {
			orderby_str = " ORDER BY origin_v4_prefixes DESC";
			limit_str = " limit " + topOrigin;
		}
			
		StringBuilder query = new StringBuilder();
		query.append("SELECT s.asn,transit_v4_prefixes,origin_v4_prefixes,\n");
		query.append("                as_name,country,org_name,max(s.timestamp) as timestamp \n");
		query.append("    FROM gen_asn_stats s left join gen_whois_asn w on (s.asn = w.asn)\n");
		query.append("    WHERE transit_v4_prefixes > 0 or origin_v4_prefixes > 0");
		query.append("    group by asn\n");
		query.append(orderby_str);
		query.append(limit_str);
	
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.select_DbToJson(mysql_ds, query.toString()));
	}
}