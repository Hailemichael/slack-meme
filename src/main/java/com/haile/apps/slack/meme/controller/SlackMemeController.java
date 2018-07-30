package com.haile.apps.slack.meme.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
		logger.info("ResponseUrl: " + responseUrl);
		String slackMessage = parameterMap.get("text")[0];
		LinkedHashMap<String, Object> errorMap = new LinkedHashMap<String, Object>();
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
			errorMap.put("text", "Couldn't memefy the image resource specified by url: " + imageUrl);
			try {
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error("Error while preparing body to Memefy API request");
			}
		} catch (IOException e) {
			logger.error("Couldn't generate body to MemefyApi!");
			errorMap.put("text", "Couldn't memefy the image resource specified by url: " + imageUrl);
			try {
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error("Error while preparing body to Memefy API request");
			}
		}
		
		/*HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basic("admin", "abbhst");
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(authFeature);
		clientConfig.register(JacksonFeature.class);
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget webTarget = client.target("https://memefyapi.herokuapp.com/memefy/url");

		Response memeResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(memefyJsonString, MediaType.APPLICATION_JSON_TYPE));
    			
		HashMap<String, String> memeResponseMap = memeResponse.readEntity(new GenericType<HashMap<String, String>>() { });
		String responseImageUrl = memeResponseMap.get("imageUrl");
		logger.info("meme image url: " + responseImageUrl);*/
		//End
		
		// Prepare image attachments
		ArrayList<LinkedHashMap<String, Object>> attachments = new ArrayList<LinkedHashMap<String, Object>>();
		LinkedHashMap<String, Object> imageAttachment = new LinkedHashMap<String, Object>();		
		imageAttachment.put("image_url", imageUrl);
		
		// Prepare option attachments
		LinkedHashMap<String, Object> optionAttachment = new LinkedHashMap<String, Object>();
		optionAttachment.put("fallback", "Do you want to post the meme?");
		optionAttachment.put("title", "Do you want to post the meme?");
		optionAttachment.put("callback_id", "confirm_meme_1985");
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
		output.put("text", "The generated meme..");
		output.put("attachments", attachments);
		
		String jsonString = null;
		try {
			jsonString = mapper.writeValueAsString(output);
			logger.info("Output: " + jsonString);
		} catch (JsonProcessingException e) {
			logger.error("Couldn't generate slack output!");
			errorMap.put("text", "Couldn't generate slack output!");
			try {
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error("Error while preparing body to Slack reply");
			}
		} catch (IOException e) {
			logger.error("Couldn't generate slack output!");
			errorMap.put("text", "Couldn't generate slack output!");
			try {
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error("Error while preparing body to Slack reply");
			}
		}
		
		/*WebTarget slackWebTarget = client.target(responseUrl);

		Response slackResponse = slackWebTarget.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		
		logger.info("Message OPTION posted to Slack with status code: " + slackResponse.getStatus());*/
		return new ResponseEntity<>(jsonString, HttpStatus.OK);
	}

	@RequestMapping(value = "/meme/confirm", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED, produces = MediaType.APPLICATION_JSON)
	public @ResponseBody ResponseEntity<?> confirmPost(HttpServletRequest request) {
		logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_"
				+ request.getRemoteUser());
		String text = null;
		String imageUrl = null;
		String bodyString = request.getParameterMap().get("payload")[0];
		logger.info("Payload: " + bodyString);
		ObjectMapper mapper = new ObjectMapper();
		LinkedHashMap<String, Object> errorMap = new LinkedHashMap<String, Object>();
		JsonNode payload;
		try {
			payload = mapper.readTree(bodyString);
			
			JsonNode actions = payload.get("actions");
			String command = actions.get(0).get("name").getTextValue();
			

			if (command.equalsIgnoreCase("post")) {
				JsonNode originalMessage = payload.get("original_message");
				text = (originalMessage.get("text")!=null)?originalMessage.get("text").getTextValue():"Your memefied image...";
				JsonNode attachements = originalMessage.get("attachments");
				if (attachements.isArray() && (attachements.size() == 2)) {
					if (attachements.get(0).has("image_url")) {
						imageUrl = attachements.get(0).get("image_url").getTextValue();
					} else if (attachements.get(1).has("image_url")) {
						imageUrl = attachements.get(1).get("image_url").getTextValue();
					}
				} 
				if (imageUrl == null) {
					errorMap.put("text", "Couldn't find image in attachement from Slack!");
					return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.BAD_REQUEST);
				}								
				logger.info(originalMessage.get("text").getTextValue());
				//responseUrl = payload.get("response_url").getTextValue();
				//logger.info("ResponseUrl: " + responseUrl);
			} else if (command.equalsIgnoreCase("cancel")) {
				/*errorMap.put("text", " ");
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.OK);*/
				return null;
			} else {
				errorMap.put("text", "Not allowed button command!");
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.BAD_REQUEST);
			}

		} catch (IOException e) {
			logger.info("Couldn't read input! " + e.getMessage());
			errorMap.put("text", "Not allowed button command!");
			try {
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error("Couldn't prepare error output! " + e1.getMessage());
			}
		}

		ArrayList<LinkedHashMap<String, Object>> attachments = new ArrayList<LinkedHashMap<String, Object>>();
		LinkedHashMap<String, Object> attachment = new LinkedHashMap<String, Object>();
		attachment.put("image_url", imageUrl);
		attachments.add(attachment);

		LinkedHashMap<String, Object> output = new LinkedHashMap<String, Object>();
		output.put("response_type", "in_channel");
		output.put("text", text);
		output.put("attachments", attachments);
		String jsonString = null;
		try {
			jsonString = mapper.writeValueAsString(output);
			logger.info("Output: " + jsonString);
		} catch (JsonProcessingException e) {
			logger.error("Couldn't generate slack output!");
			errorMap.put("text", "Couldn't generate slack output!");
			try {
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error("Error while preparing body to Slack reply");
			}
		} catch (IOException e) {
			logger.error("Couldn't generate slack output!");
			errorMap.put("text", "Couldn't generate slack output!");
			try {
				return new ResponseEntity<>(mapper.writeValueAsString(errorMap), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error("Error while preparing body to Slack reply");
			}
		}
		/*ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(JacksonFeature.class);
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget webTarget = client.target(responseUrl).path("/");

		Response response = webTarget.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
		logger.info("Message Confirmation posted to Slack with status code: " + response.getStatus());*/
		return new ResponseEntity<>(jsonString, HttpStatus.OK);
	}
}
