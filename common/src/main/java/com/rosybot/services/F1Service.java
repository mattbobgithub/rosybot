package com.rosybot.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosybot.models.RosyOrder;

public class F1Service {

	private static final String APACHE_SIMPLE_LOGS = System.getenv("APACHE_LOGS");
	private static final String F1_AUTH_ENCODED = System.getenv("F1_AUTH");
	private static final String F1_URL = System.getenv("F1_URL");
	private static final String F1_CC_NUM = System.getenv("F1_CC_NUM");
	private static final String F1_CC_EXP = System.getenv("F1_CC_EXP");
	private static final String F1_CC_CVV2 = System.getenv("F1_CC_CVV2");
	private static final String F1_CC_TYPE = System.getenv("F1_CC_TYPE");
	private static final String F1_SHIPPING_FEE = System.getenv("F1_SHIPPING_FEE");

	final JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

	private String payload;
	private String urlExtension;

	public F1Service() {

	}

	public F1Service(String ue, String pl) {
		this.setUrlExtension(ue);
		this.setPayload(pl);

	}

	public String post() {

		// try to call florist one api here
		// set apache logger
		if (APACHE_SIMPLE_LOGS == "true") {
			java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);

			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
			System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
		}

		String f1_jsonOutput = "";
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost request = new HttpPost(F1_URL + urlExtension);
		Header headerAuth = new BasicHeader("Authorization", "Basic  " + F1_AUTH_ENCODED);
		request.addHeader(headerAuth);
		request.addHeader("Content-type", "application/json");

		HttpEntity reqHe = null;
		try {

			reqHe = new StringEntity(this.getPayload());
			request.setEntity(reqHe);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		HttpResponse response = null;
		try {
			response = client.execute(request);

			HttpEntity he = response.getEntity();

			// convert HttpEntity to Json

			if (he != null) {

				InputStream outputStream = he.getContent();
				ObjectMapper outputMapper = new ObjectMapper();
				TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
				};

				HashMap<String, Object> outputMap = outputMapper.readValue(outputStream, typeRef);

				if (outputMap.containsKey("errors")) {
					System.out.println(
							"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX =========== ERROR FOUND FROM F1");
					String outputErrorMsg = outputMap.get("errors").toString();
					System.out.println(outputErrorMsg);
					f1_jsonOutput = "ERROR";
				} else {
				//	System.out.println("successful call to F1");
				//	System.out.println(outputMap.toString());
					f1_jsonOutput = outputMap.get("ORDERNO").toString();
				}

			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			f1_jsonOutput = "ERROR";
			
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			f1_jsonOutput = "ERROR";
			e.printStackTrace();
		}

		return f1_jsonOutput;
	}

	public String get() {

		// try to call florist one api here
		// set apache logger
		if (APACHE_SIMPLE_LOGS == "true") {
			java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);

			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
			System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
		}

		String f1_jsonOutput = "";
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(F1_URL + urlExtension);
		Header headerAuth = new BasicHeader("Authorization", "Basic  " + F1_AUTH_ENCODED);
		request.addHeader(headerAuth);
		HttpResponse response = null;
		try {
			response = client.execute(request);
			HttpEntity he = response.getEntity();

			f1_jsonOutput = EntityUtils.toString(he);


		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return f1_jsonOutput;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getUrlExtension() {
		return urlExtension;
	}

	public void setUrlExtension(String urlExtension) {
		this.urlExtension = urlExtension;
	}

	public void setPayloadFromRosyOrder(RosyOrder ro) {
		ObjectNode requestObject = jsonFactory.objectNode();
		requestObject.put("customer", this.createCustomerObjectString(ro));
		requestObject.put("products", this.createProductObjectString(ro));
		requestObject.put("ccinfo", this.createCCInfoObjectString());

		Double orderTotal = ro.getOrderTotal();
		Double truncatedDouble = BigDecimal.valueOf(orderTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();

		requestObject.put("ordertotal", StringUtils.fromDouble(truncatedDouble));

		this.setPayload(requestObject.toString());


	}

	private String createCustomerObjectString(RosyOrder ro) {
		ObjectNode customerObject = jsonFactory.objectNode();
		
		customerObject.put("ZIPCODE", ro.getRecipientPostalCode());
		customerObject.put("PHONE", ro.getCustomerPhone());
		customerObject.put("ADDRESS2", ro.getRecipientStreetAddress2());
		customerObject.put("STATE", ro.getRecipientState());
		customerObject.put("ADDRESS1", ro.getRecipientStreetAddress1());
		customerObject.put("NAME", ro.getCustomerFullName());
		customerObject.put("COUNTRY", "US");
		customerObject.put("IP", "");
		if(!StringUtils.isNullOrEmpty(ro.getCustomerEmail())){
			customerObject.put("EMAIL", ro.getCustomerEmail());
		}else{
			customerObject.put("EMAIL", "orders@rosythebot.com");
			
			//customerObject.put("EMAIL", "");
		}
	
			customerObject.put("CITY", ro.getRecipientCity());

		ObjectMapper custMap = new ObjectMapper();
		String output = null;
		try {
			output = custMap.writeValueAsString(customerObject);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}

	private String createProductObjectString(RosyOrder ro) {
		ObjectNode productNode = jsonFactory.objectNode();
		productNode.put("PRICE", ro.getPrice());
		productNode.put("CARDMESSAGE", ro.getCardMessage());
		productNode.put("CODE", ro.getProductCode());
		// add delivery date in proper format
		String delDateStr = ro.getDeliveryDate();
		// convert to YYYY-MM-DD format
		SimpleDateFormat dt = new SimpleDateFormat("mm-dd-yyyy");
		Date delDateDate = null;
		try {
			delDateDate = dt.parse(delDateStr);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-mm-dd");

		String delDateStrYYYYMMDD = dt1.format(delDateDate);
		productNode.put("DELIVERYDATE", delDateStrYYYYMMDD);

		ObjectNode recipientNode = jsonFactory.objectNode();
		recipientNode.put("ZIPCODE", ro.getRecipientPostalCode());
		recipientNode.put("PHONE", ro.getRecipientPhone());
		recipientNode.put("ADDRESS1", ro.getRecipientStreetAddress1());
		recipientNode.put("ADDRESS2", "");
		recipientNode.put("NAME", ro.getRecipientFullName());
		recipientNode.put("COUNTRY", ro.getRecipientCountry());
		recipientNode.put("INSTITUTION", "");
		recipientNode.put("CITY", ro.getRecipientCity());
		recipientNode.put("STATE", ro.getRecipientState());		
		productNode.set("RECIPIENT", recipientNode);

		String[] prodArrayString = new String[] {};
		ArrayNode an = jsonFactory.arrayNode();
		an.add(productNode);

		ObjectMapper prodMap = new ObjectMapper();
		String output = null;
		try {
			output = prodMap.writeValueAsString(an);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}

	private String createCCInfoObjectString() {
		ObjectNode ccnumNode = jsonFactory.objectNode();

		String[] expDate = F1_CC_EXP.split("/");
		ccnumNode.put("EXPMONTH", expDate[0]);
		ccnumNode.put("EXPYEAR", expDate[1]);
		ccnumNode.put("CCNUM", F1_CC_NUM);
		ccnumNode.put("CVV2", F1_CC_CVV2);
		ccnumNode.put("TYPE", F1_CC_TYPE);

		ObjectMapper ccMap = new ObjectMapper();
		String output = null;
		try {
			output = ccMap.writeValueAsString(ccnumNode);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;

	}
	
	
	
	
	
	
	
	
}
