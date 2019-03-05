package com.haile.apps.slack.meme.controller;

import com.haile.apps.slack.meme.client.SlackClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/")
public class ImgurController {
    private static final Logger logger = LoggerFactory.getLogger(ImgurController.class);
    private static final String IMGUR_BASE_URL = "https://api.imgur.com/3/gallery/search/viral/all/2";

    @Value("${imgur.client}")
    private String imgurClientId;

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/meme", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED, produces = MediaType.APPLICATION_JSON)
    public void getMemes(HttpServletRequest request) {
        logger.info("Incomming request on path: " + request.getServletPath() + " and from addr: "
                + request.getRemoteAddr());
        LinkedHashMap<String, Object> errorMap = new LinkedHashMap<>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        logger.info(parameterMap.get("team_id")[0] + ":" + parameterMap.get("team_domain")[0] + ", "
                + parameterMap.get("channel_id")[0] + ":" + parameterMap.get("channel_name")[0] + ", "
                + parameterMap.get("user_id")[0] + ":" + parameterMap.get("user_name")[0] + ", "
                + parameterMap.get("command")[0] + ", " + parameterMap.get("text")[0]);
        String responseUrl = parameterMap.get("response_url")[0];
        logger.info("ResponseUrl: " + responseUrl);

        SlackClient imgurClient = new SlackClient(IMGUR_BASE_URL);
        Response imgurResponse = imgurClient.getImgurImageUrls("meme " + parameterMap.get("text")[0], imgurClientId);


        SlackClient slackClient = new SlackClient(responseUrl);
        if (imgurResponse.getStatus() != 200) {
            logger.error("Couldn't generate body to MemefyApi!");
            errorMap.put("response_type", "ephemeral");
            errorMap.put("delete_original", true);
            errorMap.put("text", "Error generating meme. Sorry for the inconvenience :cry:");
            sendErrorMessageToSlack(errorMap, responseUrl);
            return;
        }

        JsonNode memeResponseMap = imgurResponse.readEntity(JsonNode.class);
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

        // Prepare image attachments
        ArrayList<LinkedHashMap<String, Object>> attachments = new ArrayList<>();
        LinkedHashMap<String, Object> imageAttachment = new LinkedHashMap<>();
        imageAttachment.put("fallback", imgUrls.get(0));
        imageAttachment.put("callback_id", "confirm_meme_image");
        imageAttachment.put("color", "#ffff00");
        imageAttachment.put("attachment_type", "default");
        imageAttachment.put("image_url", imgUrls.get(0));

        // Prepare option attachments
        LinkedHashMap<String, Object> optionAttachment = new LinkedHashMap<>();
        optionAttachment.put("fallback", "Option buttons, Post or Cancel");
        optionAttachment.put("title", "Do you want to post the meme?");
        optionAttachment.put("callback_id", "confirm_meme_button");
        optionAttachment.put("color", "#3AA3E3");
        optionAttachment.put("attachment_type", "default");
        ArrayList<LinkedHashMap<String, Object>> actions = new ArrayList<>();
        LinkedHashMap<String, Object> actionPost = new LinkedHashMap<>();
        actionPost.put("name", "post");
        actionPost.put("text", "Post");
        actionPost.put("type", "button");
        actionPost.put("value", "post");
        LinkedHashMap<String, Object> actionCancel = new LinkedHashMap<>();
        actionCancel.put("name", "cancel");
        actionCancel.put("text", "Cancel");
        actionCancel.put("type", "button");
        actionCancel.put("value", "cancel");
        actions.add(actionPost);
        actions.add(actionCancel);

        optionAttachment.put("actions", actions);

        attachments.add(imageAttachment);
        attachments.add(optionAttachment);

        //prepare output
        LinkedHashMap<String, Object> output = new LinkedHashMap<>();
        output.put("response_type", "in_channel");
        output.put("delete_original", true);
        output.put("attachments", attachments);
        ObjectMapper mapper = new ObjectMapper();
        try {
            slackClient.postToSlack(mapper.writeValueAsString(output));
        } catch (IOException e) {
            logger.error("Error while preparing body to slack command reply.");
        }
    }

