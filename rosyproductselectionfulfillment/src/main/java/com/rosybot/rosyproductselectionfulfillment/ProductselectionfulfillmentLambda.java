package com.rosybot.rosyproductselectionfulfillment;

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
import com.rosybot.models.ResponseCard;

public class ProductselectionfulfillmentLambda implements RequestHandler<Object, Object> {

	private LambdaLogger ll;
	private LexRequest lexReq;
	private LexResponse lexRes = new LexResponse();

	private static Map<String, String> sessionAttributes = new HashMap<String, String>();

    private static Map<String, String> inputSlots = new HashMap<String, String>();
	private static CurrentIntent currentIntent;

	@Override
	public Object handleRequest(Object input, Context context) {
		ll = context.getLogger();
			lexReq = LexRequest.fromLexObject(input);
			

		ll.log("CURRENT INTENT: " + lexReq.getCurrentIntent().getName());

		CurrentIntent currentIntent = lexReq.getCurrentIntent();
		
		inputSlots.clear();
		inputSlots.putAll(currentIntent.getSlots());


		if (lexReq.getSessionAttributes() != null) {
			sessionAttributes.clear();
			sessionAttributes.putAll(lexReq.getSessionAttributes());
		}
		
//		ll.log("PRINTING ALL SESSION ATTRIBUTES:");
//		for (Map.Entry<String, String> entry : sessionAttributes.entrySet()) {
//			ll.log(entry.getKey() + " : " + entry.getValue());
//		}

		String customerPhone = lexReq.getUserId();
		customerPhone = customerPhone.replaceAll("[^\\d]", "");

		String route = inputSlots.get("productRoute").toLowerCase();
		String color = inputSlots.get("colorOption").toLowerCase();
		String amount = inputSlots.get("amountOption").toLowerCase();
		String confirmed = inputSlots.get("productConfirmed").toLowerCase();
		
		ll.log("productRoute:" + route);
		ll.log("productColor:" + color);
		ll.log("productAmount:" + amount);
		ll.log("productConfirmed:" + confirmed);
		
		
		
		DialogActionElicitSlot da = new DialogActionElicitSlot();


		if (route.equals("y")) {
			ll.log("route is y, main offer confirmed");
			// set amount and color and confirmed based on main offer selected
			sessionAttributes.put("s_productColor", "red");
			sessionAttributes.put("s_productAmount", "12");
			sessionAttributes.put("s_productConfirmed", "y");
			

			sessionAttributes.put("s_globalRoseProdId", "c12");
			
			//move on to get note
			da = getNoteDA();

  
		} else {
			// productRoute must be "more"

			if(transformColorInput(color).equals("nomatch")){
				color="x";
			};


			if (color.equals("x")) {
				da = getProductColorDA();
			} else {
				// assign color				
				sessionAttributes.put("s_productColor", transformColorInput(color));
				sessionAttributes.put("s_globalRoseProdId", color);

				
			if(transformAmountInput(amount).equals("nomatch")){
				amount="x";
			}
				
				if (amount.equals("x")) {
					da = getProductAmountDA();
				}else{
					sessionAttributes.put("s_productAmount", transformAmountInput(amount));
					//assign amount and move on to confirmation
					String grpid = sessionAttributes.get("s_globalRoseProdId");
					sessionAttributes.put("s_globalRoseProdId", grpid + transformAmountInput(amount));
					if(confirmed.equals("x")){
						da= getProductConfirmedDA();
						
					}else{
						
						if(confirmed.equals("y")){
							// assign confirmed and move on to note text
							sessionAttributes.put("s_productConfirmed", "y");
							da = getNoteDA();
							
						}else{
						
							// must reset and start over here
							sessionAttributes.put("s_productColor", "x");
							sessionAttributes.put("s_productAmount", "x");
							sessionAttributes.put("s_productConfirmed", "x");
							
							inputSlots.put("productRoute", "more");
							inputSlots.put("colorOption", "x");
							inputSlots.put("amountOption", "x");
							inputSlots.put("productConfirmed", "x");
							
							da = getProductColorDA();
							
						}
							
						
						
						
						
					}
					
					
					
					
				}
				
				
				
			}

		}

		lexRes.setDialogAction(da);
		lexRes.setSessionAttributes(sessionAttributes);
		
//		ll.log("PRINTING ALL SESSION ATTRIBUTES:");
//		for (Map.Entry<String, String> entry : sessionAttributes.entrySet()) {
//			ll.log(entry.getKey() + " : " + entry.getValue());
//		}


		return lexRes;
	}

