package org.triple_brain.service.resources;

import org.triple_brain.module.model.User;

/*
* Copyright Mozilla Public License 1.1
*/
public interface GraphResourceFactory {
    public GraphResource withUser(User user);
}
