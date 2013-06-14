package org.triple_brain.service.resources;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.User;
import org.triple_brain.module.search.GraphSearch;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;

/*
* Copyright Mozilla Public License 1.1
*/
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.TEXT_PLAIN)
public class SearchResource {

    @Inject
    GraphSearch graphSearch;

    private User user;

    @AssistedInject
    public SearchResource(
        @Assisted User user
    ){
        this.user = user;
    }

    @GET
    @Path("vertices/auto_complete/{search_text}")
    public Response searchVerticesForAutoComplete(
            @PathParam("search_text") String searchText
    ){
        try{
            return Response.ok(
                    graphSearch.searchVerticesForAutoCompletionByLabelAndUser(
                    Uris.decodeURL(searchText),
                    user
            )).build();
        }catch(UnsupportedEncodingException e){
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
}