	private DialogActionElicitSlot getProductColorDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyBotGetProductSelection");
		da.setMessage(new Message("Pick a color by letter:"));
		// setInputSlots(da.getIntentName());

		da.setSlots(inputSlots);
		da.setSlotToElicit("colorOption");

		// add response card with color offers
		// create response card
		ResponseCard rc = new ResponseCard();
		rc.setContentType("application/vnd.amazonaws.card.generic");
		GenericAttachment ga = new GenericAttachment();
		ga.setTitle("dozen color roses offer");
		ga.setSubTitle("dozen color roses offer");
		ga.setImageUrl("https://rosybot.com/images/products/colorOptions.jpg");
		Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
		gas.add(ga);
		rc.setGenericAttachments(gas);

		da.setResponseCard(rc);

		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private DialogActionElicitSlot getProductAmountDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyBotGetProductSelection");
		da.setMessage(new Message(
				"select an amount by option number(1-3):  1-dozen($40), 2-two dozen($60), 3-'THE ROSY BOMB' one hundred roses($120):"));
		// setInputSlots(da.getIntentName());

		da.setSlots(inputSlots);
		da.setSlotToElicit("amountOption");

		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}

	private DialogActionElicitSlot getProductConfirmedDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();

		da.setIntentName("rosyBotGetProductSelection");
		da.setMessage(new Message("Confirm selection: ($" + sessionAttributes.get("s_price") + ") " + sessionAttributes.get("s_productAmount") + " " + sessionAttributes.get("s_productColor") + " roses [y or n]." ));

		da.setSlots(inputSlots);
		da.setSlotToElicit("productConfirmed");
		
		// add response card with main offer
		// create response card
		ResponseCard rc = new ResponseCard();
		rc.setContentType("application/vnd.amazonaws.card.generic");
		GenericAttachment ga = new GenericAttachment();
		ga.setTitle("confirm product selection");
		ga.setSubTitle("confirm");
		ga.setImageUrl("https://www.rosybot.com/images/products/" + sessionAttributes.get("s_globalRoseProdId")  + ".jpg");
		Set<GenericAttachment> gas = new HashSet<GenericAttachment>();
		gas.add(ga);
		rc.setGenericAttachments(gas);


		da.setResponseCard(rc);

		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}
	
	private DialogActionElicitSlot getNoteDA() {
		DialogActionElicitSlot da = new DialogActionElicitSlot();
		
//remove/reset session attribs for this intent
		sessionAttributes.remove("s_noteText");
		sessionAttributes.remove("s_noteConfirmed");
		
		
		da.setIntentName("rosyGetNote");
		da.setMessage(new Message("Enter a message to be printed on a note with the flowers:" ));	
		inputSlots.clear();
		inputSlots.put("noteText", "x");
		inputSlots.put("noteConfirmed", "x");
		
		da.setSlots(inputSlots);
		da.setSlotToElicit("noteText");
		ll.log("INTENT TO BE CALLED:" + da.getIntentName());
		ll.log("INPUT SLOTS TO BE SENT:" + inputSlots.toString());

		return da;
	}
	
	
	
	
	
	private String transformColorInput(String colorInput) {
		switch (colorInput) {
		case "a":
			return "orange";
		case "b":
			return "white";
		case "c":
			return "red";
		case "d":
			return "ivory";
		case "e":
			return "pink";
		case "f":
			return "green";
		case "g":
			return "yellow";
		case "h":
			return "hot pink";
		case "i":
			return "peach";
		default:
			return "nomatch";
		}
	}
	
	private String transformAmountInput(String amountInput) {
		switch (amountInput) {
		case "1":
			sessionAttributes.put("s_price", "40");
			return "12";
		case "2":
			sessionAttributes.put("s_price", "60");
			return "24";
		case "3":
			sessionAttributes.put("s_price", "120");
			return "100";
		default:
			sessionAttributes.put("s_price", "40");
			return "nomatch";
		}
	}
	

	

}
