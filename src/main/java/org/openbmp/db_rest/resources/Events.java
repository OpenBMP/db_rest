/*
 * Copyright (c) 2014-2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.resources;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.openbmp.db_rest.DbUtils;
import org.openbmp.db_rest.RestResponse;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/events")
public class Events {
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
    public Response getAllEvents(@QueryParam("limit") Integer limit,
                                 @QueryParam("start") Long start,
                                 @QueryParam("end") Long end) {

        StringBuilder query = new StringBuilder();

        query.append(
                "select count(*) as peers, period_ts,max(last_ts) as last_ts,\n" +
                        "      concat(inet6_ntoa(prefix_bin), '/', prefix_len) as prefix,\n" +
                        "      watcher_rule_name as rule_name,origin_as,prev_origin_as,\n" +
                        "      wNew.org_name as CurrentOrg, wOld.org_name as PreviousOrg,wAgg.org_name as AggregateOrg,\n" +
                        "      if (aggregate_prefix_bin is not null,\n" +
                        "          concat(inet6_ntoa(aggregate_prefix_bin), '/', aggregate_prefix_len), '') as aggregate,\n" +
                        "      aggregate_origin_as\n" +
                        "  from watcher_prefix_log\n" +
                        "    LEFT JOIN gen_whois_asn wNew ON (wNew.asn = origin_as)\n" +
                        "    LEFT JOIN gen_whois_asn wOld ON (wOld.asn = prev_origin_as)\n" +
                        "    LEFT JOIN gen_whois_asn wAgg ON (wAgg.asn = aggregate_origin_as)\n");

        if (start != null && end != null) {
            query.append("  WHERE period_ts >= from_unixtime(" + start + ")\n" +
                         "      AND period_ts <= from_unixtime(" + end + ")\n");
        } else {
            query.append("  WHERE period_ts >= date_sub(current_timestamp, interval 4 hour)\n");
        }

        query.append("  group by watcher_rule_num,prefix_bin,prefix_len,period_ts\n" +
                     "  order by max(last_ts) desc ");

        String limit_st = " limit 1000";
        String where_st = "";
        String orderby_st = "";

        // Set the limit for the query
        if (limit != null && limit < 800000)
            limit_st = " limit " + limit;
        else if (limit != null && limit == 0)
            limit_st = "";

        query.append(where_st);
        query.append(orderby_st);
        query.append(limit_st);

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(DbUtils.select_DbToJson(mysql_ds, "events", query.toString()), 5000);
    }

    @GET
    @Path("/count")
    @Produces("application/json")
    public Response getStatCounts(@QueryParam("start") Long start,
                                  @QueryParam("end") Long end) {

        StringBuilder query = new StringBuilder();

        query.append("select watcher_rule_name as name,count(distinct watcher_rule_num,prefix_bin,prefix_len,period_ts) as value\n" +
                     "  from watcher_prefix_log\n" +
                     "    LEFT JOIN gen_whois_asn wNew ON (wNew.asn = origin_as)\n" +
                     "    LEFT JOIN gen_whois_asn wOld ON (wOld.asn = prev_origin_as)\n");

        if (start != null && end != null) {
            query.append("  WHERE period_ts >= from_unixtime(" + start + ")\n" +
                    "      AND period_ts <= from_unixtime(" + end + ")\n");
        } else {
            query.append("  WHERE period_ts >= date_sub(current_timestamp, interval 4 hour)\n");
        }

        query.append("       AND (watcher_rule_num != 1 OR (wOld.org_name is null or wNew.org_name != wOld.org_name))\n" +
                     "   group by watcher_rule_num\n");
        query.append("   order by watcher_rule_num");

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(DbUtils.select_DbToJson(mysql_ds, "events", query.toString()));
    }

	@GET
    @Path("/origin_mismatch")
	@Produces("application/json")
	public Response getOriginMismatch(@QueryParam("limit") Integer limit,
                                      @QueryParam("start") Long start,
                                      @QueryParam("end") Long end) {

		StringBuilder query = new StringBuilder();

		query.append(
				"select count(*) as peers, period_ts,max(last_ts) as last_ts,\n" +
				"      concat(inet6_ntoa(prefix_bin), '/', prefix_len) as prefix,\n" +
				"      watcher_rule_name as rule_name,origin_as,prev_origin_as,\n" +
				"      wNew.org_name as CurrentOrg, wOld.org_name as PreviousOrg" +
				"  from watcher_prefix_log\n" +
				"    LEFT JOIN gen_whois_asn wNew ON (wNew.asn = origin_as)\n" +
				"    LEFT JOIN gen_whois_asn wOld ON (wOld.asn = prev_origin_as)\n");

        if (start != null && end != null) {
            query.append("  WHERE period_ts >= from_unixtime(" + start + ")\n" +
                    "      AND period_ts <= from_unixtime(" + end + ")\n");
        } else {
            query.append("  WHERE period_ts >= date_sub(current_timestamp, interval 4 hour)\n");
        }

        query.append("  AND watcher_rule_num = 1\n" +
                    "       AND (wOld.org_name is null or wNew.org_name != wOld.org_name)\n" +
				    "  group by watcher_rule_num,prefix_bin,prefix_len,period_ts\n" +
				    "  order by max(last_ts) desc ");

		String limit_st = " limit 1000";
		String where_st = "";
		String orderby_st = "";

		// Set the limit for the query
		if (limit != null && limit < 800000)
			limit_st = " limit " + limit;
        else if (limit != null && limit == 0)
            limit_st = "";

		query.append(where_st);
		query.append(orderby_st);
		query.append(limit_st);

		System.out.println("QUERY: \n" + query.toString() + "\n");

		return RestResponse.okWithBody(DbUtils.select_DbToJson(mysql_ds, "events", query.toString()));
	}

    @GET
    @Path("/prefix_loss")
    @Produces("application/json")
    public Response getPrefixLoss(@QueryParam("limit") Integer limit,
                                  @QueryParam("start") Long start,
                                  @QueryParam("end") Long end) {

        StringBuilder query = new StringBuilder();

        query.append(
                "select count(*) as peers, period_ts,max(last_ts) as last_ts,\n" +
                        "      concat(inet6_ntoa(prefix_bin), '/', prefix_len) as prefix,\n" +
                        "      watcher_rule_name as rule_name,origin_as,prev_origin_as,\n" +
                        "      wNew.org_name as CurrentOrg\n" +
                        "  from watcher_prefix_log\n" +
                        "    LEFT JOIN gen_whois_asn wNew ON (wNew.asn = origin_as)\n");

        if (start != null && end != null) {
            query.append("  WHERE period_ts >= from_unixtime(" + start + ")\n" +
                    "      AND period_ts <= from_unixtime(" + end + ")\n");
        } else {
            query.append("  WHERE period_ts >= date_sub(current_timestamp, interval 4 hour)\n");
        }

        query.append("  AND watcher_rule_num in (4,5) \n" +
                     "  group by watcher_rule_num,prefix_bin,prefix_len,period_ts\n" +
                     "  order by max(last_ts) desc ");

        String limit_st = " limit 1000";
        String where_st = "";
        String orderby_st = "";

        // Set the limit for the query
        if (limit != null && limit < 800000)
            limit_st = " limit " + limit;
        else if (limit != null && limit == 0)
            limit_st = "";


        query.append(where_st);
        query.append(orderby_st);
        query.append(limit_st);

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(DbUtils.select_DbToJson(mysql_ds, "events", query.toString()));
    }

    @GET
    @Path("/aggregate_mismatch")
    @Produces("application/json")
    public Response getAggregateMismatch(@QueryParam("new") Boolean new_prefix,
                                         @QueryParam("limit") Integer limit,
                                         @QueryParam("start") Long start,
                                         @QueryParam("end") Long end) {

        StringBuilder query = new StringBuilder();

        query.append(
                "select count(*) as peers, period_ts,max(last_ts) as last_ts,\n" +
                        "      concat(inet6_ntoa(prefix_bin), '/', prefix_len) as prefix,\n" +
                        "      watcher_rule_name as rule_name,origin_as,prev_origin_as,\n" +
                        "      wNew.org_name as CurrentOrg, wOld.org_name as PreviousOrg,wAgg.org_name as AggregateOrg,\n" +
                        "      if (aggregate_prefix_bin is not null,\n" +
                        "          concat(inet6_ntoa(aggregate_prefix_bin), '/', aggregate_prefix_len), '') as aggregate,\n" +
                        "      aggregate_origin_as\n" +
                        "  from watcher_prefix_log\n" +
                        "    LEFT JOIN gen_whois_asn wNew ON (wNew.asn = origin_as)\n" +
                        "    LEFT JOIN gen_whois_asn wOld ON (wOld.asn = prev_origin_as)\n" +
                        "    LEFT JOIN gen_whois_asn wAgg ON (wAgg.asn = aggregate_origin_as)\n" +
                        "WHERE ");

        if (new_prefix == null || new_prefix == false) {
            query.append(" watcher_rule_num = 2 \n");

        } else {
            query.append("  watcher_rule_num = 3 \n");
        }

        if (start != null && end != null) {
            query.append("  AND period_ts >= from_unixtime(" + start + ")\n" +
                    "      AND period_ts <= from_unixtime(" + end + ")\n");
        } else {
            query.append("  AND period_ts >= date_sub(current_timestamp, interval 4 hour)\n");
        }
        query.append("  group by watcher_rule_num,prefix_bin,prefix_len,period_ts\n" +
                     "  order by max(last_ts) desc ");

        String limit_st = " limit 1000";
        String where_st = "";
        String orderby_st = "";

        // Set the limit for the query
        if (limit != null && limit < 800000)
            limit_st = " limit " + limit;
        else if (limit != null && limit == 0)
            limit_st = "";

        query.append(where_st);
        query.append(orderby_st);
        query.append(limit_st);

        System.out.println("QUERY: \n" + query.toString() + "\n");

        return RestResponse.okWithBody(DbUtils.select_DbToJson(mysql_ds, "events", query.toString()));
    }


}
