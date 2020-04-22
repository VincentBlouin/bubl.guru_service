/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package js_test_data;

import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexOperator;

import javax.inject.Inject;

public class AbstractScenario {

    @Inject
    VertexFactory vertexFactory;

    @Inject
    protected GraphFactory graphFactory;

    protected VertexOperator
            center,
            b1,
            b2,
            b3;

    protected RelationOperator
            r1,
            r2,
            r3;

    protected User user = User.withEmailAndUsername("a", "b");
    protected UserGraph userGraph;

    public void createUserGraph() {
        userGraph = graphFactory.loadForUser(user);
    }

    public void createVertices() {
        center = vertexFactory.createForOwner(
                user.username()
        );
        center.label("center");
        b1 = vertexFactory.createForOwner(
                user.username()
        );
        b1.label("b1");
        b2 = vertexFactory.createForOwner(
                user.username()
        );
        b2.label("b2");
        b3 = vertexFactory.createForOwner(
                user.username()
        );
        b3.label("b3");
    }

    public void createEdges() {
        RelationOperator r1 = center.addRelationToFork(b1.uri(), center.getShareLevel(), b1.getShareLevel());
        r1.label("r1");
        RelationOperator r2 = center.addRelationToFork(b2.uri(), center.getShareLevel(), b2.getShareLevel());
        r2.label("r2");
        RelationOperator r3 = center.addRelationToFork(b3.uri(), center.getShareLevel(), b3.getShareLevel());
        r3.label("r3");
    }
}
