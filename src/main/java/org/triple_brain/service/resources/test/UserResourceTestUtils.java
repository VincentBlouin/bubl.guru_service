/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.service.resources.test;

import com.google.gson.Gson;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.UserNameGenerator;
import org.triple_brain.module.model.forgot_password.UserForgotPasswordToken;
import org.triple_brain.module.model.graph.GraphTransactional;
import org.triple_brain.module.model.json.UserJson;
import org.triple_brain.module.repository.user.UserRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

@Path("test/users")
@Singleton
public class UserResourceTestUtils {

    @Inject
    UserRepository userRepository;

    @Inject
    protected QueryEngine queryEngine;

    @Inject
    UserNameGenerator userNameGenerator;

    @Inject
    Gson gson;

    @Path("{email}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response emailExists(@PathParam("email") String email)throws Exception{
        return Response.ok(
                userRepository.emailExists(email).toString()
        ).build();
    }

    @Path("/")
    @DELETE
    @GraphTransactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteAllUsers()throws Exception{
        queryEngine.query(
                "START n=node:node_auto_index('type:user') DELETE n",
                Collections.EMPTY_MAP
        );
        return Response.noContent().build();
    }


    @POST
    @Path("/vince")
    public Response createUserVince()throws Exception{
        userNameGenerator.setOverride(new UserNameGenerator() {
            @Override
            public String generate() {
                return "vince";
            }

            @Override
            public void setOverride(UserNameGenerator userNameGenerator) {

            }

            @Override
            public void reset() {

            }
        });
        User user = User.withEmail(
                "vince_email@example.org"
        ).password("password");
        userRepository.createUser(user);
        userNameGenerator.reset();
        return Response.ok().entity(
                UserJson.toJson(user)
        ).build();
    }

    @Path("/{username}/forget-password-token")
    @GET
    public Response getForgetPasswordToken(@PathParam("username") String username)throws Exception{
        User user = userRepository.findByUsername(username);
        return Response.ok().entity(
                gson.toJson(
                        userRepository.getUserForgetPasswordToken(user)
                )
        ).build();
    }

    @Path("/{username}/forget-password-token")
    @POST
    public Response setForgetPasswordToken(@PathParam("username") String username, String userForgetPasswordTokenJson)throws Exception{
        UserForgotPasswordToken userForgotPasswordToken = gson.fromJson(
                userForgetPasswordTokenJson,
                UserForgotPasswordToken.class
        );
        userRepository.generateForgetPasswordToken(
                userRepository.findByUsername(username),
                userForgotPasswordToken
        );
        return Response.noContent().build();
    }

}
