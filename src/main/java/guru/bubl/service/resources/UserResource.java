/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.service.resources;

import com.google.inject.Injector;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.GraphTransactional;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.json.UserJson;
import guru.bubl.module.model.search.GraphIndexer;
import guru.bubl.module.repository.user.UserRepository;
import guru.bubl.service.resources.center.CenterGraphElementsResource;
import guru.bubl.service.resources.center.CenterGraphElementsResourceFactory;
import guru.bubl.service.resources.center.PublicCenterGraphElementsResource;
import guru.bubl.service.resources.center.PublicCenterGraphElementsResourceFactory;
import guru.bubl.service.resources.fork.ForkResource;
import guru.bubl.service.resources.fork.ForkResourceFactory;
import guru.bubl.service.resources.identification.IdentifiedToResource;
import guru.bubl.service.resources.identification.IdentifiedToResourceFactory;
import guru.bubl.service.resources.schema.SchemaNonOwnedResource;
import guru.bubl.service.resources.schema.SchemaNonOwnedResourceFactory;
import guru.bubl.service.resources.vertex.VertexNonOwnedSurroundGraphResource;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

import static guru.bubl.module.model.json.UserJson.EMAIL;
import static guru.bubl.module.model.json.UserJson.USER_NAME;
import static guru.bubl.module.model.json.UserJson.PASSWORD;
import static guru.bubl.module.model.validator.UserValidator.ALREADY_REGISTERED_EMAIL;
import static guru.bubl.module.model.validator.UserValidator.USER_NAME_ALREADY_REGISTERED;
import static guru.bubl.module.model.validator.UserValidator.errorsForUserAsJson;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

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
    IdentifiedToResourceFactory identifiedToResourceFactory;

    @Inject
    SchemaNonOwnedResourceFactory schemaNonOwnedResourceFactory;

    @Inject
    CenterGraphElementsResourceFactory centerGraphElementsResourceFactory;

    @Inject
    PublicCenterGraphElementsResourceFactory publicCenterGraphElementsResourceFactory;

    @Inject
    private Injector injector;

    @Inject
    ForkResourceFactory forkResourceFactory;

    @Context
    HttpServletRequest request;

    @Path("{username}/graph")
    public GraphResource graphResource(
            @PathParam("username") String username
    ) {
        if (isUserNameTheOneInSession(username)) {
            return graphResourceFactory.withUser(
                    GraphManipulatorResourceUtils.userFromSession(request.getSession())
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
        if (GraphManipulatorResourceUtils.isUserInSession(request.getSession())) {
            User userInSession = GraphManipulatorResourceUtils.userFromSession(request.getSession());
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
                    GraphManipulatorResourceUtils.userFromSession(request.getSession())
            );
        }
        throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    @Path("{username}/identification")
    public IdentifiedToResource identificationResource(
            @PathParam("username") String username
    ) {
        if (!isUserNameTheOneInSession(username)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return identifiedToResourceFactory.forAuthenticatedUser(
                GraphManipulatorResourceUtils.userFromSession(
                        request.getSession()
                )
        );
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

    @Path("{username}/center-elements")
    public CenterGraphElementsResource getCenterGraphElementsResource(
            @PathParam("username") String username
    ) {
        if (!isUserNameTheOneInSession(username)) {
            throw new WebApplicationException(
                    Response.Status.FORBIDDEN
            );
        }
        return centerGraphElementsResourceFactory.forUser(
                GraphManipulatorResourceUtils.userFromSession(request.getSession())
        );
    }

    @Path("{username}/fork")
    public ForkResource getForkResource(
            @PathParam("username") String username
    ) {
        if (!isUserNameTheOneInSession(username)) {
            throw new WebApplicationException(
                    Response.Status.FORBIDDEN
            );
        }
        return forkResourceFactory.forUser(
                GraphManipulatorResourceUtils.userFromSession(request.getSession())
        );
    }

    @Path("{username}/center-elements/public")
    public PublicCenterGraphElementsResource getPublicCenterGraphElementsResource(
            @PathParam("username") String username
    ) {
        return publicCenterGraphElementsResourceFactory.forUser(
                User.withUsername(username)
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
        User user = User.withEmailAndUsername(
                jsonUser.optString(EMAIL, ""),
                jsonUser.optString(USER_NAME, "")
        ).password(
                jsonUser.optString(PASSWORD, "")
        );
        JSONArray jsonMessages = new JSONArray();
        Map<String, String> errors = errorsForUserAsJson(jsonUser);
        if (errors.isEmpty()) {
            if (userRepository.emailExists(user.email())) {
                errors.put(EMAIL, ALREADY_REGISTERED_EMAIL);
            }
            if (userRepository.usernameExists(user.username())) {
                errors.put(USER_NAME, USER_NAME_ALREADY_REGISTERED);
            }
        }
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
                .put("is_authenticated", GraphManipulatorResourceUtils.isUserInSession(request.getSession()))
        ).build();
    }

    private Boolean isUserNameTheOneInSession(String userName) {
        if (!GraphManipulatorResourceUtils.isUserInSession(request.getSession())) {
            return false;
        }
        User authenticatedUser = GraphManipulatorResourceUtils.userFromSession(request.getSession());
        return authenticatedUser.username().equals(userName);
    }

}
