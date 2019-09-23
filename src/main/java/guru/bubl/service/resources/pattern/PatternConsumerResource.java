package guru.bubl.service.resources.pattern;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.pattern.PatternUserFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatternConsumerResource {

    @Inject
    PatternUserFactory patternUserFactory;

    private User user;

    @AssistedInject
    public PatternConsumerResource(
            @Assisted User user
    ) {
        this.user = user;
    }

    @POST
    @Path("/users/{ownerUsername}/graph/{type}/{shortId}")
    public Response consume(
            @PathParam("ownerUsername") String ownerUsername,
            @PathParam("type") String type,
            @PathParam("shortId") String shortId
    ) {
        UserUris userUris = new UserUris(
                ownerUsername
        );
        URI newCenterUri = patternUserFactory.forUserAndPatternUri(
                user,
                userUris.uriFromTypeStringAndShortId(
                        type, shortId
                )
        ).use();
        return Response.created(newCenterUri).build();
    }

}
