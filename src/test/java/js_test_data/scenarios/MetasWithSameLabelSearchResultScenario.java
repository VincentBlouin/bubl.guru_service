/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package js_test_data.scenarios;

import com.google.gson.Gson;
import guru.bubl.module.model.graph.identification.IdentificationFactory;
import guru.bubl.module.model.graph.identification.IdentificationOperator;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphSearch;
import guru.bubl.module.model.test.scenarios.TestScenarios;
import js_test_data.AbstractScenario;
import js_test_data.JsTestScenario;

import javax.inject.Inject;
import java.util.List;

public class MetasWithSameLabelSearchResultScenario extends AbstractScenario implements JsTestScenario {

    /*
    * meta0 nbReference 0
    * meta1 nbReference 1
    * meta2 nbReference 2
    * meta3 nbReference 3
    * meta4 nbReference 4
    */

    @Inject
    GraphSearch graphSearch;

    @Inject
    IdentificationFactory identificationFactory;

    IdentificationOperator
            meta0,
            meta1,
            meta2,
            meta3;

    @Override
    public Object build() {
        createVertices();
        buildMetas();
        List<GraphElementSearchResult> searchResultsForMeta = graphSearch.searchForAnyResourceThatCanBeUsedAsAnIdentifier(
                "meta",
                user
        );
        return new Gson().toJson(
                searchResultsForMeta
        );
    }

    private void buildMetas() {
        meta0 = identificationFactory.withUri(
                b1.addMeta(
                        TestScenarios.identificationFromFriendlyResource(center)
                ).values().iterator().next().uri());
        meta0.label("meta0");
        meta0.setNbReferences(0);
        meta1 = identificationFactory.withUri(
                center.addMeta(
                        TestScenarios.identificationFromFriendlyResource(b1)
                ).values().iterator().next().uri());
        meta1.label("meta1");
        meta1.setNbReferences(1);
        meta2 = identificationFactory.withUri(
                center.addMeta(
                        TestScenarios.identificationFromFriendlyResource(b2)
                ).values().iterator().next().uri());
        meta2.label("meta2");
        meta2.setNbReferences(2);
        meta3 = identificationFactory.withUri(
                center.addMeta(
                        TestScenarios.identificationFromFriendlyResource(b3)
                ).values().iterator().next().uri());
        meta3.label("meta3");
        meta3.setNbReferences(3);
    }
}
