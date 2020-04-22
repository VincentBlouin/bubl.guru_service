/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package js_test_data.scenarios;

import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.subgraph.SubGraphJson;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import js_test_data.JsTestScenario;
import org.codehaus.jettison.json.JSONObject;

import javax.inject.Inject;

public class BubbleWith2RelationsToSameBubbleScenario implements JsTestScenario {

    /*
    *
    * center-r1->child
    * center-r2->child
    * child is the same bubble
    */

    @Inject
    protected GraphFactory graphFactory;

    @Inject
    protected VertexFactory vertexFactory;

    User user = User.withEmailAndUsername("a", "b");

    @Override
    public JSONObject build() {
        UserGraph userGraph = graphFactory.loadForUser(user);
        VertexOperator center = vertexFactory.createForOwner(
                user.username()
        );
        center.label("center");

        VertexOperator child = vertexFactory.createForOwner(
                user.username()
        );
        child.label("child");
        center.addRelationToFork(child.uri(), center.getShareLevel(), child.getShareLevel()).label("r1");
        center.addRelationToFork(child.uri(), center.getShareLevel(), child.getShareLevel()).label("r2");
        return SubGraphJson.toJson(
                userGraph.aroundForkUriInShareLevels(
                        center.uri(),
                        ShareLevel.allShareLevelsInt
                )
        );
    }
}