    private void sendErrorMessageToSlack(LinkedHashMap<String, Object> errorMap, String responseUrl) {
        SlackClient slackClient = new SlackClient(responseUrl);
        ObjectMapper mapper = new ObjectMapper();
        try {
            slackClient.postToSlack(mapper.writeValueAsString(errorMap));
        } catch (IOException e) {
            logger.info("Error while preparing body to slack command reply.");
        }
    }

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/meme/confirm", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED, produces = MediaType.APPLICATION_JSON)
    public void confirmPost(HttpServletRequest request) {
        logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_"
                + request.getRemoteUser());

        String bodyString = request.getParameterMap().get("payload")[0];
        logger.info("Payload: " + bodyString);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(JacksonFeature.class);
        Client client = ClientBuilder.newClient(clientConfig);
        ObjectMapper mapper = new ObjectMapper();
        LinkedHashMap<String, Object> errorMap = new LinkedHashMap<>();
        JsonNode payload = null;
        try {
            payload = mapper.readTree(bodyString);
        } catch (IOException e) {
            logger.error("Error while reading message from slack! " + e.getMessage());
            return;
        }

        if (payload.isNull() || !payload.has("response_url") || !payload.has("original_message")) {
            logger.error("Error because of either of the following: The generated payload from request is null, payload doesn't contain response url or original message.");
            return;
        }
        String responseUrl = payload.get("response_url").getTextValue();
        logger.info("ResponseUrl: " + responseUrl);

        JsonNode actions = payload.get("actions");
        String command = actions.get(0).get("name").getTextValue();
        if (command.equalsIgnoreCase("post")) {
            JsonNode originalMessage = payload.get("original_message");
            JsonNode imageNode = null;
            for (JsonNode node : originalMessage.get("attachments")) {
                if (node.get("callback_id").getTextValue().equalsIgnoreCase("confirm_meme_image")) {
                    imageNode = node;
                    break;
                }
            }
            String id = payload.get("user").get("id").getTextValue();

            ArrayList<LinkedHashMap<String, Object>> attachments = new ArrayList<>();
            LinkedHashMap<String, Object> attachment = new LinkedHashMap<>();
            attachment.put("fallback", imageNode.get("fallback").getTextValue());
            attachment.put("callback_id", imageNode.get("callback_id").getTextValue());
            attachment.put("color", "#00cc66");
            attachment.put("attachment_type", "default");
            attachment.put("image_url", imageNode.get("image_url").getTextValue());
            attachments.add(attachment);

            LinkedHashMap<String, Object> output = new LinkedHashMap<>();
            output.put("response_type", "in_channel");
            output.put("replace_original", true);
            //output.put("delete_original", true);
            output.put("text", "<@" + id + "> memed :smiley:");
            output.put("attachments", attachments);

            SlackClient slackClient = new SlackClient(responseUrl);
            try {
                slackClient.postToSlack(mapper.writeValueAsString(output));
            } catch (IOException e) {
                logger.error("Error while preparing body to slack command reply.");
            }
        } else if (command.equalsIgnoreCase("cancel")) {
            logger.info("Cancel received from slack. Posting Cancel message to Slack...");
            errorMap.put("response_type", "ephemeral");
            //errorMap.put("replace_original", true);
            errorMap.put("delete_original", true);
            sendErrorMessageToSlack(errorMap, responseUrl);
        } else {
            logger.info("Invalid Command button received from slack. Posting Cancel message to Slack...");
            errorMap.put("response_type", "ephemeral");
            //errorMap.put("replace_original", true);
            errorMap.put("delete_original", true);
            sendErrorMessageToSlack(errorMap, responseUrl);
        }
    }
}

