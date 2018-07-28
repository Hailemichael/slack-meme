package com.haile.apps.slack.meme.controller;

import java.io.BufferedReader;
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
import org.springframework.util.MultiValueMap;
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
	
	@RequestMapping(value = "/meme/confirm", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> confirmPost(HttpServletRequest request){
		logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_" + request.getRemoteUser());
		logger.info(request.getContentType());
		logger.info(getBody(request));
		Map<String, String[]> parMap = request.getParameterMap();
		String responseURL = null;
		JsonNode originalMessage = null;
		ObjectMapper mapper = new ObjectMapper();		
/*		try {
			String jsonString = mapper.writeValueAsString(body);			
			JsonNode jsonBody = mapper.readTree(jsonString);
			JsonNode payload = jsonBody.get("payload");
			logger.info(payload.getTextValue());
			JsonNode type = payload.get("type");
			if(payload.get("type").asText().equalsIgnoreCase("interactive_message")) {
				new ResponseEntity<> ("Not interactive message", HttpStatus.INTERNAL_SERVER_ERROR);
			}
			JsonNode actions = payload.get("actions");
			
			if(payload.get("actions").get(0).get("name").getTextValue().equalsIgnoreCase("post")) {
				responseURL = payload.get("response_url").getTextValue();
				logger.info(responseURL);
				originalMessage = payload.get("original_message");
				logger.info(originalMessage.asText());
			} else if(payload.get("actions").get(0).get("name").getTextValue().equalsIgnoreCase("cancel")) {
				new ResponseEntity<> (null, HttpStatus.OK);
			}
			
			logger.info(mapper.writeValueAsString(body));
		} catch (JsonProcessingException e) {
			new ResponseEntity<> ("Couldn't read input!", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
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
			new ResponseEntity<> ("Couldn't generate output!", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new ResponseEntity<> (jsonString, HttpStatus.OK);
	}
	
	private String getBody(HttpServletRequest req) {
		  String body = "";
		  if (req.getMethod().equals("POST") )
		  {
		    StringBuilder sb = new StringBuilder();
		    BufferedReader bufferedReader = null;

		    try {
		      bufferedReader =  req.getReader();
		      char[] charBuffer = new char[128];
		      int bytesRead;
		      while ((bytesRead = bufferedReader.read(charBuffer)) != -1) {
		        sb.append(charBuffer, 0, bytesRead);
		      }
		    } catch (IOException ex) {
		      // swallow silently -- can't get body, won't
		    } finally {
		      if (bufferedReader != null) {
		        try {
		          bufferedReader.close();
		        } catch (IOException ex) {
		          // swallow silently -- can't get body, won't
		        }
		      }
		    }
		    body = sb.toString();
		  }
		  return body;
		}

}
