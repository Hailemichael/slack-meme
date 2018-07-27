package com.haile.apps.slack.meme.controller;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/")
public class SlackMemeController {
	private static final Logger logger = LoggerFactory.getLogger(SlackMemeController.class);
	
	@RequestMapping(value = "/meme", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> memefyImage(HttpServletRequest request, @RequestParam HashMap<String, Object> body){
		logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_" + request.getRemoteUser());
		ObjectMapper mapper = new ObjectMapper();		
		try {
			logger.info(mapper.writeValueAsString(body));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new ResponseEntity<> (jsonString, HttpStatus.OK);
	}

}
