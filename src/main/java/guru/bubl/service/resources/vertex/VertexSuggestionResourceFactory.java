/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.service.resources.vertex;

import guru.bubl.module.model.graph.vertex.VertexOperator;

public interface VertexSuggestionResourceFactory {
    VertexSuggestionResource ofVertex(VertexOperator vertex);
}
