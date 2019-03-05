package com.haile.apps.slack.meme.client;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class SlackClient {
    private static final Logger logger = LoggerFactory.getLogger(SlackClient.class);

    private Client client;

    private WebTarget webTarget;

    public SlackClient(String url) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(JacksonFeature.class);
        client = ClientBuilder.newClient(clientConfig);
        webTarget = client.target(url);
    }

    public Response getImgurImageUrls(String searchString, String clientId) {
        return webTarget.queryParam("q_any", searchString).request(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Client-ID " + clientId).get();
    }

    public Response postToSlack(String jsonString) {
        return webTarget.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    }

}
