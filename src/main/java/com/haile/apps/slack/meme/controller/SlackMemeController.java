package com.haile.apps.slack.meme.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
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
	
	@ResponseStatus(value = HttpStatus.OK)
	@RequestMapping(value = "/meme", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED, produces = MediaType.APPLICATION_JSON)
	public void memefyImage(HttpServletRequest request) {
		logger.info("Incomming request on path: " + request.getServletPath() + " and from addr: "
				+ request.getRemoteAddr());
		HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basic("admin", "abbhst");
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(authFeature);
		clientConfig.register(JacksonFeature.class);
		Client client = ClientBuilder.newClient(clientConfig);
		LinkedHashMap<String, Object> errorMap = new LinkedHashMap<String, Object>();
		Map<String, String[]> parameterMap = request.getParameterMap();
		logger.info(parameterMap.get("team_id")[0] + ":" + parameterMap.get("team_domain")[0] + ", "
				+ parameterMap.get("channel_id")[0] + ":" + parameterMap.get("channel_name")[0] + ", "
				+ parameterMap.get("user_id")[0] + ":" + parameterMap.get("user_name")[0] + ", "
				+ parameterMap.get("command")[0] + ", " + parameterMap.get("text")[0]);
		String responseUrl = parameterMap.get("response_url")[0];
		logger.info("ResponseUrl: " + responseUrl);
		
		WebTarget slackWebTarget = client.target(responseUrl);
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
		ObjectMapper mapper = new ObjectMapper();
		//memefyapi request
		HashMap<String, String> memeRequest = new HashMap<String, String>();
		memeRequest.put("imageUrl", imageUrl);
		memeRequest.put("memeText", memeText);
				
		
		String memefyJsonString = null;
		try {
			memefyJsonString = mapper.writeValueAsString(memeRequest);
			logger.info("Memefy json Body: " + memefyJsonString);
		} catch (JsonProcessingException e) {
			logger.error("Couldn't generate body to MemefyApi!");
			errorMap.put("response_type", "ephemeral");
			errorMap.put("delete_original", true);
			errorMap.put("text", "Error generating meme. Sorry for the inconvienience :cry:");
			Response slackResponse;
			try {
				slackResponse = slackWebTarget.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.entity(mapper.writeValueAsString(errorMap), MediaType.APPLICATION_JSON_TYPE));
				logger.info("Error message posted to Slack with status code: " + slackResponse.getStatus());
			} catch (IOException e1) {
				logger.info("Error while preparing body to slack command reply.");
			}
			return;
		} catch (IOException e) {
			logger.error("Couldn't generate body to MemefyApi!");
			errorMap.put("response_type", "ephemeral");
			errorMap.put("delete_original", true);
			errorMap.put("text", "Error generating meme. Sorry for the inconvienience :cry:");
			Response slackResponse;
			try {
				slackResponse = slackWebTarget.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.entity(mapper.writeValueAsString(errorMap), MediaType.APPLICATION_JSON_TYPE));
				logger.info("Error message posted to Slack with status code: " + slackResponse.getStatus());
			} catch (IOException e1) {
				logger.info("Error while preparing body to slack command reply.");
			}
			return;
		}
		
		
		WebTarget webTarget = client.target("https://memefyapi.herokuapp.com/memefy/url");

		Response memeResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(memefyJsonString, MediaType.APPLICATION_JSON_TYPE));
    			
		HashMap<String, String> memeResponseMap = memeResponse.readEntity(new GenericType<HashMap<String, String>>() { });
		String responseImageUrl = memeResponseMap.get("imageUrl");
		logger.info("meme image url: " + responseImageUrl);
		//End
		
		// Prepare image attachments
		ArrayList<LinkedHashMap<String, Object>> attachments = new ArrayList<LinkedHashMap<String, Object>>();
		LinkedHashMap<String, Object> imageAttachment = new LinkedHashMap<String, Object>();
		imageAttachment.put("fallback", imageUrl);
		imageAttachment.put("callback_id", "confirm_meme_image");
		imageAttachment.put("color", "#ffff00");
		imageAttachment.put("attachment_type", "default");
		imageAttachment.put("image_url", imageUrl);
		
		// Prepare option attachments
		LinkedHashMap<String, Object> optionAttachment = new LinkedHashMap<String, Object>();
		optionAttachment.put("fallback", "Option buttons, Post or Cancel");
		optionAttachment.put("title", "Do you want to post the meme?");
		optionAttachment.put("callback_id", "confirm_meme_button");
		optionAttachment.put("color", "#3AA3E3");
		optionAttachment.put("attachment_type", "default");
		ArrayList<LinkedHashMap<String, Object>> actions = new ArrayList<LinkedHashMap<String, Object>>();
		LinkedHashMap<String, Object> actionPost = new LinkedHashMap<String, Object>();
		actionPost.put("name", "post");
		actionPost.put("text", "Post");
		actionPost.put("type", "button");
		actionPost.put("value", "post");
		LinkedHashMap<String, Object> actionCancel = new LinkedHashMap<String, Object>();
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
		LinkedHashMap<String, Object> output = new LinkedHashMap<String, Object>();
		output.put("response_type", "in_channel");
		output.put("delete_original", true);
		output.put("attachments", attachments);
		
		String jsonString = null;
		try {
			jsonString = mapper.writeValueAsString(output);
			logger.info("Output: " + jsonString);
			Response slackResponse = slackWebTarget.request(MediaType.APPLICATION_JSON_TYPE)
					.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
			
			logger.info("Message OPTION posted to Slack with status code: " + slackResponse.getStatus());
		} catch (JsonProcessingException e) {
			logger.error("Couldn't generate slack output!");
			errorMap.put("response_type", "ephemeral");
			errorMap.put("delete_original", true);
			errorMap.put("text", "Error while preparing your slack command reply. Sorry for the inconvienience :cry:");
			Response slackResponse;
			try {
				slackResponse = slackWebTarget.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.entity(mapper.writeValueAsString(errorMap), MediaType.APPLICATION_JSON_TYPE));
				logger.info("Error message posted to Slack with status code: " + slackResponse.getStatus());
			} catch (IOException e1) {
				logger.info("Error while preparing your slack command reply.");
			}
			return;
		} catch (IOException e) {
			logger.error("Couldn't generate slack output!");
			errorMap.put("response_type", "ephemeral");
			errorMap.put("delete_original", true);
			errorMap.put("text", "Error while preparing your slack command reply. Sorry for the inconvienience :cry:");
			Response slackResponse;
			try {
				slackResponse = slackWebTarget.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.entity(mapper.writeValueAsString(errorMap), MediaType.APPLICATION_JSON_TYPE));
				logger.info("Error message posted to Slack with status code: " + slackResponse.getStatus());
			} catch (IOException e1) {
				logger.info("Error while preparing your slack command reply.");
			}
			return;			
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
		LinkedHashMap<String, Object> errorMap = new LinkedHashMap<String, Object>();
		JsonNode payload;
		WebTarget webTarget = null;
		try {
			payload = mapper.readTree(bodyString);
			if (payload.isNull() || !payload.has("response_url") || !payload.has("original_message")){
				logger.error("Error because of either of the following: The generated payload from request is null, payload doesn't contain response url or original message.");;
				return;
			}
			String responseUrl = payload.get("response_url").getTextValue();
			logger.info("ResponseUrl: " + responseUrl);
			webTarget = client.target(responseUrl);		

			JsonNode actions = payload.get("actions");
			String command = actions.get(0).get("name").getTextValue();			
			if (command.equalsIgnoreCase("post")) {
				JsonNode originalMessage = payload.get("original_message");
				JsonNode attachements = originalMessage.get("attachments");
				JsonNode imageNode = null;
				for (JsonNode node: attachements) {
					if(node.get("callback_id").getTextValue().equalsIgnoreCase("confirm_meme_image")) {
						imageNode = node;
						
						break;
					}
				}	
				String id = payload.get("user").get("id").getTextValue();
				
				ArrayList<LinkedHashMap<String, Object>> attachments = new ArrayList<LinkedHashMap<String, Object>>();
				LinkedHashMap<String, Object> attachment = new LinkedHashMap<String, Object>();
				attachment.put("fallback", imageNode.get("fallback").getTextValue());
				attachment.put("callback_id", imageNode.get("callback_id").getTextValue());
				attachment.put("color", "#00cc66");
				attachment.put("attachment_type", "default");
				attachment.put("image_url", imageNode.get("image_url").getTextValue());
				attachments.add(attachment);

				LinkedHashMap<String, Object> output = new LinkedHashMap<String, Object>();
				output.put("response_type", "in_channel");
				output.put("replace_original", true);
				//output.put("delete_original", true);
				output.put("text", "<@"+ id + "> memed :smiley:");
				output.put("attachments", attachments);
				String jsonString = null;
				jsonString = mapper.writeValueAsString(output);
				logger.info("Output: " + jsonString);			

				Response response = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
				logger.info("Message Confirmation posted to Slack with status code: " + response.getStatus());
			} else if (command.equalsIgnoreCase("cancel")) {
				errorMap.put("response_type", "ephemeral");
				//errorMap.put("replace_original", true);
				errorMap.put("delete_original", true);
				Response slackResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.entity(mapper.writeValueAsString(errorMap), MediaType.APPLICATION_JSON_TYPE));			
				logger.info("Cancel received from slack. Cancel message posted to Slack with status code: " + slackResponse.getStatus());
				return;
			} else {
				errorMap.put("response_type", "ephemeral");
				//errorMap.put("replace_original", true);
				errorMap.put("delete_original", true);
				Response slackResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.entity(mapper.writeValueAsString(errorMap), MediaType.APPLICATION_JSON_TYPE));			
				logger.info("Invalid Command button received from slack. Cancel message posted to Slack with status code: " + slackResponse.getStatus());
				return;
			}
			
		} catch (IOException e) {
			logger.info("Error while reading/writing message from/to slack!" + e.getMessage());
			errorMap.put("response_type", "ephemeral");
			//errorMap.put("replace_original", true);
			errorMap.put("delete_original", true);
			errorMap.put("text", "Error while reading/writing message from/to slack!. Sorry for the inconvienience :cry:");
			Response slackResponse;
			try {
				slackResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.entity(mapper.writeValueAsString(errorMap), MediaType.APPLICATION_JSON_TYPE));
				logger.info("Error message posted to Slack with status code: " + slackResponse.getStatus());
			} catch (IOException e1) {
				logger.info("Error while preparing your slack command reply.");
			}
		}
	}
}
