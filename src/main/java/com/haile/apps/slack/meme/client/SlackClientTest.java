package com.haile.apps.slack.meme.client;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlackClientTest {
    private static final Logger logger = LoggerFactory.getLogger(SlackClientTest.class);
    private static final String IMGUR_BASE_URL = "https://api.imgur.com/3/gallery/search/viral/all/2";
 public static void main(String [] args) {
     SlackClient imgurClient = new SlackClient(IMGUR_BASE_URL);
     Response imgurResponse = imgurClient.getImgurImageUrls("meme " + "crazy", "b67f9877da5b315");

     ObjectMapper mapper = new ObjectMapper();
     JsonNode memeResponseMap = null;
     try {
         memeResponseMap = mapper.readTree(imgurResponse.readEntity(String.class));
     } catch (IOException e) {
         e.printStackTrace();
     }
     List<String> imgUrls = new ArrayList<>();
     logger.info("Data size: " + memeResponseMap.get("data").size());
     for (int i = 0; i < memeResponseMap.get("data").size(); i++) {
         if (memeResponseMap.get("data").get(i).has("images")) {
             if (memeResponseMap.get("data").get(i).get("images").has(0)) {
                 if (memeResponseMap.get("data").get(i).get("images").get(0).has("link")) {
                     String imgUrl = String.valueOf(memeResponseMap.get("data").get(i).get("images").get(0).get("link"));
                     logger.info(imgUrl);
                     imgUrls.add(imgUrl);
                 }
             }
         }

     }
 }


}
