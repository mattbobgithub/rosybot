package com.rosythebot.rosyintro;

import com.rosybot.models.*;
import com.rosybot.models.Enums.RosyOrderStatus;
import com.rosybot.services.*;


import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.WordUtils;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;

public class LexRosyIntroValidation implements RequestHandler<Object, Object> {


	private static final String F1_SUPPORT_NUMBER = System.getenv("F1_SUPPORT_NUMBER");
	private static final String GR_SUPPORT_NUMBER = System.getenv("GR_SUPPORT_NUMBER");
	
	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes;

	@Override
	public Object handleRequest(Object input, Context context) {
		ll = context.getLogger();

/*		Map<String, String> sessionAttributes = new HashMap<String, String>();

		lexReq = LexRequest.fromLexObject(input);
		ll.log("CURRENT INTENT:" + lexReq.getCurrentIntent().getName() + lexReq.toString());
		
		String customerPhone = lexReq.getUserId();
		customerPhone = customerPhone.replaceAll("[^\\d]", "");
		
		
	
		if (lexReq.getInputTranscript().toLowerCase().equals("reset")) {
			lexReq.setSessionAttributes(null);
			
		}

		// transform "y" to "ok" on this intent only
		if (lexReq.getInputTranscript().toLowerCase().equals("y")) {
			lexReq.setInputTranscript("ok");
		}

		if (lexReq.getSessionAttributes() != null) {
			ll.log("RECEIVED LEX REQ SESSION ATTRIBUTES:");
			for (Map.Entry<String, String> entry : lexReq.getSessionAttributes().entrySet()) {
				ll.log(entry.getKey() + " : " + entry.getValue());
			}

			sessionAttributes.putAll(lexReq.getSessionAttributes());
		}
		
    				
		// only get customer data once, this runs after input of confirmation
		// "ok" and we don't need it to

		String introMessage = "";

		if (!sessionAttributes.containsKey("s_incomingPhoneNumber")) {

	
			sessionAttributes.put("s_incomingPhoneNumber", customerPhone);

			// check if customer exists already
			if (!sessionAttributes.containsKey("existingCustomerFlag")) {

				sessionAttributes.put("existingCustomerFlag", "false");
				RosyCustomerService rosycustsvc = new RosyCustomerService();
				RosyCustomer existingCustomer = rosycustsvc.getRosyCustomerByPhone(customerPhone);

				if (existingCustomer != null && !StringUtils.isNullOrEmpty(existingCustomer.getFirstName())) {
					sessionAttributes.put("existingCustomerFirstName", existingCustomer.getFirstName());
					sessionAttributes.put("existingCustomerLastName", existingCustomer.getLastName());
					sessionAttributes.put("existingCustomerEmail", existingCustomer.getEmail());

					sessionAttributes.put("existingCustomerId", StringUtils.fromInteger(existingCustomer.getId()));

					sessionAttributes.put("existingCustomerFlag", "true");
					
					
		
					
					
					
					
					
					introMessage = "Hi " + WordUtils.capitalize(existingCustomer.getFirstName()) + ",";

					// check for existing order:
					RosyOrder foundRosyOrder = new RosyOrder();
					RosyOrderService ros = new RosyOrderService();
					foundRosyOrder = ros.getRosyOrder(StringUtils.fromInteger(existingCustomer.getId()), "CustomerId");
					if (foundRosyOrder != null) {

						sessionAttributes.put("existingRecipientFullName", foundRosyOrder.getRecipientFullName());

						introMessage = introMessage + " your order(#" + foundRosyOrder.getRosyOrderId() + ") to " + foundRosyOrder.getRecipientFullName();

						DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy");
						DateTime deliveryDt = formatter.parseDateTime(foundRosyOrder.getDeliveryDate());
						LocalDate deliveryDateLocal = deliveryDt.toLocalDate();

						DateTime todayDt = DateTime.now();

						LocalDate todayDateLocal = todayDt.toLocalDate();

						if (foundRosyOrder.getRosyOrderStatus().equals(RosyOrderStatus.DRAFT)) {
							introMessage = introMessage + " has not been completed, "
									+ "click link to checkout: "
									+ "http://rosybot.com/?ro=" + foundRosyOrder.getId().toString()									
									+ ", or type 'ok' to restart an order. ";
						} else {

							if (deliveryDateLocal.isBefore(todayDateLocal)) {
								introMessage = introMessage + " was delivered on " + foundRosyOrder.getDeliveryDate()
										+ ".";
							} else {
								introMessage = introMessage + " is scheduled for delivery on "
										+ foundRosyOrder.getDeliveryDate();
								if (foundRosyOrder.getRosyOrderId().substring(0, 2).equals("GR")) {
									introMessage = introMessage
											+ ".  If you have an issue you can call GlobalRose support at "
											+ GR_SUPPORT_NUMBER;
								} else {
									introMessage = introMessage
											+ ".  If you have an issue you can call FloristOne support at "
											+ F1_SUPPORT_NUMBER;
								}
							}

							introMessage = introMessage
									+ ". If you'd like to create a new order, type 1 to share a contact or 2 to enter address manually. ";

						}
					} else {

						introMessage = introMessage
								+ ". If you'd like to create a new order, type 1 to share a contact or 2 to enter address manually. ";
					}
				} else {

					//new customer, record to database prior to anything. 
					RosyCustomerService rcs = new RosyCustomerService();
					int createdCustId = rcs.createCustomerByPhone(customerPhone);
					sessionAttributes.put("existingCustomerId", StringUtils.fromInteger(createdCustId));
					
					
					introMessage = introMessage + "Hi, I'm Rosy, ";
					introMessage = introMessage + "I can help you get flowers by asking some questions.  Wait for my responses please. ";
					introMessage = introMessage + "If I get lost, or you want to start over, just type 'reset',  ok? ";
					//introMessage = introMessage
					//		+ "Also, turn on your data on if you'd like to see the pictures of my offers, ok?";
				}
			}
		}
		sessionAttributes.put("introMessage", introMessage);

		*/
		lexRes = new LexResponse();
		DialogActionDelegate da = new DialogActionDelegate();
		lexRes.setDialogAction(da);

		lexRes.setSessionAttributes(sessionAttributes);

		return lexRes;

	}

}
