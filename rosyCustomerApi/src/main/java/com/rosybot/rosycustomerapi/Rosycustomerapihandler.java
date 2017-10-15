package com.rosybot.rosycustomerapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosybot.services.PRODRosyCustomerService;
import com.rosybot.services.RosyCustomerService;

public class Rosycustomerapihandler implements RequestHandler<Object, String> {

	private LambdaLogger ll;
	
    @Override
    public String handleRequest(Object input, Context context) {
    	ll = context.getLogger();
		// determine type of post based on json object's first field.
		// if "ro" then it's the first post to get the order and populate it.
		// if it's a full order, then treat it like a post and update database
	//	ll.log("Input: " + input);
   
		// first convert input object to JSON
		ObjectMapper mapperInput = new ObjectMapper();
		String inputObjectAsJsonString = null;  

		try { 
			inputObjectAsJsonString = mapperInput.writeValueAsString(input);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	//	ll.log(inputObjectAsJsonString);
		
		
		// now convert json string to HashMap
		HashMap<String, Object> inputMap = new HashMap<String, Object>();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};
		InputStream is = null;

		try {

			is = new ByteArrayInputStream(inputObjectAsJsonString.getBytes("UTF-8"));
			inputMap = mapperInput.readValue(is, typeRef);
			
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) { 
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block 
			e.printStackTrace();
		}
		  

		int createdCustId = 0;
		
		if (inputMap.size() == 1) {

			// String orderId = inputparms[1].substring(0,
			// inputparms[1].length() - 1);

			PRODRosyCustomerService rcs = new PRODRosyCustomerService();
			createdCustId = rcs.createCustomerByPhone(inputMap.get("customerPhone").toString());

			
		} else {
			createdCustId = -1;
		}

        // TODO: implement your handler
        return StringUtils.fromInteger(createdCustId);
    }

}
