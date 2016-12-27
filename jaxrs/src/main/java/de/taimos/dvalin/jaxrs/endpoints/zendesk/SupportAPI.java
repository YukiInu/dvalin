/**
 *
 */
package de.taimos.dvalin.jaxrs.endpoints.zendesk;

import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.taimos.dvalin.jaxrs.MapperFactory;
import de.taimos.httputils.HTTPRequest;
import de.taimos.httputils.WS;
import de.taimos.restutils.RESTAssert;

/**
 * Copyright 2016 Taimos GmbH<br>
 * <br>
 *
 * @author thoeger
 *
 */
@Consumes(MediaType.APPLICATION_JSON)
public abstract class SupportAPI<T extends ITicketRS> {
    
    @Value("${zendesk.subdomain}")
    private String subdomain;
    @Value("${zendesk.agent}")
    private String agentMail;
    @Value("${zendesk.token}")
    private String agentToken;
    
    @POST
    public Response createTicket(T ticket) {
		RESTAssert.assertNotEmpty(ticket.getRequesterMail());
		RESTAssert.assertNotEmpty(ticket.getRequesterMail());
		RESTAssert.assertNotEmpty(ticket.getSubject());
		RESTAssert.assertNotEmpty(ticket.getBody());
		
		Ticket tick = new Ticket();
		tick.setRequesterName(ticket.getRequesterName());
		tick.setRequesterEMail(ticket.getRequesterMail());
		tick.setSubject(ticket.getSubject());
		tick.setComment(ticket.getBody());
		
		this.customConversion(tick, ticket);
		
		this.createTicket(tick);
		
		return Response.accepted().build();
	}
    
    protected void customConversion(Ticket tick, T ticket) {
        //
    }
    
    private String createTicket(Ticket ticket) {
        HTTPRequest req = WS.url("https://" + this.subdomain + ".zendesk.com/api/v2/tickets.json").authBasic(this.agentMail + "/token", this.agentToken);
        try {
            String json = MapperFactory.createDefault().writeValueAsString(ticket.toJsonMap());
            req.contentType(MediaType.APPLICATION_JSON).body(json);
            HttpResponse post = req.post();
            if (WS.getStatus(post) != 201) {
                throw new InternalServerErrorException();
            }
            return WS.getResponseAsString(post);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
	
}