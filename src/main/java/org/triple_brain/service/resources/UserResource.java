/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.service.resources;

import com.google.inject.Injector;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.GraphFactory;
import org.triple_brain.module.model.graph.GraphTransactional;
import org.triple_brain.module.model.graph.UserGraph;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.json.UserJson;
import org.triple_brain.module.repository.user.UserRepository;
import org.triple_brain.module.search.GraphIndexer;
import org.triple_brain.service.resources.schema.SchemaNonOwnedResource;
import org.triple_brain.service.resources.schema.SchemaNonOwnedResourceFactory;
import org.triple_brain.service.resources.vertex.VertexNonOwnedSurroundGraphResource;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.triple_brain.module.model.json.UserJson.*;
import static org.triple_brain.module.model.validator.UserValidator.*;
import static org.triple_brain.service.resources.GraphManipulatorResourceUtils.isUserInSession;
import static org.triple_brain.service.resources.GraphManipulatorResourceUtils.userFromSession;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class UserResource {

    @Inject
    UserRepository userRepository;

    @Inject
    GraphIndexer graphIndexer;

    @Inject
    private GraphFactory graphFactory;

    @Inject
    GraphResourceFactory graphResourceFactory;

    @Inject
    SearchResourceFactory searchResourceFactory;

    @Inject
    SchemaNonOwnedResourceFactory schemaNonOwnedResourceFactory;

    @Inject
    private Injector injector;

    @Context
    HttpServletRequest request;

    @Path("{username}/graph")
    public GraphResource graphResource(
            @PathParam("username") String username
    ) {
        if (isUserNameTheOneInSession(username)) {
            return graphResourceFactory.withUser(
                    userFromSession(request.getSession())
            );
        }
        throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    @Path("{username}/non_owned/vertex/{shortId}/surround_graph")
    @GraphTransactional
    public VertexNonOwnedSurroundGraphResource surroundGraphResource(
            @PathParam("username") String username,
            @PathParam("shortId") String shortId
    ) {
        UserGraph userGraph = graphFactory.loadForUser(
                userRepository.findByUsername(username)
        );
        URI centerVertexUri = new UserUris(
                username
        ).vertexUriFromShortId(
                shortId
        );
        if (!userGraph.haveElementWithId(centerVertexUri)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Vertex centerVertex = userGraph.vertexWithUri(
                centerVertexUri
        );
        Boolean skipVerification = false;
        if (isUserInSession(request.getSession())) {
            User userInSession = userFromSession(request.getSession());
            skipVerification = userInSession.username().equals(
                    centerVertex.getOwnerUsername()
            );
        }
        return new VertexNonOwnedSurroundGraphResource(
                userGraph,
                centerVertex,
                skipVerification
        );
    }

    @Path("{username}/non_owned/schema")
    public SchemaNonOwnedResource schemaNonOwnedResource(
            @PathParam("username") String username
    ) {
        return schemaNonOwnedResourceFactory.fromUserGraph(
                graphFactory.loadForUser(
                        userRepository.findByUsername(username)
                )
        );
    }

    @Path("{username}/search")
    public SearchResource searchResource(
            @PathParam("username") String username
    ) {
        if (isUserNameTheOneInSession(username)) {
            return searchResourceFactory.withUser(
                    userFromSession(request.getSession())
            );
        }
        throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    @Path("{username}/admin")
    public AdminResource adminResource(
            @PathParam("username") String username
    ) {
        if (isUserNameTheOneInSession(username) && username.equals("vince")) {
            return injector.getInstance(
                    AdminResource.class
            );
        }
        throw new WebApplicationException(
                Response.Status.FORBIDDEN
        );
    }

    @Path("session")
    public UserSessionResource sessionResource() {
        return injector.getInstance(
                UserSessionResource.class
        );
    }

    @Path("password")
    public UserPasswordResource getPasswordResource() {
        return injector.getInstance(
                UserPasswordResource.class
        );
    }

    @POST
    @Produces(MediaType.WILDCARD)
    @GraphTransactional
    @Path("/")
    public Response createUser(JSONObject jsonUser) {
        User user = User.withEmail(
                jsonUser.optString(EMAIL, "")
        ).password(
                jsonUser.optString(PASSWORD, "")
        );
        JSONArray jsonMessages = new JSONArray();
        Map<String, String> errors = errorsForUserAsJson(jsonUser);
        if (userRepository.emailExists(jsonUser.optString(EMAIL, "")))
            errors.put(EMAIL, ALREADY_REGISTERED_EMAIL);

        if (!errors.isEmpty()) {
            for (Map.Entry<String, String> entry : errors.entrySet()) {
                try {
                    jsonMessages.put(new JSONObject().put(
                            "field", entry.getKey()
                    ).put(
                            "reason", entry.getValue()
                    ));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            throw new WebApplicationException(Response
                    .status(BAD_REQUEST)
                    .entity(jsonMessages)
                    .build()
            );
        }
        user = userRepository.createUser(user);
        graphFactory.createForUser(user);
        UserGraph userGraph = graphFactory.loadForUser(user);
        graphIndexer.indexVertex(
                userGraph.defaultVertex()
        );
        UserSessionResource.authenticateUserInSession(
                user, request.getSession()
        );
        return Response.created(URI.create(
                user.username()
        )).entity(UserJson.toJson(user)).build();
    }

    @GET
    @Path("/is_authenticated")
    public Response isAuthenticated(@Context HttpServletRequest request) throws JSONException {
        return Response.ok(new JSONObject()
                        .put("is_authenticated", isUserInSession(request.getSession()))
        ).build();
    }

    private Boolean isUserNameTheOneInSession(String userName) {
        if (!isUserInSession(request.getSession())) {
            return false;
        }
        User authenticatedUser = userFromSession(request.getSession());
        return authenticatedUser.username().equals(userName);
    }

}
