package org.triple_brain.service.resources.vertex;

import org.triple_brain.module.model.graph.UserGraph;

/*
* Copyright Mozilla Public License 1.1
*/
public interface VertexGroupResourceFactory {
    public VertexGroupResource withUserGraph(UserGraph userGraph);
}