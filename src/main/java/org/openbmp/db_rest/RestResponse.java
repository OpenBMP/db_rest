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
		return Response.status(200).entity(body)
				.header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Methods", "GET")
				.allow("OPTIONS")
				.build();
	}
}
