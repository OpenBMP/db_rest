package org.openbmp.db_rest.resources;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.openbmp.db_rest.RestResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Path("/dns")
public class DNS {

    @GET
    @Produces("application/json")
    public Response getDNS(@QueryParam("hostname") String hostname) {
        try {
            InetAddress[] inetAddress = InetAddress.getAllByName(hostname);

            JsonFactory jfac = new JsonFactory();
            StringWriter stringWriter = new StringWriter();
            try {
                JsonGenerator jgen = jfac.createJsonGenerator(stringWriter);
                jgen.writeStartObject(); //root object
                jgen.writeArrayFieldStart("data");
                jgen.writeStartObject(); // hostname array object
                int i = 0;
                for (InetAddress host : inetAddress) {
                    jgen.writeFieldName("IPAddr" + i);
                    jgen.writeString(host.getHostAddress());
                    i++;
                }
                jgen.writeEndObject(); // end of array object
                jgen.writeEndArray();
                jgen.writeEndObject(); // enf of root
                jgen.close();
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return RestResponse.okWithBody(stringWriter.toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return Response.status(400).entity("")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET")
                    .allow("OPTIONS")
                    .build();
        }
    }
}
