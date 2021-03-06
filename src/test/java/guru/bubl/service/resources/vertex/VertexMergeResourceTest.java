/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.service.resources.vertex;

import com.sun.jersey.api.client.ClientResponse;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.service.SessionHandler;
import guru.bubl.service.utils.GraphManipulationRestTestUtils;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class VertexMergeResourceTest extends GraphManipulationRestTestUtils {

    @Test
    public void returns_no_content_status() {
        Vertex farVertex = vertexUtils().createSingleVertex();
        ClientResponse response = mergeTo(farVertex, vertexC());
        assertThat(
                response.getStatus(),
                is(Response.Status.NO_CONTENT.getStatusCode())
        );
    }

    @Test
    public void mergeTo_merges() {
        Vertex farVertex = vertexUtils().createSingleVertex();
        vertexUtils().addAVertexToForkWithUri(farVertex.uri());
        vertexUtils().addAVertexToForkWithUri(farVertex.uri());
        assertThat(
                vertexC().getNbNeighbors().getTotal(),
                is(2)
        );
        farVertex = vertexUtils().vertexWithUriOfAnyUser(farVertex.uri());
        assertThat(
                farVertex.getNbNeighbors().getTotal(),
                is(2)
        );
        mergeTo(farVertex, vertexC());
        assertThat(
                vertexC().getNbNeighbors().getTotal(),
                is(4)
        );
    }

    @Test
    public void bad_request_if_other_user_uri() {
        JSONObject jsonUser = userUtils().validForCreation();
        createUserUsingJson(jsonUser);
        authenticate(jsonUser);
        Vertex anotherUserVertex = vertexUtils().createSingleVertex();
        authenticate(defaultAuthenticatedUser);
        Vertex farVertex = vertexUtils().createSingleVertex();
        Integer nbEdges = vertexUtils().vertexWithUriOfAnyUser(
                anotherUserVertex.uri()
        ).getNbNeighbors().getTotal();
        assertThat(
                mergeTo(farVertex, anotherUserVertex).getStatus(),
                is(Response.Status.BAD_REQUEST.getStatusCode())
        );
        assertThat(
                vertexUtils().vertexWithUriOfAnyUser(
                        anotherUserVertex.uri()
                ).getNbNeighbors().getTotal(),
                is(nbEdges)
        );
    }

    @Test
    public void bad_request_when_one_of_the_vertex_is_a_pattern_or_under_a_pattern() {
        Vertex farVertex = vertexUtils().createSingleVertex();
        vertexUtils().addAVertexToForkWithUri(farVertex.uri());
        vertexUtils().addAVertexToForkWithUri(farVertex.uri());
        Integer nbEdges = vertexC().getNbNeighbors().getTotal();
        vertexUtils().makePattern(farVertex.uri());
        assertThat(
                mergeTo(farVertex, vertexC()).getStatus(),
                is(Response.Status.BAD_REQUEST.getStatusCode())
        );
        assertThat(
                vertexC().getNbNeighbors().getTotal(),
                is(nbEdges)
        );
    }

    private ClientResponse mergeTo(Vertex source, Vertex destination) {
        return resource
                .path(
                        source.uri().getPath()
                )
                .path("mergeTo")
                .path(
                        UserUris.graphElementShortId(
                                destination.uri()
                        )
                ).cookie(authCookie)
                .header(SessionHandler.X_XSRF_TOKEN, currentXsrfToken)
                .post(ClientResponse.class);
    }
}
