package com.haile.apps.slack.meme.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;

@Controller
@RequestMapping("/")
public class SlackMemeController {
	private static final Logger logger = LoggerFactory.getLogger(SlackMemeController.class);

	@RequestMapping(value = "/meme", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED, produces = MediaType.APPLICATION_JSON)
	public @ResponseBody ResponseEntity<?> memefyImage(HttpServletRequest request) {
		logger.info("Incomming request on path: " + request.getServletPath() + " and from addr: "
				+ request.getRemoteAddr());
		Map<String, String[]> parameterMap = request.getParameterMap();
		logger.info(parameterMap.get("team_id")[0] + ":" + parameterMap.get("team_domain")[0] + ", "
				+ parameterMap.get("channel_id")[0] + ":" + parameterMap.get("channel_name")[0] + ", "
				+ parameterMap.get("user_id")[0] + ":" + parameterMap.get("user_name")[0] + ", "
				+ parameterMap.get("command")[0] + ", " + parameterMap.get("text")[0]);
		String responseUrl = parameterMap.get("response_url")[0];
		String slackMessage = parameterMap.get("text")[0];
		
		String imageUrl = null;
		String memeText = null;
		
		if (slackMessage.startsWith("@")){
			imageUrl = "http://habeshait.com/MemePics/original/" + slackMessage.substring(1, slackMessage.indexOf(" ")) + ".jpg";
			memeText = slackMessage.substring(slackMessage.indexOf(" ") + 1).trim();
		} else if (slackMessage.startsWith("[")) {
			imageUrl = slackMessage.substring(1, slackMessage.indexOf("]")).trim();
			memeText = slackMessage.substring(slackMessage.indexOf("]") + 1).trim();
		} else {
			imageUrl = "http://habeshait.com/MemePics/original/sunflower.jpg";
			memeText = slackMessage;
		}
		
		//memefyapi request
		HashMap<String, String> memeRequest = new HashMap<String, String>();
		memeRequest.put("imageUrl", imageUrl);
		memeRequest.put("memeText", memeText);
				
		ObjectMapper mapper = new ObjectMapper();
		String memefyJsonString = null;
		try {
			memefyJsonString = mapper.writeValueAsString(memeRequest);
			logger.info("Memefy json Body: " + memefyJsonString);
		} catch (JsonProcessingException e) {
			logger.error("Couldn't generate body to MemefyApi!");
			return new ResponseEntity<>("Couldn't memefy the image resource specified by url: " + imageUrl, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			logger.error("Couldn't generate body to MemefyApi!");
			return new ResponseEntity<>("Couldn't memefy the image resource specified by url: " + imageUrl, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basic("admin", "abbhst");
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(authFeature);
		clientConfig.register(JacksonFeature.class);
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget webTarget = client.target("https://memefyapi.herokuapp.com/memefy/url").path("/");

		Response memeResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(memefyJsonString, MediaType.APPLICATION_JSON_TYPE));
    			
		HashMap<String, String> memeResponseMap = memeResponse.readEntity(new GenericType<HashMap<String, String>>() { });
		String responseImageUrl = memeResponseMap.get("imageUrl");
		logger.info("meme image url: " + responseImageUrl);
		//End
		
		// Prepare image attachments
		ArrayList<HashMap<String, Object>> attachments = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> imageAttachment = new HashMap<String, Object>();		
		imageAttachment.put("image_url", responseImageUrl);
		
		// Prepare option attachments
		HashMap<String, Object> optionAttachment = new HashMap<String, Object>();
		optionAttachment.put("fallback", "Do you want to post the meme?");
		optionAttachment.put("title", "Do you want to post the meme?");
		optionAttachment.put("callback_id", "confirm_meme_1985");
		optionAttachment.put("color", "#3AA3E3");
		optionAttachment.put("attachment_type", "default");
		ArrayList<HashMap<String, Object>> actions = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> actionPost = new HashMap<String, Object>();
		actionPost.put("name", "post");
		actionPost.put("text", "Post");
		actionPost.put("type", "button");
		actionPost.put("value", "post");
		HashMap<String, Object> actionCancel = new HashMap<String, Object>();
		actionCancel.put("name", "cancel");
		actionCancel.put("text", "Cancel");
		actionCancel.put("type", "button");
		actionCancel.put("value", "cancel");
		actions.add(actionPost);
		actions.add(actionCancel);
		optionAttachment.put("actions", actions);
		
		attachments.add(optionAttachment);
		attachments.add(imageAttachment);

		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("response_type", "in_channel");
		output.put("text", "The generated meme..");
		output.put("attachments", attachments);
		
		String jsonString = null;
		try {
			jsonString = mapper.writeValueAsString(output);
			logger.info("Output: " + jsonString);
		} catch (JsonProcessingException e) {
			logger.error("Couldn't generate slack output!");
			return new ResponseEntity<>("Couldn't generate slack output!", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			logger.error("Couldn't generate slack output!");
			return new ResponseEntity<>("Couldn't generate slack output!", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		//Response slackResponse = null;
		//slackResponse = sendToSlack(responseUrl, jsonString, "", "");
		//logger.info("Message OPTION posted to Slack with status code: " + slackResponse.getStatus());
		return new ResponseEntity<>(jsonString, HttpStatus.OK);
	}

	@RequestMapping(value = "/meme/confirm", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED, produces = MediaType.APPLICATION_JSON)
	public @ResponseBody ResponseEntity<?> confirmPost(HttpServletRequest request) {
		logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_"
				+ request.getRemoteUser());
		String responseUrl = null;
		String imageUrl = null;
		String bodyString = request.getParameterMap().get("payload")[0];
		ObjectMapper mapper = new ObjectMapper();
		
		JsonNode payload;
		try {
			payload = mapper.readTree(bodyString);
			//JsonNode type = payload.get("type");
			
			JsonNode actions = payload.get("actions");
			String command = actions.get(0).get("name").getTextValue();

			if (command.equalsIgnoreCase("post")) {
				JsonNode originalMessage = payload.get("original_message");
				imageUrl = payload.get("original_message").get("attachments").get(0).get("image_url").getTextValue();
				logger.info(originalMessage.get("text").getTextValue());
				responseUrl = payload.get("response_url").getTextValue();
				logger.info(responseUrl);
			} else if (command.equalsIgnoreCase("cancel")) {				
				return new ResponseEntity<>(null, HttpStatus.OK);
			} else {
				return new ResponseEntity<>("Not allowed!", HttpStatus.BAD_REQUEST);
			}

		} catch (IOException e) {
			logger.info("Couldn't read input! " + e.getMessage());
			return new ResponseEntity<>("Couldn't read input! ", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ArrayList<HashMap<String, Object>> attachments = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> attachment = new HashMap<String, Object>();
		attachment.put("image_url", imageUrl);
		attachments.add(attachment);

		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("response_type", "in_channel");
		output.put("attachments", attachments);
		String jsonString = null;
		try {
			jsonString = mapper.writeValueAsString(output);
			logger.info("Output: " + jsonString);
		} catch (JsonProcessingException e) {
			logger.info("Couldn't generate output!" + e.getMessage());
			return new ResponseEntity<>("Couldn't generate output!", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			logger.info("Couldn't generate output!" + e.getMessage());
			return new ResponseEntity<>("Couldn't generate output!", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		//Response slackResponse = null;
		
		//slackResponse = sendToSlack(responseUrl, jsonString, "", "");

		return new ResponseEntity<>(jsonString, HttpStatus.OK);
	}

	private Response sendToSlack(String responseURL, String jsonString, String apiUser, String apiPassword) {
		HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basic(apiUser, apiPassword);
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(authFeature);
		clientConfig.register(JacksonFeature.class);
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget webTarget = client.target(responseURL).path("/");

		Response response = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

		return response;
	}

}
