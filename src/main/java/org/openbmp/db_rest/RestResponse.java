/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest;

import javax.ws.rs.core.Response;

public class RestResponse {
	
	/**
	 * Generate REST response including a [json] body
	 * 
	 * @param  body			Formatted body of the message (normally json)
	 * 
	 * @return the formatted rest response	
	 */
	public static Response okWithBody(String body) {
	    return okWithBody(body, null);
	}

    /**
     * Generate REST response including a [json] body
     *
     * @param  body			Formatted body of the message (normally json)
     * @param  total_count  Adds x-total-count header to support paging.
     *                      Total should be total available.
     *
     * @return the formatted rest response
     */
    public static Response okWithBody(String body, Integer total_count) {
        Response.ResponseBuilder resp = Response.status(200).entity(body)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Credentials ", "true")
                .header("Access-Control-Allow-Methods", "GET, POST")
                .allow("OPTIONS");

        if (total_count != null) {
            resp.header("x-total-count", total_count.toString());
            resp.header("Access-Control-Expose-Headers", "X-Total-Count, Link");
        }

        return resp.build();
    }
}
