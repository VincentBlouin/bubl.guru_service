/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.service.resources.test;

import guru.bubl.module.common_utils.Uris;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.relation.RelationJson;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.relation.RelationPojo;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexJson;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.service.SessionHandler;
import org.codehaus.jettison.json.JSONArray;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/test/vertex/")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class VertexResourceTestUtils {

    @Inject
    private GraphFactory graphFactory;

    @Inject
    private SessionHandler sessionHandler;

    @Inject
    private VertexFactory vertexFactory;

    @Path("{vertexId}")
    @GET
    public Response vertexWithId(@Context HttpServletRequest request, @PathParam("vertexId") String vertexId) throws Exception {
        UserGraph userGraph = graphFactory.loadForUser(
                sessionHandler.userFromSession(request.getSession())
        );
        URI vertexUri = new URI(vertexId);
        SubGraphPojo subGraph = userGraph.aroundForkUriInShareLevels(
                vertexUri,
                ShareLevel.allShareLevelsInt
        );
        return Response.ok(VertexJson.toJson(
                subGraph.vertexWithIdentifier(
                        vertexUri
                )
        )).build();
    }

    @Path("{vertexId}/connected_edges")
    @GET
    public Response connectedEdges(@Context HttpServletRequest request, @PathParam("vertexId") String vertexId) throws Exception {
        VertexOperator vertex = vertexFactory.withUri(new URI(vertexId));
        JSONArray edges = new JSONArray();
        for (RelationOperator edge : vertex.connectedEdges().values()) {
            edges.put(
                    RelationJson.toJson(
                            new RelationPojo(edge)
                    )
            );
        }
        return Response.ok(edges).build();
    }

    @Path("{vertexId}/has_destination/{otherVertexId}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public Response destinationVertices(@Context HttpServletRequest request, @PathParam("vertexId") String vertexId, @PathParam("otherVertexId") String otherVertexId) throws Exception {
        VertexOperator vertex = vertexFactory.withUri(new URI(Uris.decodeUrlSafe(vertexId)));
        Vertex otherVertex = vertexFactory.withUri(new URI(Uris.decodeUrlSafe(otherVertexId)));
        return Response.ok(
                vertex.hasDestinationVertex(otherVertex).toString()
        ).build();
    }
}
