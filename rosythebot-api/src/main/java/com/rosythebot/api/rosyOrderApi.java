package com.rosythebot.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
import com.rosythebot.models.Enums.RosyOrderStatus;
import com.rosythebot.models.RosyOrder;
import com.rosythebot.services.F1Request;
import com.rosythebot.services.RosyOrderService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class rosyOrderApi implements RequestHandler<Object, String> {

	private static final String TWILIO_ACCT_SID = System.getenv("TWILIO_ACCT_SID");
	private static final String TWILIO_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
	private static final String TWILIO_FROM_NUMBER = System.getenv("TWILIO_FROM_NUMBER");
	private static final String F1_SUPPORT_NUMBER = System.getenv("F1_SUPPORT_NUMBER");

	private static LambdaLogger ll;

	@Override
	public String handleRequest(Object input, Context context) {
		ll = context.getLogger();
		// determine type of post based on json object's first field.
		// if "ro" then it's the first post to get the order and populate it.
		// if it's a full order, then treat it like a post and update database
		ll.log("Input: " + input);

		// first convert input object to JSON
		ObjectMapper mapperInput = new ObjectMapper();
		String inputObjectAsJsonString = null;

		try {
			inputObjectAsJsonString = mapperInput.writeValueAsString(input);
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
			System.out.println("Got: " + inputMap);
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

			// String orderId = inputparms[1].substring(0,
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
			boolean fulfillmentRequest = false;

			RosyOrder roPriorToUpdate = ros.getRosyOrder(inputMap.get("id").toString(), "OrderId");

			if (inputMap.size() == 3 && inputMap.get("rosyOrderStatus").equals("PAID")) {

				// check if order has just been fulfilled by looking at previous
				// status

				if (roPriorToUpdate.getRosyOrderStatus().toString().equals("DRAFT")) {
					fulfillmentRequest = true;
				}
			}
			ll.log(inputObjectAsJsonString);
			
			int rosyCustomerId = roPriorToUpdate.getRosyCustomerId();
			String rosyCustomerIdStr = StringUtils.fromInteger(rosyCustomerId);
			rosyCustomerIdStr = ",\"rosyCustomerId\":"+ rosyCustomerIdStr +"}";
		
			inputObjectAsJsonString = inputObjectAsJsonString.replace("}",rosyCustomerIdStr);
			ll.log(inputObjectAsJsonString);
			// update whole order json.
			RosyOrder roUpdate = ros.updateRosyOrder(inputObjectAsJsonString);

			ObjectMapper mapper = new ObjectMapper();
			String jsonInString = null;
			try {
				jsonInString = mapper.writeValueAsString(roUpdate);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ll.log("fulfillmentRequest: " + fulfillmentRequest);
			// fulfill order if necessary
			if (fulfillmentRequest) {

				// fulfill order here
				String fulfillmentId = ros.fulfillOrder(roUpdate);

				/// now update order again - did this in 2 parts in case
				/// fulfillment fails,
				// we still have the order, and no orders should have "PAID"
				/// status unless there was a problem
				roUpdate.setRosyOrderStatus(RosyOrderStatus.FULFILLED);
				roUpdate.setRosyOrderId(fulfillmentId);
				roUpdate.setCreateDate(null);
				roUpdate.setModifiedDate(null);
				String orderAsJson = null;
				try {
					orderAsJson = mapperInput.writeValueAsString(roUpdate);
					roUpdate = ros.updateRosyOrder(orderAsJson);
					// set output json with updated order
					jsonInString = orderAsJson;
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// now send confirmation text back to twilio

				String confirmationSmsMessageText = "Your Order# is " + roUpdate.getRosyOrderId()
						+ "  If you have any questions contact FloristOne support at " + F1_SUPPORT_NUMBER;

				// Find your Account Sid and Token at twilio.com/user/account

				Twilio.init(TWILIO_ACCT_SID, TWILIO_AUTH_TOKEN);

				Message message = Message.creator(new PhoneNumber(roUpdate.getCustomerPhone()),
						new PhoneNumber(TWILIO_FROM_NUMBER), confirmationSmsMessageText).create();
				

				System.out.println(message.getSid());

			}

			ll.log("returning updated order to client");
			ll.log(jsonInString);
			return jsonInString;

		}
	}
}