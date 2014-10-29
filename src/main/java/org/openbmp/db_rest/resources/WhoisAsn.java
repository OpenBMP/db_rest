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

@Path("/whois/asn")
public class WhoisAsn {
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
	public Response getWhois(@QueryParam("where") String where) {
		
		if (where == null) {
			System.out.println("Bad request");
			return Response.status(400).entity("")
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Methods", "GET")
					.allow("OPTIONS")
					.build();
		}
			
		String where_str = where;
		
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, "gen_whois_asn", null, where_str, null));
	}
		
	@GET
	@Path("/{asn}")
	@Produces("application/json")
	public Response getWhoisAsn(@PathParam("asn") Integer asn,
							  @QueryParam("where") String where) {
		
		String where_str = " asn = " + asn;
		if (where != null)
			where_str += " and " + where;
		
	
		return RestResponse.okWithBody(
				DbUtils.selectStar_DbToJson(mysql_ds, "gen_whois_asn", null, where_str, null));
	}
}