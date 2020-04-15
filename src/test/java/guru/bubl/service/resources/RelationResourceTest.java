/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.service.resources;

import com.sun.jersey.api.client.ClientResponse;
import guru.bubl.module.common_utils.Uris;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.center_graph_element.CenterGraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.SubGraphJson;
import guru.bubl.module.model.graph.relation.Relation;
import guru.bubl.module.model.graph.group_relation.GroupRelationPojo;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.service.SessionHandler;
import guru.bubl.service.utils.GraphManipulationRestTestUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hamcrest.core.Is;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;

public class RelationResourceTest extends GraphManipulationRestTestUtils {

    @Test
    public void can_add_a_relation() {
        assertFalse(vertexUtils().vertexWithUriHasDestinationVertexWithUri(
                vertexAUri(),
                vertexCUri()
        ));
        edgeUtils().addRelationBetweenVertexAAndC();
        assertTrue(vertexUtils().vertexWithUriHasDestinationVertexWithUri(
                vertexAUri(),
                vertexCUri()
        ));
    }

    @Test
    public void adding_a_relation_returns_correct_response_status() {
        ClientResponse response = edgeUtils().addRelationBetweenVertexAAndC();
        assertThat(
                response.getStatus(), is(
                        Response.Status.CREATED.getStatusCode()
                ));
    }

    @Test
    public void adding_a_relation_returns_correct_headers() {
        ClientResponse response = edgeUtils().addRelationBetweenVertexAAndC();
        Relation relationBetweenAAndC = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexCUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        try {
            assertThat(
                    Uris.decodeUrlSafe(
                            response.getHeaders().get("Location").get(0)
                    ),
                    is(
                            BASE_URI + relationBetweenAAndC.uri().toString()
                    ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void add_relation_involving_patterns_returns_bad_request() {
        vertexUtils().makePattern(vertexAUri());
        assertThat(
                edgeUtils().addRelationBetweenVertexAAndC().getStatus(),
                is(Response.Status.BAD_REQUEST.getStatusCode())
        );
        assertFalse(vertexUtils().vertexWithUriHasDestinationVertexWithUri(
                vertexAUri(),
                vertexCUri()
        ));
    }


    @Test
    public void can_remove_a_relation() {
        Relation relationBetweenAAndB = edgeUtils().edgeBetweenAAndB();
        edgeUtils().removeEdgeBetweenVertexAAndB();
        assertFalse(
                graphUtils().graphWithCenterVertexUri(
                        vertexAUri()
                ).containsEdge(
                        relationBetweenAAndB
                )
        );
    }

    @Test
    public void removing_a_relation_returns_correct_status() {
        ClientResponse response = edgeUtils().removeEdgeBetweenVertexAAndB();
        assertThat(
                response.getStatus(), is(
                        Response.Status.OK.getStatusCode()
                ));
    }

    @Test
    public void can_update_label() {
        Relation relationBetweenAAndB = edgeUtils().edgeBetweenAAndB();
        assertThat(
                relationBetweenAAndB.label(),
                is(not("new edge label"))
        );
        updateEdgeLabelBetweenAAndB("new edge label");
        relationBetweenAAndB = edgeUtils().edgeBetweenAAndB();
        assertThat(
                relationBetweenAAndB.label(),
                is("new edge label")
        );
    }

    @Test
    public void updating_label_returns_correct_status() {
        ClientResponse response = updateEdgeLabelBetweenAAndB("new edge label");
        assertThat(
                response.getStatus(), is(
                        Response.Status.OK.getStatusCode()
                ));
    }

    private ClientResponse updateEdgeLabelBetweenAAndB(String label) {
        return edgeUtils().updateEdgeLabel(
                label,
                edgeUtils().edgeBetweenAAndB()
        );
    }

    @Test
    public void inverseReturnsCorrectStatus() {
        assertThat(
                inverseRelationBetweenAAndB().getStatus(), is(
                        Response.Status.OK.getStatusCode()
                ));
    }

    @Test
    public void can_inverse() {
        Relation relationBetweenAAndB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        assertThat(
                relationBetweenAAndB.sourceUri().toString(), is(
                        vertexAUri().toString()
                ));
        assertThat(
                relationBetweenAAndB.destinationUri().toString(), is(
                        vertexBUri().toString()
                ));
        inverseRelationBetweenAAndB();
        relationBetweenAAndB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexBUri()).edges()
        );
        assertThat(
                relationBetweenAAndB.sourceUri().toString(), is(
                        vertexBUri().toString()
                ));
        assertThat(
                relationBetweenAAndB.destinationUri().toString(), is(
                        vertexAUri().toString()
                ));
    }

    @Test
    public void updating_note_returns_correct_status() {
        Relation relationBetweenAAndB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        ClientResponse response = edgeUtils().updateNoteOfEdge(
                "some note",
                relationBetweenAAndB
        );
        assertThat(
                response.getStatus(),
                is(Response.Status.NO_CONTENT.getStatusCode())
        );
    }

    @Test
    public void can_update_note() {
        Relation relationBetweenAAndB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        assertThat(
                relationBetweenAAndB.comment(),
                is(
                        not("some note")
                )
        );
        edgeUtils().updateNoteOfEdge("some note", relationBetweenAAndB);
        relationBetweenAAndB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        assertThat(
                relationBetweenAAndB.comment(),
                is(
                        "some note"
                )
        );
    }

    @Test
    public void changing_source_vertex_returns_correct_status() {
        Relation relationBC = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexBUri(),
                vertexCUri(),
                graphUtils().graphWithCenterVertexUri(vertexBUri()).edges()
        );
        ClientResponse response = changeSource(
                relationBC,
                vertexA().uri(),
                GraphElementType.Vertex,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE
        );
        assertThat(
                response.getStatus(),
                is(
                        Response.Status.NO_CONTENT.getStatusCode()
                )
        );
    }

    @Test
    public void can_change_source_vertex() {
        Relation relation = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexBUri(),
                vertexCUri(),
                graphUtils().graphWithCenterVertexUri(vertexBUri()).edges()

        );
        SubGraph graph = graphUtils().graphWithCenterVertexUri(
                vertexAUri()
        );
        assertFalse(
                graph.containsEdge(relation)
        );
        changeSource(
                relation,
                vertexA().uri(),
                GraphElementType.Vertex,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE
        );
        graph = graphUtils().graphWithCenterVertexUri(
                vertexAUri()
        );
        assertTrue(
                graph.containsEdge(relation)
        );
    }

