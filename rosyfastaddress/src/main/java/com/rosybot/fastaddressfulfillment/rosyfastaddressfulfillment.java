package com.rosybot.fastaddressfulfillment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.rosybot.models.CurrentIntent;
import com.rosybot.models.DialogActionElicitSlot;
import com.rosybot.models.GenericAttachment;
import com.rosybot.models.LexRequest;
import com.rosybot.models.LexResponse;
import com.rosybot.models.Message;
import com.rosybot.models.RecipientAddress;
import com.rosybot.models.ResponseCard;
import com.rosybot.services.RecipientAddressService;

public class rosyfastaddressfulfillment implements RequestHandler<Object, Object> {

	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();

	private static Map<String, String> sessionAttributes = new HashMap<String, String>();
	private static Map<String, String> inputSlots = new HashMap<String, String>();
	private static CurrentIntent currentIntent;

	@Override
	public Object handleRequest(Object input, Context context) {
		ll = context.getLogger();   
		ll.log("Input: " + input);
		lexReq = LexRequest.fromLexObject(input);
		ll.log("CURRENT INTENT: " + lexReq.getCurrentIntent().getName() + "     request obj: " + lexReq.toString());

		CurrentIntent currentIntent = lexReq.getCurrentIntent();

		inputSlots.putAll(currentIntent.getSlots());
		ll.log("INPUT SLOTS:" + inputSlots.toString());

		if (lexReq.getSessionAttributes() != null) {
			// print all session attributes
			ll.log("RECEIVED ALL SESSION ATTRIBUTES:");
			for (Map.Entry<String, String> entry : sessionAttributes.entrySet()) {
				ll.log(entry.getKey() + " : " + entry.getValue());
			}

			sessionAttributes.clear();
			sessionAttributes.putAll(lexReq.getSessionAttributes());
		}

		String customerPhone = lexReq.getUserId();
		customerPhone = customerPhone.replaceAll("[^\\d]", "");

		ll.log("input transcript:" + lexReq.getInputTranscript());

	//fix null aptNo
		if(StringUtils.isNullOrEmpty(inputSlots.get("aptNo")) && lexReq.getInputTranscript().toLowerCase().equals("n")){
			inputSlots.put("aptNo", "n");
		}

		// check for session aptNo because lex is setting to null for some lame
		// reason
		if (sessionAttributes.containsKey("s_recipientFullName") && !StringUtils.isNullOrEmpty(sessionAttributes.get("s_recipientFullName"))) {
			inputSlots.put("recipientFullName", sessionAttributes.get("s_recipientFullName"));
		}
		if (sessionAttributes.containsKey("s_recipientPhone") && !StringUtils.isNullOrEmpty(sessionAttributes.get("s_recipientPhone"))) {
			inputSlots.put("recipientPhone", sessionAttributes.get("s_recipientPhone"));
		}
		if (sessionAttributes.containsKey("s_streetAddress1") && !StringUtils.isNullOrEmpty(sessionAttributes.get("s_streetAddress1"))) {
			inputSlots.put("streetAddress", sessionAttributes.get("s_streetAddress1"));
		}
		if (sessionAttributes.containsKey("s_streetAddress2") && !StringUtils.isNullOrEmpty(sessionAttributes.get("s_streetAddress2"))) {
			inputSlots.put("aptNo", sessionAttributes.get("s_streetAddress2"));
		}
		if (sessionAttributes.containsKey("s_postalCode") && !StringUtils.isNullOrEmpty(sessionAttributes.get("s_postalCode"))) {
			inputSlots.put("postalCode", sessionAttributes.get("s_postalCode"));
		}
		
		String recipientFullName = inputSlots.get("recipientFullName");
		String recipientPhone = inputSlots.get("recipientPhone");
		String streetAddress = inputSlots.get("streetAddress");
		String aptNo = inputSlots.get("aptNo");
		String postalCode = inputSlots.get("postalCode");
		String addressConfirmed = inputSlots.get("addressConfirmed").toLowerCase();

		ll.log("streetAddress:" + streetAddress);
		ll.log("aptNo:" + aptNo);
		ll.log("postalCode:" + postalCode);
		ll.log("addressConfirmed:" + addressConfirmed);

		DialogActionElicitSlot da = new DialogActionElicitSlot();
		if (recipientFullName.equals("x")) {
			sessionAttributes.remove("s_recipientFullName");
			da = getDeliveryAddressDA("recipientFullName");
		} else {
			sessionAttributes.put("s_recipientFullName", recipientFullName);
			if (recipientPhone.equals("x")) {
				sessionAttributes.remove("s_recipientPhone");
				da = getDeliveryAddressDA("recipientPhone");
			} else {

				sessionAttributes.put("s_recipientPhone", recipientPhone);
				if (streetAddress.equals("x")) {
					sessionAttributes.remove("s_streetAddress1");
					da = getDeliveryAddressDA("streetAddress");
				} else {
					// assign street address to session
					sessionAttributes.put("s_streetAddress1", streetAddress);
					if (aptNo.equals("x")) {
						sessionAttributes.remove("s_streetAddress2");
						da = getDeliveryAddressDA("aptNo");

					} else {
						// assign aptNo to session here only if not "n",
						// otherwise blank
						if (aptNo.equals("n")) {
							sessionAttributes.put("s_streetAddress2", "");
						} else {
							sessionAttributes.put("s_streetAddress2", aptNo);
						}

						if (postalCode.equals("x")) {
							sessionAttributes.remove("s_postalCode");
							sessionAttributes.remove("s_city");
							sessionAttributes.remove("s_state");
							sessionAttributes.remove("s_country");
							
							da = getDeliveryAddressDA("postalCode");
						} else {
							// assign postal code to session and call google
							// maps api
							sessionAttributes.put("s_postalCode", postalCode);

							if (addressConfirmed.equals("x")) {

								RecipientAddressService ras = new RecipientAddressService();
								RecipientAddress ra = ras.fromStreetAddressAndPostal(streetAddress + " " + aptNo,
										postalCode);
								String derivedAddressForConfirmation = ra.getStreetAddress1() + ", " + ra.getCity()
										+ ", " + ra.getState() + " " + ra.getPostalCode();

								derivedAddressForConfirmation = " NAME:" + recipientFullName.trim() + ",";
								derivedAddressForConfirmation = derivedAddressForConfirmation + " ADDRESS:";

								if (!StringUtils.isNullOrEmpty(streetAddress)) {
									sessionAttributes.put("s_streetAddress1", streetAddress);
									derivedAddressForConfirmation = derivedAddressForConfirmation + streetAddress + ", ";
										
								}
								if (!StringUtils.isNullOrEmpty(aptNo)) {
									sessionAttributes.put("s_streetAddress2", aptNo);
									if(!aptNo.toLowerCase().equals("n")){
									derivedAddressForConfirmation = derivedAddressForConfirmation + aptNo + ", ";
									}
								}
								if (!StringUtils.isNullOrEmpty(ra.getCity())) {
									sessionAttributes.put("s_city", ra.getCity());
									derivedAddressForConfirmation = derivedAddressForConfirmation + ra.getCity() + ", ";
								}
								if (!StringUtils.isNullOrEmpty(ra.getState())) {
									sessionAttributes.put("s_state", ra.getState());
									derivedAddressForConfirmation = derivedAddressForConfirmation + ra.getState() + " ";
								}
								if (!StringUtils.isNullOrEmpty(ra.getPostalCode())) {
									sessionAttributes.put("s_postalCode", ra.getPostalCode());
									derivedAddressForConfirmation = derivedAddressForConfirmation + ra.getPostalCode();
								}

								if (StringUtils.isNullOrEmpty(ra.getCountry()) || ra.getCountry().equals("USA")) {
									sessionAttributes.put("s_country", "US");
								}

								derivedAddressForConfirmation = derivedAddressForConfirmation + " PHONE:"
										+ recipientPhone;

								sessionAttributes.put("s_recipientPhone", recipientPhone);
								sessionAttributes.put("s_recipientFullName", recipientFullName);

								ll.log(derivedAddressForConfirmation);

								da = getDeliveryAddressConfirmationDA(derivedAddressForConfirmation);

							} else {

								String messageIntro = "";
								if (addressConfirmed.equals("n")) {
									// address not confirmed, retry or move on
									// to manual
									// or just do it at checkout??
									ll.log("address not confirmed");
									// blank out session address
									sessionAttributes.remove("s_streetAddress1");
									sessionAttributes.remove("s_streetAddress2");
									sessionAttributes.remove("s_city");
									sessionAttributes.remove("s_state");
									sessionAttributes.remove("s_postalCode");
									sessionAttributes.remove("s_country");

									messageIntro = "That's ok, we'll get the address later, let's pick the flowers (no pun intended). ";

								} else {
									// address confirmed, move on
									ll.log("address confirmed");

								}
								da = getMainOfferDA(messageIntro);

							}

						}
					}

				}
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

	private DialogActionElicitSlot getDeliveryAddressDA(String addressField) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		String addyDescr = addressField;
		if (addressField.equals("recipientFullName")) {
			addyDescr = "full name of the recipient:";
		}
		if (addressField.equals("recipientPhone")) {
			addyDescr = "phone number of the recipient (for delivery use only, we will not contact them):";
		}
		if (addressField.equals("postalCode")) {
			addyDescr = "postal code of delivery (US only):";
		}
		if (addressField.equals("streetAddress")) {
			addyDescr = "street number and name of delivery address [without apt or suite no]:";
		}
		if (addressField.equals("aptNo")) {
			addyDescr = "apt/suite/unit # of delivery [or n if not applicable]:";
		}

		da.setIntentName("GetDeliveryAddress");
		da.setMessage(new Message("Enter only " + addyDescr));
		setInputSlots();
		da.setSlots(inputSlots);
		da.setSlotToElicit(addressField);

		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());
		return da;
	}

	private DialogActionElicitSlot getDeliveryAddressConfirmationDA(String fullAddress) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("GetDeliveryAddress");
		da.setMessage(new Message("Look good? [y or n]:" + fullAddress));

		setInputSlots();
		da.setSlots(inputSlots);
		da.setSlotToElicit("addressConfirmed");

		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private DialogActionElicitSlot getMainOfferDA(String messageIntro) {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyBotGetProductSelection");
		da.setMessage(new Message(messageIntro
				+ "Our best offer is a dozen of the freshest premium red roses delivered next day for $40 ['y' to accept, or 'more' for more color and amount options]:"));
		// setInputSlots(da.getIntentName());
		inputSlots.clear();
		inputSlots.put("productRoute", "x");
		inputSlots.put("amountOption", "x");
		inputSlots.put("colorOption", "x");
		inputSlots.put("productConfirmed", "x");
		da.setSlots(inputSlots);
		da.setSlotToElicit("productRoute");

		// add response card with main offer
		// create response card
		ResponseCard rc = new ResponseCard();
		rc.setContentType("application/vnd.amazonaws.card.generic");
		GenericAttachment ga = new GenericAttachment();
		ga.setTitle("dozen red roses offer");
		ga.setSubTitle("dozen red roses offer");
		ga.setImageUrl("https://s3.amazonaws.com/rosybot.com/images/products/mainOffer.jpg");
		Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
		gas.add(ga);
		rc.setGenericAttachments(gas);

		da.setResponseCard(rc);

		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private void setInputSlots() {
		// put slots into session variables

		String rfn = "";
		String rp = "";
		String strAddy1 = "";
		String strAddy2 = "";
		String postalCode = "";
		String addressConfirmed = "";

		// add or keep slots for existing intent
		
		if (!inputSlots.containsKey("recipientFullName")) {
			rfn = null;
		} else {
			// move to sessionAttributes
			rfn = inputSlots.get("recipientFullName");
		}
		if (!inputSlots.containsKey("recipientPhone")) {
			rp = null;
		} else {
			// move to sessionAttributes
			rp = inputSlots.get("recipientPhone");
		}
		
		if (!inputSlots.containsKey("streetAddress")) {
			strAddy1 = null;
		} else {
			// move to sessionAttributes
			strAddy1 = inputSlots.get("streetAddress");
		}
		if (!inputSlots.containsKey("aptNo")) {
			strAddy2 = null;
		} else {
			strAddy2 = inputSlots.get("aptNo");

		}
		if (!inputSlots.containsKey("postalCode")) {
			postalCode = null;
		} else {
			postalCode = inputSlots.get("postalCode");

		}
		if (!inputSlots.containsKey("addressConfirmed")) {
			addressConfirmed = null;
		} else {

			addressConfirmed = inputSlots.get("addressConfirmed");
		}

		inputSlots.clear();
		
		
		if (!StringUtils.isNullOrEmpty(rfn)) {
			inputSlots.put("recipientFullName", rfn);
		} else {

			inputSlots.put("recipientFullName", "x");
		}
		
		if (!StringUtils.isNullOrEmpty(rp)) {
			inputSlots.put("recipientPhone", rp);
		} else {

			inputSlots.put("recipientPhone", "x");
		}
		

		if (!StringUtils.isNullOrEmpty(strAddy1)) {
			inputSlots.put("streetAddress", strAddy1);
		} else {

			inputSlots.put("streetAddress", "x");
		}
		if (!StringUtils.isNullOrEmpty(strAddy2)) {

			inputSlots.put("aptNo", strAddy2);
		} else {
			inputSlots.put("aptNo", "x");
		}
		if (!StringUtils.isNullOrEmpty(postalCode)) {
			inputSlots.put("postalCode", postalCode);
		} else {
			inputSlots.put("postalCode", "x");
		}
		if (!StringUtils.isNullOrEmpty(addressConfirmed)) {
			inputSlots.put("addressConfirmed", addressConfirmed);
		} else {
			inputSlots.put("addressConfirmed", "x");
		}
	}

}
