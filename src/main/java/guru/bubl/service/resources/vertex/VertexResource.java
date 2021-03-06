/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.service.resources.vertex;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperator;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperatorFactory;
import guru.bubl.module.model.graph.fork.ForkOperator;
import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexJson;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.service.resources.GraphElementTagResource;
import guru.bubl.service.resources.fork.ForkResource;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VertexResource implements ForkResource {

    @Inject
    GraphElementTagResourceFactory graphElementTagResourceFactory;

    @Inject
    CenterGraphElementOperatorFactory centerGraphElementOperatorFactory;

    @Inject
    VertexFactory vertexFactory;

    private UserGraph userGraph;

    @AssistedInject
    public VertexResource(
            @Assisted UserGraph userGraph
    ) {
        this.userGraph = userGraph;
    }

    @POST
    @Path("/")
    public Response createVertex() {
        VertexPojo newVertex = userGraph.createVertex();
        CenterGraphElementOperator centerGraphElementOperator = centerGraphElementOperatorFactory.usingFriendlyResource(
                newVertex
        );
        centerGraphElementOperator.incrementNumberOfVisits();
        centerGraphElementOperator.updateLastCenterDate();
        return Response.ok()
                .entity(VertexJson.toJson(
                        newVertex
                ))
                .build();
    }

    @POST
    @Path("/{shortId}/pattern")
    public Response setAsPattern(@PathParam("shortId") String shortId) {
        VertexOperator vertex = vertexFactory.withUri(
                getUriFromShortId(shortId)
        );
        if (!vertex.makePattern()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{shortId}/pattern")
    public Response undoPattern(@PathParam("shortId") String shortId) {
        VertexOperator vertex = vertexFactory.withUri(
                getUriFromShortId(shortId)
        );
        vertex.undoPattern();
        return Response.noContent().build();
    }


//    @Path("{shortId}/image")
//    public VertexImageResource image(
//            @PathParam("shortId") String shortId
//    ) {
//        return vertexImageResourceFactory.ofVertex(
//                vertexFromShortId(shortId)
//        );
//    }

    @Path("{shortId}/identification")
    public GraphElementTagResource getVertexIdentificationResource(
            @PathParam("shortId") String shortId) {
        return graphElementTagResourceFactory.forGraphElement(
                vertexFromShortId(shortId)
        );
    }

    @POST
    @Path("{shortId}/mergeTo/{destinationShortId}")
    public Response mergeTo(
            @PathParam("shortId") String shortId,
            @PathParam("destinationShortId") String destinationShortId
    ) {
        URI destinationVertexUri = getUriFromShortId(destinationShortId);
        if (!userGraph.haveElementWithId(destinationVertexUri)) {
            return Response.status(
                    Response.Status.BAD_REQUEST
            ).build();
        }
        Boolean success = vertexFactory.withUri(getUriFromShortId(shortId)).mergeTo(
                vertexFactory.withUri(destinationVertexUri)
        );
        if (!success) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return Response.noContent().build();
    }

    private VertexOperator vertexFromShortId(String shortId) {
        return vertexFactory.withUri(
                getUriFromShortId(shortId)
        );
    }

    @Override
    public URI getUriFromShortId(String shortId) {
        return new UserUris(
                userGraph.user()
        ).vertexUriFromShortId(
                shortId
        );
    }

    @Override
    public ForkOperator getForkOperatorFromURI(URI uri) {
        return vertexFactory.withUri(uri);
    }

    @Override
    public GraphElementOperator getOperatorFromShortId(String shortId) {
        return vertexFactory.withUri(
                getUriFromShortId(shortId)
        );
    }

    @Override
    public UserGraph getUserGraph() {
        return userGraph;
    }

    @Override
    public CenterGraphElementOperatorFactory getCenterOperatorFactory() {
        return centerGraphElementOperatorFactory;
    }
}