    @Test
    public void can_change_source_fork_to_a_group_relation() {
        Relation relationBC = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexBUri(),
                vertexCUri(),
                graphUtils().graphWithCenterVertexUri(vertexBUri()).edges()
        );
        SubGraphPojo graph = graphUtils().graphWithCenterVertexUri(
                graphUtils().getTodoGroupRelation().uri()
        );
        assertFalse(
                graph.containsEdge(relationBC)
        );
        changeSource(
                relationBC,
                graphUtils().getTodoGroupRelation().uri(),
                GraphElementType.GroupRelation,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE
        );
        graph = graphUtils().graphWithCenterVertexUri(
                graphUtils().getTodoGroupRelation().uri()
        );
        assertTrue(
                graph.containsEdge(relationBC)
        );
    }


    @Test
    public void changing_destination_vertex_returns_correct_status() {
        Relation relationBetweenAAndB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        ClientResponse response = changeDestination(
                relationBetweenAAndB,
                vertexC().uri(),
                GraphElementType.Vertex,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE
        );
        assertThat(
                response.getStatus(),
                is(
                        Response.Status.NO_CONTENT.getStatusCode()
                )
        );
    }

    @Test
    public void can_change_destination_vertex() {
        Relation relation = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()

        );
        SubGraph graph = graphUtils().graphWithCenterVertexUri(
                vertexCUri()
        );
        assertFalse(
                graph.containsEdge(relation)
        );
        changeDestination(
                relation,
                vertexC().uri(),
                GraphElementType.Vertex,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE,
                ShareLevel.PRIVATE
        );
        graph = graphUtils().graphWithCenterVertexUri(
                vertexCUri()
        );
        assertTrue(
                graph.containsEdge(relation)
        );
    }

    @Test
    public void getting_surround_graph_returns_ok_status() {
        assertThat(
                getSurroundGraphOfEdgeBetweenAAndB().getStatus(),
                is(Response.Status.OK.getStatusCode())
        );
    }

    @Test
    public void getting_surround_graph_of_edge_returns_surround_graph_of_source_vertex() {
        SubGraph subGraph = SubGraphJson.fromJson(
                getSurroundGraphOfEdgeBetweenAAndB().getEntity(
                        JSONObject.class
                )
        );
        assertTrue(
                subGraph.containsVertex(vertexA())
        );
        assertTrue(
                subGraph.containsVertex(vertexB())
        );
        assertFalse(
                subGraph.containsVertex(vertexC())
        );
    }

    @Test
    public void updates_last_visit_date_when_getting_surround_graph() {
        graphUtils().graphWithCenterVertexUri(vertexAUri());
        List<CenterGraphElementPojo> centerElements = graphUtils().getCenterGraphElements();
        Integer numberOfVisitedElements = centerElements.size();
        getSurroundGraphOfEdgeBetweenAAndB();
        centerElements = graphUtils().getCenterGraphElements();
        assertThat(
                centerElements.size(),
                Is.is(numberOfVisitedElements + 1)
        );
    }

    @Test
    public void increments_number_of_visits_when_getting_surround_graph() {
        Relation relation = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()

        );
        getSurroundGraphOfEdgeBetweenAAndB();
        CenterGraphElementPojo centerEdge = graphUtils().getCenterWithUri(
                graphUtils().getCenterGraphElements(),
                relation.uri()
        );
        assertThat(
                centerEdge.getNumberOfVisits(),
                is(1)
        );
        getSurroundGraphOfEdgeBetweenAAndB();
        centerEdge = graphUtils().getCenterWithUri(
                graphUtils().getCenterGraphElements(),
                relation.uri()
        );
        assertThat(
                centerEdge.getNumberOfVisits(),
                is(2)
        );
    }

    @Test
    public void convert_to_group_relation_returns_ok_status() {
        Relation relationAB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        ClientResponse response = convertToGroupRelation(
                relationAB.uri(),
                UUID.randomUUID().toString(),
                ShareLevel.PRIVATE
        );
        assertThat(
                response.getStatus(),
                is(Response.Status.OK.getStatusCode())
        );
    }

    @Test
    public void convert_to_group_relation_returns_group_relation_json_object() {
        Relation relationAB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        graphElementUtils().addTagToGraphElementWithUri(modelTestScenarios.toDo(), relationAB.uri());
        String newGroupRelationShortId = UUID.randomUUID().toString();
        ClientResponse response = convertToGroupRelation(
                relationAB.uri(),
                newGroupRelationShortId,
                ShareLevel.PRIVATE
        );
        GroupRelationPojo newGroupRelation = JsonUtils.getGson().fromJson(
                response.getEntity(String.class),
                GroupRelationPojo.class
        );
        assertThat(
                newGroupRelation.uri(),
                is(new UserUris(defaultAuthenticatedUser.username()).groupRelationUriFromShortId(newGroupRelationShortId))
        );
    }

    private ClientResponse convertToGroupRelation(URI edgeUri, String newGroupRelationShortId, ShareLevel initialShareLevel) {
        try {
            return resource
                    .path(edgeUri.toString())
                    .path("convertToGroupRelation")
                    .cookie(authCookie)
                    .header(SessionHandler.X_XSRF_TOKEN, currentXsrfToken)
                    .post(
                            ClientResponse.class, new JSONObject().put(
                                    "newGroupRelationShortId", newGroupRelationShortId
                            ).put(
                                    "initialShareLevel", initialShareLevel.name().toUpperCase()
                            )
                    );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private ClientResponse getSurroundGraphOfEdgeBetweenAAndB() {
        Relation relationBetweenAAndB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        return resource
                .path(relationBetweenAAndB.uri().toString())
                .path("surround_graph")
                .queryParam("center", "true")
                .cookie(authCookie)
                .header(SessionHandler.X_XSRF_TOKEN, currentXsrfToken)
                .get(ClientResponse.class);
    }


    private ClientResponse inverseRelationBetweenAAndB() {
        Relation relationBetweenAAndB = edgeUtils().edgeBetweenTwoVerticesUriGivenEdges(
                vertexAUri(),
                vertexBUri(),
                graphUtils().graphWithCenterVertexUri(vertexAUri()).edges()
        );
        return resource
                .path(relationBetweenAAndB.uri().toString())
                .path("inverse")
                .cookie(authCookie)
                .header(SessionHandler.X_XSRF_TOKEN, currentXsrfToken)
                .put(ClientResponse.class);
    }

    private ClientResponse changeSource(Relation relation, URI newSourceUri, GraphElementType sourceType, ShareLevel oldEndShareLevel, ShareLevel keptEndShareLevel, ShareLevel newEndShareLevel) {
        try {
            return resource
                    .path(relation.uri().toString())
                    .path("source")
                    .path(UserUris.graphElementShortId(newSourceUri))
                    .cookie(authCookie)
                    .header(SessionHandler.X_XSRF_TOKEN, currentXsrfToken)
                    .put(ClientResponse.class, new JSONObject().put(
                            "forkType",
                            sourceType.name()
                    ).put(
                            "oldEndShareLevel", oldEndShareLevel.name().toUpperCase()
                    ).put(
                            "keptEndShareLevel", keptEndShareLevel.name().toUpperCase()
                    ).put(
                            "newEndShareLevel", newEndShareLevel.name().toUpperCase()
                    ));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private ClientResponse changeDestination(Relation relation, URI destinationUri, GraphElementType destinationType, ShareLevel oldEndShareLevel, ShareLevel keptEndShareLevel, ShareLevel newEndShareLevel) {
        try {
            return resource
                    .path(relation.uri().toString())
                    .path("destination")
                    .path(UserUris.graphElementShortId(destinationUri))
                    .cookie(authCookie)
                    .header(SessionHandler.X_XSRF_TOKEN, currentXsrfToken)
                    .put(ClientResponse.class, new JSONObject().put(
                            "forkType",
                            destinationType
                    ).put(
                            "oldEndShareLevel", oldEndShareLevel.name().toUpperCase()
                    ).put(
                            "keptEndShareLevel", keptEndShareLevel.name().toUpperCase()
                    ).put(
                            "newEndShareLevel", newEndShareLevel.name().toUpperCase()
                    ));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}