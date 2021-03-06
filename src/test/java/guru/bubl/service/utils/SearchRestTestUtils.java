/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.service.utils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.json.UserJson;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.service.SessionHandler;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SearchRestTestUtils {
    private WebResource resource;
    private NewCookie authCookie;
    private JSONObject authenticatedUserAsJson;
    private String xsrfToken;

    private Gson gson = new Gson();

    public static SearchRestTestUtils withWebResourceAndAuthCookie(WebResource resource, NewCookie authCookie, JSONObject authenticatedUser, String xsrfToken) {
        return new SearchRestTestUtils(resource, authCookie, authenticatedUser, xsrfToken);
    }

    protected SearchRestTestUtils(WebResource resource, NewCookie authCookie, JSONObject authenticatedUserAsJson, String xsrfToken) {
        this.resource = resource;
        this.authCookie = authCookie;
        this.authenticatedUserAsJson = authenticatedUserAsJson;
        this.xsrfToken = xsrfToken;
    }

    public List<GraphElementSearchResult> searchForRelations(String textToSearchWith, JSONObject user) {
        return gson.fromJson(
                searchForRelationsClientResponse(
                        textToSearchWith,
                        user
                ).getEntity(String.class),
                new TypeToken<List<GraphElementSearchResultPojo>>() {
                }.getType()
        );
    }

    private ClientResponse searchForRelationsClientResponse(String textToSearchWith, JSONObject user) {
        ClientResponse clientResponse = NoEx.wrap(() -> resource
                .path("service")
                .path("users")
                .path(user.optString(UserJson.USER_NAME))
                .path("search")
                .path("relations")
                .path("auto_complete")
                .cookie(authCookie)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(SessionHandler.X_XSRF_TOKEN, xsrfToken)
                .post(ClientResponse.class, new JSONObject().put(
                        "searchText",
                        textToSearchWith
                ))
        ).get();
        assertThat(
                clientResponse.getStatus(),
                is(Response.Status.OK.getStatusCode())
        );
        return clientResponse;
    }

    public List<GraphElementSearchResult> autoCompletionResultsForPublicAndUserVertices(
            String textToSearchWith, JSONObject user
    ) {
        return vertexSearchResultsFromResponse(
                clientResponseOfAutoCompletionForVerticesOfUser(
                        textToSearchWith,
                        user,
                        false
                )
        );
    }


    public List<GraphElementSearchResult> autoCompletionResultsForUserVerticesOnly(
            String textToSearchWith, JSONObject user
    ) {
        return vertexSearchResultsFromResponse(
                clientResponseOfAutoCompletionOfUserOwnedVertices(
                        textToSearchWith,
                        user
                )
        );
    }

    public ClientResponse autoCompletionForPublicVertices(
            String textToSearch
    ) {
        return resource
                .path("service")
                .path("search")
                .queryParam("text", textToSearch)
                .header(SessionHandler.X_XSRF_TOKEN, xsrfToken)
                .get(ClientResponse.class);
    }

    public ClientResponse getSearchDetailsAnonymously(
            URI uri
    ) {
        return resource
                .path("service")
                .path("search")
                .path("details")
                .queryParam("uri", uri.toString())
                .header(SessionHandler.X_XSRF_TOKEN, xsrfToken)
                .get(ClientResponse.class);
    }

    public GraphElementSearchResult graphElementSearchResultFromClientResponse(ClientResponse clientResponse) {
        JSONObject jsonObject = clientResponse.getEntity(JSONObject.class);
        Gson gson = new Gson();
        return jsonObject.has("edge") ?
                gson.fromJson(
                        jsonObject.toString(),
                        GraphElementSearchResultPojo.class
                ) :
                gson.fromJson(
                        jsonObject.toString(),
                        GraphElementSearchResultPojo.class
                );
    }

    public List<GraphElementSearchResult> autoCompletionResultsForCurrentUserVerticesOnly(String textToSearchWith) {
        return vertexSearchResultsFromResponse(
                clientResponseOfAutoCompletionOfCurrentUserVerticesOnly(
                        textToSearchWith
                )
        );
    }

    public List<GraphElementSearchResult> vertexSearchResultsFromResponse(ClientResponse clientResponse) {
        String jsonString = clientResponse.getEntity(JSONArray.class).toString();
        return gson.fromJson(
                jsonString,
                new TypeToken<List<GraphElementSearchResultPojo>>() {
                }.getType()
        );
    }

    public ClientResponse clientResponseOfAutoCompletionOfCurrentUserVerticesOnly(String textToSearchWith) {
        return clientResponseOfAutoCompletionOfUserOwnedVertices(
                textToSearchWith,
                authenticatedUserAsJson
        );
    }

    public ClientResponse clientResponseOfAutoCompletionOfUserOwnedVertices(String textToSearchWith, JSONObject user) {
        return clientResponseOfAutoCompletionForVerticesOfUser(
                textToSearchWith,
                user,
                true
        );
    }

    public GraphElementSearchResult searchDetailsByUri(URI uri) {
        return graphElementSearchResultFromClientResponse(
                searchByUriClientResponse(
                        uri
                )
        );
    }

    public ClientResponse searchByUriClientResponse(URI uri) {
        return resource
                .path("service")
                .path("users")
                .path(authenticatedUserAsJson.optString(UserJson.USER_NAME))
                .path("search")
                .path("details")
                .queryParam("uri", uri.toString())
                .cookie(authCookie)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(SessionHandler.X_XSRF_TOKEN, xsrfToken)
                .get(ClientResponse.class);
    }

    public void indexAll() {
        ClientResponse response = resource
                .path("service")
                .path("test")
                .path("search")
                .path("index_graph")
                .cookie(authCookie)
                .header(SessionHandler.X_XSRF_TOKEN, xsrfToken)
                .get(ClientResponse.class);
        assertThat(response.getStatus(), is(200));
    }

    private ClientResponse clientResponseOfAutoCompletionForVerticesOfUser(String textToSearchWith, JSONObject user, Boolean onlyOwnVertices) {
        ClientResponse clientResponse = NoEx.wrap(() -> resource
                .path("service")
                .path("users")
                .path(user.optString(UserJson.USER_NAME))
                .path("search")
                .path(onlyOwnVertices ? "own_vertices" : "vertices")
                .path("auto_complete")
                .cookie(authCookie)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(SessionHandler.X_XSRF_TOKEN, xsrfToken)
                .post(ClientResponse.class, new JSONObject().put(
                        "searchText",
                        textToSearchWith
                ))).get();
        assertThat(
                clientResponse.getStatus(),
                is(Response.Status.OK.getStatusCode())
        );
        return clientResponse;
    }
}
