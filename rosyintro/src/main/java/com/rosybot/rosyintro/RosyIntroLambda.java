package com.rosybot.rosyintro;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.rosybot.models.CurrentIntent;
import com.rosybot.models.DialogActionConfirmIntent;
import com.rosybot.models.DialogActionDelegate;
import com.rosybot.models.DialogActionElicitSlot;
import com.rosybot.models.GenericAttachment;
import com.rosybot.models.LexRequest;
import com.rosybot.models.LexResponse;
import com.rosybot.models.Message;
import com.rosybot.models.ResponseCard;
import com.rosybot.models.RosyCustomer;
import com.rosybot.models.RosyOrder;
import com.rosybot.models.Enums.RosyOrderStatus;
import com.rosybot.services.RosyCustomerService;
import com.rosybot.services.RosyOrderService;

public class RosyIntroLambda implements RequestHandler<Object, Object> {

	private static final String F1_SUPPORT_NUMBER = System.getenv("F1_SUPPORT_NUMBER");
	private static final String GR_SUPPORT_NUMBER = System.getenv("GR_SUPPORT_NUMBER");

	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();
	private Map<String, String> sessionAttributes = new HashMap<String, String>();

	private Map<String, String> inputSlots = new HashMap<String, String>();

	@Override
	public Object handleRequest(Object input, Context context) {

		ll = context.getLogger();

		lexReq = LexRequest.fromLexObject(input);

		ll.log("CURRENT INTENT: " + lexReq.getCurrentIntent().getName() + "     request obj: " + lexReq.toString());

		CurrentIntent currentIntent = lexReq.getCurrentIntent();
		if (null == currentIntent.getSlots() || currentIntent.getSlots().isEmpty()) {
			// do nothing
			// ll.log("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXcurrent
			// intent slots is empty or null");
		} else {
			inputSlots.putAll(currentIntent.getSlots());
		}

		String customerPhone = lexReq.getUserId();
		customerPhone = customerPhone.replaceAll("[^\\d]", "");


		ll.log("received LEXREQ sessionAttributes:");
		for (Map.Entry<String, String> entry : lexReq.getSessionAttributes().entrySet()) {
			ll.log(entry.getKey() + " : " + entry.getValue());
		}

		if (lexReq.getInputTranscript().toLowerCase().equals("reset")
				|| lexReq.getInputTranscript().toLowerCase().equals("roses")) {
			inputSlots.clear();

		}

		sessionAttributes.clear();
		sessionAttributes.putAll(lexReq.getSessionAttributes());

		// transform "y" to "ok" on this intent only
		if (lexReq.getInputTranscript().toLowerCase().equals("y")) {
			lexReq.setInputTranscript("ok");
		}

	

		DialogActionElicitSlot da = new DialogActionElicitSlot();

		if (StringUtils.isNullOrEmpty(inputSlots.get("recipientRoute"))) {
			ll.log("recipient route is null, must be first time through.");
			// only get customer data once, this runs after input of
			// confirmation
			// "ok" and we don't need it to

			String introMessage = "";
			introMessage = introMessage + "Hi, I'm ROSY! ";
			introMessage = introMessage + "I can send a dozen red roses to someone tomorrow for $40 (or you can "
					+ "pick different colors and amounts too!). " + "Press 1 to share a contact for delivery, "
					+ "press 2 to manually add the recipient.";

			if (!sessionAttributes.containsKey("s_incomingPhoneNumber")) {
				ll.log("incoming phone session attribute not set yet, must be first time through. ...setting it now.");

				sessionAttributes.put("s_incomingPhoneNumber", customerPhone);

				// check if customer exists already
				if (!sessionAttributes.containsKey("existingCustomerFlag")) {
					ll.log("existing customer flag not set yet.  ....setting it now");

					sessionAttributes.put("existingCustomerFlag", "false");
					RosyCustomerService rosycustsvc = new RosyCustomerService();
					RosyCustomer existingCustomer = rosycustsvc.getRosyCustomerByPhone(customerPhone);

					if (existingCustomer != null && !StringUtils.isNullOrEmpty(existingCustomer.getFirstName())) {
						sessionAttributes.put("existingCustomerFirstName", existingCustomer.getFirstName());
						sessionAttributes.put("existingCustomerLastName", existingCustomer.getLastName());
						sessionAttributes.put("s_customerFullName",
								existingCustomer.getFirstName() + " " + existingCustomer.getLastName());
						sessionAttributes.put("existingCustomerEmail", existingCustomer.getEmail());

						sessionAttributes.put("existingCustomerId", StringUtils.fromInteger(existingCustomer.getId()));

						sessionAttributes.put("existingCustomerFlag", "true");
						// blank intro message and re-populate
						introMessage = "";
						introMessage = "Hi " + WordUtils.capitalize(existingCustomer.getFirstName()) + ",";

						// check for existing order:
						RosyOrder foundRosyOrder = new RosyOrder();
						RosyOrderService ros = new RosyOrderService();
						foundRosyOrder = ros.getRosyOrder(StringUtils.fromInteger(existingCustomer.getId()),
								"CustomerId");
						if (foundRosyOrder != null) {

							sessionAttributes.put("existingRecipientFullName", foundRosyOrder.getRecipientFullName());

							introMessage = introMessage + " your order";
									if(!StringUtils.isNullOrEmpty(foundRosyOrder.getRosyOrderId())){
										introMessage = introMessage + "(#" + foundRosyOrder.getRosyOrderId() + ")";
									}
									
									introMessage = introMessage	+ " to "	+ foundRosyOrder.getRecipientFullName();

							DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy");
							DateTime deliveryDt = formatter.parseDateTime(foundRosyOrder.getDeliveryDate());
							LocalDate deliveryDateLocal = deliveryDt.toLocalDate();

							DateTime todayDt = DateTime.now();

							LocalDate todayDateLocal = todayDt.toLocalDate();

							if (foundRosyOrder.getRosyOrderStatus().equals(RosyOrderStatus.DRAFT)) {
								introMessage = introMessage + " has not been completed, " + "click link to checkout: "
										+ "http://rosybot.com/?ro=" + foundRosyOrder.getId().toString();
								introMessage = introMessage
										+ ". If you'd like to create a new order, type 1 to share a contact or 2 to enter address manually. ";
							} else {

								if (deliveryDateLocal.isBefore(todayDateLocal)) {
									introMessage = introMessage + " was delivered on "
											+ foundRosyOrder.getDeliveryDate() + ".";
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

						// new customer, record to database prior to anything.
						RosyCustomerService rcs = new RosyCustomerService();
						int createdCustId = rcs.createCustomerByPhone(customerPhone);
						sessionAttributes.put("existingCustomerId", StringUtils.fromInteger(createdCustId));

					}
				}
			}

			ll.log("intro message:" + introMessage);
			da = new DialogActionElicitSlot();
			da.setIntentName("rosyBotIntro");
			da.setMessage(new Message(introMessage));

			inputSlots.put("recipientRoute", null);

			da.setSlots(inputSlots);
			da.setSlotToElicit("recipientRoute");

			ll.log("prompt for recipient route action");
		} else {

			switch (inputSlots.get("recipientRoute")) {
			case "1":
				// get vcard
				da = getVCardDA();

				break;
			case "2":
				// get manually
				da = getFastDeliveryAddressDA();
				break;
			default:

				break;
			}

		}

		lexRes.setDialogAction(da);
		lexRes.setSessionAttributes(sessionAttributes);

		ll.log("SENDING THESE SESSION ATTRIBUTES:");
		for (Map.Entry<String, String> entry : lexRes.getSessionAttributes().entrySet()) {
			ll.log(entry.getKey() + " : " + entry.getValue());
		}

		return lexRes;
	}

	private DialogActionElicitSlot getVCardDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		// get by sharing contact
		da.setIntentName("rosyBotgetVCard");
		inputSlots.clear();
		inputSlots.put("deliveryAddressConfirmation", null);
		da.setSlots(inputSlots);
		da.setSlotToElicit("deliveryAddressConfirmation");
		String vCardPrompt = "OK, save this Rosy contact, then share the contact of the recipient with Rosy in a text message (we don't contact the recipient).";
		da.setMessage(new Message(vCardPrompt));

		// create response card
		ResponseCard rc = new ResponseCard();
		rc.setContentType("application/vnd.amazonaws.card.generic");
		GenericAttachment ga = new GenericAttachment();
		ga.setTitle("get contact");
		ga.setSubTitle("get contact");
		ga.setImageUrl("https://s3.amazonaws.com/rosybot.com/images/ROSYBOT.vcf");
		Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
		gas.add(ga);
		rc.setGenericAttachments(gas);

		da.setResponseCard(rc);
		return da;

	}

	private DialogActionElicitSlot getFastDeliveryAddressDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetDeliveryAddress");
		da.setMessage(new Message("Enter full name of recipient:"));
		setFastAddressInputSlots();
		da.setSlots(inputSlots);
		da.setSlotToElicit("recipientFullName");

		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private void setFastAddressInputSlots() {
		// put slots into session variables
		// remove slots from previous intent ??
		inputSlots.clear();

		inputSlots.put("recipientFullName", "x");
		inputSlots.put("recipientPhone", "x");
		inputSlots.put("streetAddress", "x");
		inputSlots.put("aptNo", "x");
		inputSlots.put("postalCode", "x");
		inputSlots.put("addressConfirmed", "x");

	}
}
