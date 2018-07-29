package com.haile.apps.slack.meme.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

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

@Controller
@RequestMapping("/")
public class SlackMemeController {
	private static final Logger logger = LoggerFactory.getLogger(SlackMemeController.class);
	
	@RequestMapping(value = "/meme", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED, produces=MediaType.APPLICATION_JSON)
	public @ResponseBody ResponseEntity<?> memefyImage(HttpServletRequest request, @RequestParam HashMap<String, Object> body){
		logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_" + request.getRemoteUser());
		logger.info(request.getContentType());
		Map<String, String[]> parMap = request.getParameterMap();
		logger.info("body: " + body);
		logger.info("Response url: " + body.get("response_url"));
		ObjectMapper mapper = new ObjectMapper();		
		try {
			logger.info(mapper.writeValueAsString(body));
		} catch (JsonProcessingException e) {
			new ResponseEntity<> ("Couldn't read input!", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Prepare image attachments
		ArrayList<HashMap<String, Object>> attachments = new ArrayList<HashMap<String, Object>> ();
		HashMap<String, Object> imageAttachment = new HashMap<String, Object> ();
		imageAttachment.put("image_url", "http://habeshait.com/MemePics/memefied/2018-07-26T15-16-33.574+0000_IMG_9700.JPG");
		//Prepare option attachments
		HashMap<String, Object> optionAttachment = new HashMap<String, Object> ();
		optionAttachment.put("fallback", "Do you want to post the meme?");
		optionAttachment.put("title", "Do you want to post the meme?");
		optionAttachment.put("callback_id", "confirm_meme_1985");
		optionAttachment.put("color", "#3AA3E3");
		optionAttachment.put("attachment_type", "default");		
		ArrayList<HashMap<String, Object>> actions = new ArrayList<HashMap<String, Object>> ();
		HashMap<String, Object> actionPost = new HashMap<String, Object> ();
		actionPost.put("name", "post");
		actionPost.put("text", "Post");
		actionPost.put("type", "button");
		actionPost.put("value", "post");		
		HashMap<String, Object> actionCancel = new HashMap<String, Object> ();
		actionCancel.put("name", "cancel");
		actionCancel.put("text", "Cancel");
		actionCancel.put("type", "button");
		actionCancel.put("value", "cancel");		
		actions.add(actionPost);
		actions.add(actionCancel);		
		optionAttachment.put("actions", actions);
		
		attachments.add(imageAttachment);
		attachments.add(optionAttachment);
		
		HashMap<String, Object> output = new HashMap<String, Object> ();		
		output.put("response_type", "in_channel");
		output.put("text", "The generated meme..");
		output.put("attachments", attachments);
		
		String jsonString = null;
		try {
			jsonString = mapper.writeValueAsString(output);
			logger.info("Output: " + jsonString);
		} catch (JsonProcessingException e) {
			new ResponseEntity<> ("Couldn't generate output!", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new ResponseEntity<> (jsonString, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/meme/confirm", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED, produces=MediaType.APPLICATION_JSON)
	public @ResponseBody ResponseEntity<?> confirmPost(HttpServletRequest request) {
		logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_" + request.getRemoteUser());		
		String responseURL = null;
		JsonNode originalMessage = null;
		String bodyString = request.getParameterMap().entrySet().iterator().next().getKey();
		logger.info(bodyString);
	
	    ObjectMapper mapper = new ObjectMapper();	    
	    
	    JsonNode node;
		try {
			node = mapper.readTree(bodyString);
			JsonNode payload = node.get("payload");	    
			JsonNode type = payload.get("type"); 
			
			if(type.toString().equalsIgnoreCase("interactive_message")) {
				new ResponseEntity<> ("Not interactive message", HttpStatus.INTERNAL_SERVER_ERROR);
			}
			JsonNode actions = payload.get("actions");
			String command = actions.get(0).get("name").getTextValue();
			
			if(command.equalsIgnoreCase("post")) {
				originalMessage = payload.get("original_message");
				responseURL = payload.get("response_url").getTextValue();
				logger.info(responseURL);
			} else if(command.equalsIgnoreCase("cancel")) {
				return new ResponseEntity<> (null, HttpStatus.OK);
			} else {
				return new ResponseEntity<> ("Not allowed!", HttpStatus.BAD_REQUEST);
			}
			
		} catch (IOException e) {
			return new ResponseEntity<> ("Couldn't read input! " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}		
	    
		
		ArrayList<HashMap<String, Object>> attachments = new ArrayList<HashMap<String, Object>> ();
		HashMap<String, Object> attachment = new HashMap<String, Object> ();
		attachment.put("image_url", "http://habeshait.com/MemePics/memefied/2018-07-26T15-16-33.574+0000_IMG_9700.JPG");
		attachments.add(attachment);
		
		HashMap<String, Object> output = new HashMap<String, Object> ();		
		output.put("response_type", "in_channel");
		output.put("text", "What's upp");
		output.put("attachments", attachments);
		String jsonString = null;
		try {
			jsonString = mapper.writeValueAsString(output);
			logger.info("Output: " + jsonString);
		} catch (JsonProcessingException e) {			
			return new ResponseEntity<> ("Couldn't generate output!" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			return new ResponseEntity<> ("Couldn't generate output!" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<> (jsonString, HttpStatus.OK);
	}
}
