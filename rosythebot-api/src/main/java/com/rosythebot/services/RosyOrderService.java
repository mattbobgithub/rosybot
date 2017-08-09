package com.rosythebot.services;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.joda.time.DateTime;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosythebot.models.Enums.RosyOrderStatus;
import com.rosythebot.models.RosyOrder;

public class RosyOrderService {

	private static final String mySqlConnStr = System.getenv("DB_CONN_STR");
	private static final String DB_USER = "mattbob";
	private static final String DB_PSWD = "Bronco20";

	public RosyOrderService() {

	}

	public RosyOrder getRosyOrder(String requestedId, String queryType) {

		RosyOrder foundRosyOrder = null;
		// print all the input parms, neet to match to order fields

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
			stmt = conn.createStatement();
			if(queryType.equals("OrderId")){
				rs = stmt.executeQuery("Select * from RosyOrder where id=\"" + requestedId + "\"");
			}else{
				rs = stmt.executeQuery("Select * from RosyOrder where rosyCustomerId=" + requestedId);
			}
		
			while(rs.next()){
				foundRosyOrder = new RosyOrder();
		

			String id = rs.getString(1);
			String rosyOrderId = rs.getString(2);
			String rosyOrderStatus = rs.getString(3);
			int rosyCustomerId = rs.getInt(4);
			String customerFullName = rs.getString(5);
			String customerPhone = rs.getString(6);
			String customerEmail = rs.getString(7);
			int rosyProductId = rs.getInt(8);
			String productCode = rs.getString(9);
			String recipientFullName = rs.getString(10);
			String recipientInstitution = rs.getString(11);
			String recipientStreetAddress1 = rs.getString(12);
			String recipientStreetAddress2 = rs.getString(13);
			String recipientCity = rs.getString(14);
			String recipientState = rs.getString(15);
			String recipientPostalCode = rs.getString(16);
			String recipientCountry = rs.getString(17);
			String recipientPhone = rs.getString(18);
			Double orderTotal = rs.getDouble(19);
			String deliveryDate = rs.getString(20);
			String cardMessage = rs.getString(21);
			String specialInstructions = rs.getString(22);
		
			Date createdDate = rs.getTimestamp(23);
			Date modifiedDate = rs.getTimestamp(24);
			String paymentId = rs.getString(25);
			Double price = rs.getDouble(26);
			String productImageUrl = rs.getString(27);
			String productName = rs.getString(28);
			// don't get timestamps

			foundRosyOrder.setId(UUID.fromString(id));
			foundRosyOrder.setRosyOrderId(rosyOrderId);
			foundRosyOrder.setRosyOrderStatus(RosyOrderStatus.valueOf(rosyOrderStatus));
			foundRosyOrder.setRosyCustomerId(rosyCustomerId);
			foundRosyOrder.setCustomerFullName(customerFullName);
			foundRosyOrder.setCustomerPhone(customerPhone);
			foundRosyOrder.setCustomerEmail(customerEmail);
			foundRosyOrder.setRosyProductId(rosyProductId);
			foundRosyOrder.setProductCode(productCode);
			foundRosyOrder.setRecipientFullName(recipientFullName);
			foundRosyOrder.setRecipientInstitution(recipientInstitution);
			foundRosyOrder.setRecipientStreetAddress1(recipientStreetAddress1);
			foundRosyOrder.setRecipientStreetAddress2(recipientStreetAddress2);
			foundRosyOrder.setRecipientCity(recipientCity);
			foundRosyOrder.setRecipientState(recipientState);
			foundRosyOrder.setRecipientPostalCode(recipientPostalCode);
			foundRosyOrder.setRecipientCountry(recipientCountry);
			foundRosyOrder.setRecipientPhone(recipientPhone);
			foundRosyOrder.setOrderTotal(orderTotal);
			foundRosyOrder.setDeliveryDate(deliveryDate);
			foundRosyOrder.setCardMessage(cardMessage);
			foundRosyOrder.setSpecialInstructions(specialInstructions);
			foundRosyOrder.setCreateDate(new DateTime(createdDate));
			foundRosyOrder.setModifiedDate(new DateTime(modifiedDate));
			foundRosyOrder.setPaymentId(paymentId);
			foundRosyOrder.setPrice(price);
			foundRosyOrder.setProductImageUrl(productImageUrl);
			foundRosyOrder.setProductName(productName);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {

			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					/* ignored */ }
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					/* ignored */ }
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					/* ignored */ }
			}
		}

		return foundRosyOrder;
	}

	public RosyOrder updateRosyOrder(String jsonRosyOrder) {
		

		RosyOrder updateRosyOrder = new RosyOrder();
		// print all the input parms, neet to match to order fields
		ObjectMapper mapper = new ObjectMapper();
		// JSON from String to Object
		try {
			System.out.println(jsonRosyOrder);
			updateRosyOrder = mapper.readValue(jsonRosyOrder, RosyOrder.class);
			//System.out.println(mapper.writeValueAsString(updateRosyOrder));
		} catch (IOException e1) {
			System.out.println("ERROR WITH MAPPING JSON INPUT TO ROSYORDER CLASS");
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		System.out.println(updateRosyOrder.toString());


		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
			int rosyOrderIdIdx = 0;
			int rosyOrderStatusIdx = 0;
			int customerFullNameIdx = 0;
			int customerPhoneIdx = 0;
			int customerEmailIdx = 0;
			int rosyProductIdIdx = 0;
			int productCodeIdx = 0;
			int recipientFullNameIdx = 0;
			int recipientStreetAddress1Idx = 0;
			int recipientStreetAddress2Idx = 0;
			int recipientCityIdx = 0;
			int recipientStateIdx = 0;
			int recipientPostalCodeIdx = 0;
			int recipientCountryIdx = 0;
			int recipientPhoneIdx = 0;
			int orderTotalIdx = 0;
			int deliveryDateIdx = 0;
			int cardMessageIdx = 0;
			int specialInstructionsIdx = 0;
			int paymentIdIdx = 0;
			int productImageUrlIdx = 0;
		 int productNameIdx = 0;
		 

			int counter = 0;

			String updateTableSQL = "UPDATE RosyOrder set ";
		if (updateRosyOrder.getRosyOrderId() != null) {		
				
				updateTableSQL = updateTableSQL + "rosyOrderId = ?, ";

				counter++;
				rosyOrderIdIdx = counter;
			}
			if (updateRosyOrder.getRosyOrderStatus() != null) {		
				
				updateTableSQL = updateTableSQL + "rosyOrderStatus = ?, ";

				counter++;
				rosyOrderStatusIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerFullName())) {
				updateTableSQL = updateTableSQL + "customerFullName = ?, ";

				counter++;
				customerFullNameIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerPhone())) {
				updateTableSQL = updateTableSQL + "customerPhone = ?, ";

				counter++;
				customerPhoneIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerEmail())) {
				updateTableSQL = updateTableSQL + "customerEmail = ?, ";

				counter++;
				customerEmailIdx = counter;
			}

			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientFullName())) {
				updateTableSQL = updateTableSQL + "recipientFullName = ?, ";

				counter++;
				recipientFullNameIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientStreetAddress1())) {
				updateTableSQL = updateTableSQL + "recipientStreetAddress1 = ?, ";

				counter++;
				recipientStreetAddress1Idx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientStreetAddress2())) {
				updateTableSQL = updateTableSQL + "recipientStreetAddress2 = ?, ";

				counter++;
				recipientStreetAddress2Idx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientCity())) {
				updateTableSQL = updateTableSQL + "recipientCity = ?, ";

				counter++;
				recipientCityIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientState())) {
				updateTableSQL = updateTableSQL + "recipientState = ?, ";

				counter++;
				recipientStateIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientPostalCode())) {
				updateTableSQL = updateTableSQL + "recipientPostalCode = ?, ";

				counter++;
				recipientPostalCodeIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientCountry())) {
				updateTableSQL = updateTableSQL + "recipientCountry = ?, ";

				counter++;
				recipientCountryIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientPhone())) {
				updateTableSQL = updateTableSQL + "recipientPhone = ?, ";

				counter++;
				recipientPhoneIdx = counter;
			}

			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getDeliveryDate())) {
				updateTableSQL = updateTableSQL + "deliveryDate = ?, ";

				counter++;
				deliveryDateIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCardMessage())) {
				updateTableSQL = updateTableSQL + "cardMessage = ?, ";

				counter++;
				cardMessageIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getSpecialInstructions())) {
				updateTableSQL = updateTableSQL + "specialInstructions = ?, ";

				counter++;
				specialInstructionsIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getPaymentId())) {
				updateTableSQL = updateTableSQL + "paymentId = ?, ";

				counter++;
				paymentIdIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getProductImageUrl())) {
				updateTableSQL = updateTableSQL + "productImageUrl = ?, ";

				counter++;
				productImageUrlIdx = counter;
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getProductName())) {
				updateTableSQL = updateTableSQL + "productName = ?, ";

				counter++;
				productNameIdx = counter;
			}
			
			
			
			
			// replace last comma with close paren
			updateTableSQL = updateTableSQL.substring(0, updateTableSQL.length() - 2);

			String whereClause = "  where id='" + updateRosyOrder.getId().toString() + "'";
			updateTableSQL = updateTableSQL + whereClause;

			preparedStatement = conn.prepareStatement(updateTableSQL);
			
			if (updateRosyOrder.getRosyOrderId() != null) {
				// rosyOrderStatus,
				preparedStatement.setString(rosyOrderIdIdx, updateRosyOrder.getRosyOrderId());
			}

			if (updateRosyOrder.getRosyOrderStatus() != null) {
				// rosyOrderStatus,
				preparedStatement.setString(rosyOrderStatusIdx, updateRosyOrder.getRosyOrderStatus().toString());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerFullName())) {
				// customerFullName
				preparedStatement.setString(customerFullNameIdx, updateRosyOrder.getCustomerFullName());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerPhone())) {
				// customerPhone,
				preparedStatement.setString(customerPhoneIdx, updateRosyOrder.getCustomerPhone());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerEmail())) {
				preparedStatement.setString(customerEmailIdx, updateRosyOrder.getCustomerEmail());
			}

			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientFullName())) {
				// recipientFullName,
				preparedStatement.setString(recipientFullNameIdx, updateRosyOrder.getRecipientFullName());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientStreetAddress1())) {
				// recipientStreetAddress1,
				preparedStatement.setString(recipientStreetAddress1Idx, updateRosyOrder.getRecipientStreetAddress1());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientStreetAddress2())) {
				// recipientStreetAddress1,
				preparedStatement.setString(recipientStreetAddress2Idx, updateRosyOrder.getRecipientStreetAddress2());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientCity())) {
				// recipientCity,
				preparedStatement.setString(recipientCityIdx, updateRosyOrder.getRecipientCity());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientState())) {
				// recipientState,
				preparedStatement.setString(recipientStateIdx, updateRosyOrder.getRecipientState());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientPostalCode())) {
				// recipientPostalCode,
				preparedStatement.setString(recipientPostalCodeIdx, updateRosyOrder.getRecipientPostalCode());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientCountry())) {
				// recipientCountry,
				preparedStatement.setString(recipientCountryIdx, updateRosyOrder.getRecipientCountry());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientPhone())) {
				// recipientPhone,
				preparedStatement.setString(recipientPhoneIdx, updateRosyOrder.getRecipientPhone());
			}

			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getDeliveryDate())) {
				// deliveryDate,
				preparedStatement.setString(deliveryDateIdx, updateRosyOrder.getDeliveryDate());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCardMessage())) {
				// cardMessage,
				preparedStatement.setString(cardMessageIdx, updateRosyOrder.getCardMessage());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getSpecialInstructions())) {
				// specialInstructions
				preparedStatement.setString(specialInstructionsIdx, updateRosyOrder.getSpecialInstructions());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getPaymentId())) {
				// paymentId
				preparedStatement.setString(paymentIdIdx, updateRosyOrder.getPaymentId());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getProductImageUrl())) {
				// paymentId
				preparedStatement.setString(productImageUrlIdx, updateRosyOrder.getProductImageUrl());
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getProductName())) {
				// paymentId
				preparedStatement.setString(productNameIdx, updateRosyOrder.getProductName());
			}
			
		//	System.out.println(preparedStatement.toString());

			// execute insert SQL stetement
			preparedStatement.executeUpdate();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {

			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					/* ignored */ }
			}
			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					/* ignored */ }
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					/* ignored */ }
			}
		}

		//now update rosyCustomer
		
		Map<String, String> custMap = new HashMap<String, String>();
		System.out.println("custId:" + updateRosyOrder.getRosyCustomerId());
		if(updateRosyOrder.getRosyCustomerId() > 0 ){
			custMap.put("customerId",StringUtils.fromInteger(updateRosyOrder.getRosyCustomerId()));
		}
		if(!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerPhone())){
			custMap.put("customerPhone", updateRosyOrder.getCustomerPhone());
		}
		if(!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerEmail())){
			custMap.put("customerEmail", updateRosyOrder.getCustomerEmail());
		}
		if(!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerFullName())){
			custMap.put("customerFullName", updateRosyOrder.getCustomerFullName());
		}

		RosyCustomerService rcs = new RosyCustomerService();
		int updCustId = rcs.updateCustomer(custMap);
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		return this.getRosyOrder(updateRosyOrder.getId().toString(), "OrderId");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public String fulfillOrder(RosyOrder ro){
		String externalOrderId = null;
		
		//now create Florist One order
		F1Request f1req = new F1Request();
		//base f1 url is:
		//https://www.floristone.com/api/rest/flowershop
		f1req.setUrlExtension("/placeorder");
		f1req.setPayloadFromRosyOrder(ro);
		String response = f1req.post();
		
		//if error, just create new 
		if(response.equals("ERROR")){
			//this should not happen,  but if it does, create order with rosy order instead of f1 order number.  this is because they've already paid!!!
			response = "ROSY-";
		    Random generator = new Random(); 
			int i = generator.nextInt(100000) + 1;
			response = response + StringUtils.fromInteger(i);
		}
		
		//get orderid from response here		
		System.out.println(response);
		
		
	
		
		
		return response;
	}
	
	
}