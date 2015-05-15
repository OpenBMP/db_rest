/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openbmp.db_rest.RestResponse;
import org.openbmp.db_rest.DbUtils;

@Path("/upstream")
public class Upstream {
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
	@Path("/{ASN}")
	@Produces("application/json")
	public Response getUpstream(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();
		
		StringBuilder query = new StringBuilder();
		
		/*
		query.append(" select cast(trim(substring_index(concat(\n");
	    query.append("           SUBSTRING_index(concat(as_path, ' '), ' " + asn + " ', 1), ' '), ' ', -2)) as unsigned) as asn,\n");
	    query.append("       as_name,country,org_name\n");
	    query.append("     from path_attrs PARTITION p join rib r ON (p.hash_id = r.path_attr_hash_id)\n");
	    query.append("         left join gen_whois_asn w ON (cast(trim(substring_index(concat(\n");
	    query.append("                  SUBSTRING_index(concat(as_path, ' '), ' "+ asn + " ', 1), ' '), ' ', -2)) as unsigned) = w.asn)\n");
		query.append("     where as_path regexp '.* " + asn + "([ ]|$)+'");
		query.append("     group by asn\n");
		query.append("     order by null\n");
		*/
		
		query.append("select p.asn,as_name,city,state_prov,country,org_name\n");
		query.append("   from (select hash_id,peer_hash_id,cast(trim(substring_index(concat(");
		query.append("             SUBSTRING_index(concat(as_path, ' '), ' " + asn + " ', 1), ' '), ' ', -2)) as unsigned) as asn\n");
		query.append("          from path_attrs PARTITION\n");
		query.append("          where as_path regexp '.* " + asn + "([ ]|$)+' \n");
		query.append("        ) p join rib r on (p.hash_id = r.path_attr_hash_id and p.peer_hash_id = r.peer_hash_id) \n");
		query.append("            left join gen_whois_asn w ON (p.asn = w.asn)\n");
		query.append("    where p.asn > 0\n");
		query.append("    group by p.asn\n");
		query.append("    order by p.asn\n");
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		return RestResponse.okWithBody(
					DbUtils.selectPartitions_DbToJson("upstreamASN", mysql_ds, query.toString(), 0, 47, null));
	}
	
	@GET
	@Path("/{ASN}/count")
	@Produces("application/json")
	public Response getUpstreamCount(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();
		
		StringBuilder query = new StringBuilder();
		query.append(" select p.asn, count(prefix) as Prefixes_Learned,\n");
	    query.append("           as_name,city,state_prov,country,org_name\n");
	    query.append("     from (select distinct hash_id,peer_hash_id,cast(trim(substring_index(concat(SUBSTRING_index(\n");
	    query.append("               concat(as_path, ' '), ' "+asn+" ', 1), ' '), ' ', -2)) as unsigned) as asn\n");
	    query.append("            from path_attrs PARTITION \n");
	    query.append("            where as_path regexp '.* "+asn+"([ ]|$)+' ) p\n");
	    query.append("        join rib r on (r.path_attr_hash_id = p.hash_id and r.peer_hash_id = p.peer_hash_id)\n");
	    query.append("        left join gen_whois_asn w ON (p.asn = w.asn)\n");
	    query.append("     where p.asn > 0\n");
		query.append("     group by p.asn\n");
		query.append("     order by p.asn\n");
		
		/*
		query.append("select asn, count(distinct prefix, prefix_len) as Prefixes_Learned,\n");
		query.append("              as_name,country,org_name\n");
		query.append("   from (select hash_id,peer_hash_id,cast(trim(substring_index(concat(SUBSTRING_index(");
		query.append("               concat(as_path, ' '), ' " + asn +" ', 1), ' '), ' ', -2)) as unsigned) as asn\n");
		query.append("           from path_attrs PARTITION\n");
		query.append("           where as_path regexp '.* " + asn +"([ ]|$)+'");
		query.append("	      ) p join rib r on (p.peer_hash_id = r.peer_hash_id and p.hash_id = r.path_attr_hash_id) \n");
		query.append("            left join gen_whois_asn w ON (p.asn = w.asn)\n");
		query.append("    group by asn\n");
		query.append("    order by asn\n");
		*/
		
		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		ArrayList<Integer> includeColumns = new ArrayList<Integer>();
		includeColumns.add(1);
		return RestResponse.okWithBody(
					DbUtils.selectPartitions_DbToJson("upstreamASN", mysql_ds, query.toString(), 0, 47, includeColumns));
	}
	
	@GET
	@Path("/{ASN}/peer/count")
	@Produces("application/json")
	public Response getUpstreamPeerCount(@PathParam("ASN") Integer asn) {
		if (asn == null)
			return Response.status(400).entity(
					"{ \"error\": \"Missing required path parameter\" }").build();
		
		StringBuilder query = new StringBuilder();
		query.append("select rtr.ip_address as BMPRouter, bp.peer_addr as PeerAddr, d.asn,d.Prefixes_Learned,\n");
		query.append("                   as_name,city,state_prov,country,org_name\n");
		query.append("        FROM (select peer_hash_id,asn, count(distinct prefix, prefix_len) as Prefixes_Learned\n");
		query.append("               from (select hash_id, cast(trim(substring_index(concat(SUBSTRING_index(\n");
		query.append("                        concat(as_path, ' '), ' "+ asn +" ', 1), ' '), ' ', -2)) as unsigned) as asn\n");
		query.append("               from path_attrs PARTITION \n");
		query.append("               where as_path regexp '.* "+ asn +"([ ]|$)+'\n");
		query.append("         ) p join rib r on (p.hash_id = r.path_attr_hash_id)\n");
		query.append("         where p.asn > 0\n");
		query.append("         group by p.asn\n");
		query.append("         order by null\n");
		query.append("     ) d\n");
		query.append("    JOIN bgp_peers bp on (d.peer_hash_id = bp.hash_id)\n"); 
		query.append("    JOIN routers rtr on (bp.router_hash_id = rtr.hash_id)\n");
		query.append("    left join gen_whois_asn w ON (d.asn = w.asn)\n");
	    
		query.append("    group by rtr.ip_address,bp.peer_addr,d.asn\n");
		query.append("    order by BMPRouter,PeerAddr,cast(d.asn as unsigned)\n");

		System.out.println("QUERY: \n" + query.toString() + "\n");
		
		ArrayList<Integer> includeColumns = new ArrayList<Integer>();
		includeColumns.add(3);
		return RestResponse.okWithBody(
					DbUtils.selectPartitions_DbToJson("upstreamASN", mysql_ds, query.toString(), 0, 47, includeColumns));
	}
	
}
