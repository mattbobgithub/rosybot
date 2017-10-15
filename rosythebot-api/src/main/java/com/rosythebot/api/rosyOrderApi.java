package com.rosythebot.api;

import com.rosybot.models.*;
import com.rosybot.services.*;
import com.rosybot.models.Enums;
import com.rosybot.models.Enums.RosyOrderStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.InputMap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import com.smartystreets.api.StaticCredentials;
import com.smartystreets.api.exceptions.SmartyException;
import com.smartystreets.api.us_street.*;
import com.smartystreets.api.us_street.ClientBuilder;

public class rosyOrderApi implements RequestHandler<Object, String> {

	private static final String TWILIO_ACCT_SID = System.getenv("TWILIO_ACCT_SID");
	private static final String TWILIO_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
	private static final String TWILIO_FROM_NUMBER = System.getenv("TWILIO_FROM_NUMBER");
	private static final String F1_SUPPORT_NUMBER = System.getenv("F1_SUPPORT_NUMBER");
	private static final String GR_SUPPORT_NUMBER = System.getenv("GR_SUPPORT_NUMBER");

	private static LambdaLogger ll;
 
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
			ll.log(inputObjectAsJsonString);
		} catch (JsonProcessingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}  

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

		RosyOrderService ros = new RosyOrderService();

		String inputId = (String) inputMap.get("id");

		// if only 1 parm, then it's a post to get the rosy order by id
		if (inputMap.size() == 1) {

			// inputparms[1].length() - 1);
			RosyOrder ro = ros.getRosyOrder(inputId, "OrderId");
			ObjectMapper mapper = new ObjectMapper();

			String jsonInString = null;
			ro.setCreateDate(null);
			ro.setModifiedDate(null);	

			   
			try {
				jsonInString = mapper.writeValueAsString(ro);

			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return jsonInString;
		} else {

			RosyOrder roPriorToUpdate = ros.getRosyOrder(inputMap.get("id").toString(), "OrderId");
			RosyOrder roUpdate = null;
			
			if (inputMap.size() == 3 && inputMap.get("rosyOrderStatus").equals("PAID")) {

				// check if order has just been fulfilled by looking at previous
				// status
				ll.log("ROSY API- fulfillment request received");

				if (roPriorToUpdate.getRosyOrderStatus().toString().equals("DRAFT")) {
					ll.log(" ..moving on to fulfill ord ");
					
					
					// fulfill order here
					String fulfillmentId = ros.fulfillGlobalRoseOrder(roPriorToUpdate.getId().toString());

					/// now update order again - did this in 2 parts in case fulfillment fails,
					// we still have the order, and no orders should have "PAID" status unless there was a problem
						
			 			roUpdate = ros.updateRosyOrderToFulfillment(roPriorToUpdate.getId().toString(), fulfillmentId, inputMap.get("paymentId").toString());
							

					// now send confirmation text back to twilio

					String confirmationSmsMessageText = "Your Order# is " + roUpdate.getRosyOrderId()
							+ "  If you have any questions contact GlobalRose support at " + GR_SUPPORT_NUMBER;

					// Find your Account Sid and Token at twilio.com/user/account

					Twilio.init(TWILIO_ACCT_SID, TWILIO_AUTH_TOKEN);

					Message message = Message.creator(new PhoneNumber(roUpdate.getCustomerPhone()),
							new PhoneNumber(TWILIO_FROM_NUMBER), confirmationSmsMessageText).create();

					ll.log("Order UPDATED SUCCESSFULLY to fulfilled, text sent  - orderid:" + roUpdate.getId());
				}
			}else{

		    
			int rosyCustomerId = roPriorToUpdate.getRosyCustomerId();
			String rosyCustomerIdStr = StringUtils.fromInteger(rosyCustomerId);
			rosyCustomerIdStr = ",\"rosyCustomerId\":"+ rosyCustomerIdStr +"}";		
			inputObjectAsJsonString = inputObjectAsJsonString.replace("}",rosyCustomerIdStr);
  
			// update whole order json.
			roUpdate = ros.updateRosyOrderFromJSON(inputObjectAsJsonString);

			ll.log("Order and Customer UPDATED SUCCESSFULLY." + roUpdate.getId());
			}
	
			
			
			//format output

			ObjectMapper mapper = new ObjectMapper();
			String jsonOutString = null;
			try {
				jsonOutString = mapper.writeValueAsString(roUpdate);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			return jsonOutString;

		}
	}
	
	
	
	
	
	
	
	
	
}
