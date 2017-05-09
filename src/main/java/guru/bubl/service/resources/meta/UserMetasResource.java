/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.service.resources.meta;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.meta.MetaJson;
import guru.bubl.module.model.meta.UserMetasOperatorFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
public class UserMetasResource {

    @Inject
    UserMetasOperatorFactory userMetasOperatorFactory;

    private User user;

    @AssistedInject
    public UserMetasResource(
            @Assisted User user
    ) {
        this.user = user;
    }

    @GET
    public Response get(){
        return Response.ok().entity(
                MetaJson.toJsonSet(
                        userMetasOperatorFactory.forUser(user).get()
                )
        ).build();
    }
}
