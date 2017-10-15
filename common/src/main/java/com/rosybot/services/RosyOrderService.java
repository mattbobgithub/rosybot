package com.rosybot.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.DateTime;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosybot.models.RosyOrder;
import com.rosybot.models.Enums.RosyOrderStatus;

public class RosyOrderService {

	private static String mySqlConnStr = System.getenv("DB_CONN_STR");
	private static final String myDevSqlConnStr = System.getenv("DEV_DB_CONN_STR");
	private static final String prodInd = System.getenv("PROD_IND");
	private static final String DB_USER = "mattbob";
	private static final String DB_PSWD = "Bronco20";

	private static final String F1_SHIPPING_FEE = System.getenv("F1_SHIPPING_FEE");
	private static final String SAT_SHIPPING_FEE = System.getenv("SAT_SHIPPING_FEE");

	private static final String SMARTY_AUTH_ID = System.getenv("SMARTY_AUTH_ID");
	private static final String SMARTY_AUTH_TOKEN = System.getenv("SMARTY_AUTH_TOKEN");
	private static final String SMARTY_URL = System.getenv("SMARTY_URL");

	public RosyOrderService() {
		if (prodInd.toLowerCase().equals("false")) {
			mySqlConnStr = myDevSqlConnStr;
		}
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
			if (queryType.equals("OrderId")) {
				rs = stmt.executeQuery("Select * from RosyOrder where id=\"" + requestedId + "\"");
			} else {
				rs = stmt.executeQuery("Select * from RosyOrder where rosyCustomerId=" + requestedId
						+ " order by modifiedDate desc limit 1");
			}

			while (rs.next()) {

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
				boolean validAddress = rs.getBoolean(30);

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
				
				foundRosyOrder.setValidAddress(validAddress);
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

	public String createRosyOrder(Map<String, String> rosyOrderMap) {

		// print all the input parms, neet to match to order fields

		System.out.println(rosyOrderMap.toString());

		// rosyOrderMap.put("s_recipientPhone", "555-555-5555");
		rosyOrderMap.put("s_country", "US");
		rosyOrderMap.put("s_specialInstructions", "");

		String custPhone = rosyOrderMap.get("s_incomingPhoneNumber");
		if (custPhone.length() > 20) {
			custPhone = custPhone.substring(0, 20);
		}
		// first create customer if new customer, then create order
		int createdCustId = 0;

		if (rosyOrderMap.get("existingCustomerFlag").equals("false")) {
			Map<String, String> custMap = new HashMap<String, String>();
			custMap.put("customerId", rosyOrderMap.get("existingCustomerId"));
			custMap.put("customerPhone", custPhone);
			custMap.put("customerFullName", rosyOrderMap.get("s_customerFullName"));
			custMap.put("customerEmail", rosyOrderMap.get("s_customerEmail"));
			RosyCustomerService rcs = new RosyCustomerService();
			// change to only update because create always happens at intro
			createdCustId = rcs.updateCustomer(custMap);
		} else {
			createdCustId = Integer.parseInt(rosyOrderMap.get("existingCustomerId"));
		}

		RosyOrder ro = new RosyOrder();

		UUID newUuid = UUID.randomUUID();

		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);

			String insertTableSQL = "INSERT INTO RosyOrder"
					+ "(id, rosyOrderStatus, customerFullName,  customerPhone,  "
					+ "customerEmail,  rosyProductId,  productCode,  "
					+ "recipientFullName,  recipientStreetAddress1, recipientStreetAddress2, "
					+ "recipientCity,  recipientState,  "
					+ "recipientPostalCode,   recipientCountry,  recipientPhone,  "
					+ "orderTotal, price,  deliveryDate,  cardMessage,  "
					+ "specialInstructions, productImageUrl, productName, rosyCustomerId, occasion, validAddress) VALUES"
					+ "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			preparedStatement = conn.prepareStatement(insertTableSQL);
			// uuid
			preparedStatement.setString(1, newUuid.toString());
			// rosyId is auto set by db on insert (not yet, working on it).
			// rosyOrderStatus,
			preparedStatement.setString(2, "DRAFT");
			// rosyCustomerId gets assigned after order checkout
			// customerFullName
			preparedStatement.setString(3, rosyOrderMap.get("s_customerFullName"));
			// customerPhone,
			preparedStatement.setString(4, custPhone);
			// customerEmail,
			preparedStatement.setString(5, rosyOrderMap.get("s_customerEmail"));
			// rosyProductId,
			preparedStatement.setString(6, rosyOrderMap.get("s_rosyProductId"));
			// productCode,
			preparedStatement.setString(7, rosyOrderMap.get("s_currentOfferProductCode"));
			// orderDateTime - automatically set timestamp by mysql
			// recipientFullName,
			preparedStatement.setString(8, rosyOrderMap.get("s_recipientFullName"));
			// recipientStreetAddress1,
			preparedStatement.setString(9, rosyOrderMap.get("s_streetAddress"));
			// recipientStreetAddress2,
			preparedStatement.setString(10, rosyOrderMap.get("s_aptNo"));
			// recipientCity,
			preparedStatement.setString(11, rosyOrderMap.get("s_city"));
			// recipientState,
			preparedStatement.setString(12, rosyOrderMap.get("s_state"));
			// recipientPostalCode,
			preparedStatement.setString(13, rosyOrderMap.get("s_postalCode"));
			// recipientCountry,
			preparedStatement.setString(14, rosyOrderMap.get("s_country"));
			// recipientPhone,
			preparedStatement.setString(15, rosyOrderMap.get("s_recipientPhone"));
			// orderTotal and price,
			Double price = Double.parseDouble(rosyOrderMap.get("s_price"));
			Double shipFee = Double.parseDouble(F1_SHIPPING_FEE);
			Double orderTot = price + shipFee;
			Double truncatedDouble = BigDecimal.valueOf(orderTot).setScale(3, RoundingMode.HALF_UP).doubleValue();
			preparedStatement.setDouble(16, truncatedDouble);

			preparedStatement.setDouble(17, price);

			// deliveryDate,
			preparedStatement.setString(18, rosyOrderMap.get("s_deliveryDate"));
			// cardMessage,
			preparedStatement.setString(19, rosyOrderMap.get("s_noteText"));
			// specialInstructions
			preparedStatement.setString(20, rosyOrderMap.get("s_specialInstructions"));
			// productImageUrl
			preparedStatement.setString(21, rosyOrderMap.get("s_productImageUrl"));
			// productName
			preparedStatement.setString(22, rosyOrderMap.get("s_productName"));
			// customerId
			preparedStatement.setString(23, StringUtils.fromInteger(createdCustId));

			preparedStatement.setString(24, rosyOrderMap.get("s_occasion"));

			boolean va = validateDeliveryAddress(rosyOrderMap.get("s_streetAddress1"), rosyOrderMap.get("s_city"),
					rosyOrderMap.get("s_state"), rosyOrderMap.get("s_postal"));

			preparedStatement.setBoolean(25, va);

			System.out.println(preparedStatement.toString());
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

		return newUuid.toString();
	}

	public RosyOrder updateRosyOrder(String jsonRosyOrder) {

		System.out.println(jsonRosyOrder);
		RosyOrder updateRosyOrder = new RosyOrder();
		// print all the input parms, neet to match to order fields
		ObjectMapper mapper = new ObjectMapper();
		// JSON from String to Object
		try {
			// System.out.println(jsonRosyOrder);
			updateRosyOrder = mapper.readValue(jsonRosyOrder, RosyOrder.class);
			// System.out.println(mapper.writeValueAsString(updateRosyOrder));
		} catch (IOException e1) {
			System.out.println("ERROR WITH MAPPING JSON INPUT TO ROSYORDER CLASS");
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// System.out.println(updateRosyOrder.toString());

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
			int validAddressIdx = 0;

			int counter = 0;

			String updateTableSQL = "UPDATE RosyOrder set ";
			if (updateRosyOrder.getRosyOrderId() != null) {

				updateTableSQL = updateTableSQL + "rosyOrderId = ?, ";

				counter++;
				rosyOrderIdIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (updateRosyOrder.getRosyOrderStatus() != null) {

				updateTableSQL = updateTableSQL + "rosyOrderStatus = ?, ";

				counter++;
				rosyOrderStatusIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerFullName())) {
				updateTableSQL = updateTableSQL + "customerFullName = ?, ";

				counter++;
				customerFullNameIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerPhone())) {
				updateTableSQL = updateTableSQL + "customerPhone = ?, ";

				counter++;
				customerPhoneIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerEmail())) {
				updateTableSQL = updateTableSQL + "customerEmail = ?, ";

				counter++;
				customerEmailIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}

			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientFullName())) {
				updateTableSQL = updateTableSQL + "recipientFullName = ?, ";

				counter++;
				recipientFullNameIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientStreetAddress1())) {
				updateTableSQL = updateTableSQL + "recipientStreetAddress1 = ?, ";

				counter++;
				recipientStreetAddress1Idx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientStreetAddress2())) {
				updateTableSQL = updateTableSQL + "recipientStreetAddress2 = ?, ";

				counter++;
				recipientStreetAddress2Idx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientCity())) {
				updateTableSQL = updateTableSQL + "recipientCity = ?, ";

				counter++;
				recipientCityIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientState())) {
				updateTableSQL = updateTableSQL + "recipientState = ?, ";

				counter++;
				recipientStateIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientPostalCode())) {
				updateTableSQL = updateTableSQL + "recipientPostalCode = ?, ";

				counter++;
				recipientPostalCodeIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientCountry())) {
				updateTableSQL = updateTableSQL + "recipientCountry = ?, ";

				counter++;
				recipientCountryIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getRecipientPhone())) {
				updateTableSQL = updateTableSQL + "recipientPhone = ?, ";

				counter++;
				recipientPhoneIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}

			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getDeliveryDate())) {
				updateTableSQL = updateTableSQL + "deliveryDate = ?, ";

				counter++;
				deliveryDateIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCardMessage())) {
				updateTableSQL = updateTableSQL + "cardMessage = ?, ";

				counter++;
				cardMessageIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getSpecialInstructions())) {
				updateTableSQL = updateTableSQL + "specialInstructions = ?, ";

				counter++;
				specialInstructionsIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getPaymentId())) {
				updateTableSQL = updateTableSQL + "paymentId = ?, ";

				counter++;
				paymentIdIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getProductImageUrl())) {
				updateTableSQL = updateTableSQL + "productImageUrl = ?, ";

				counter++;
				productImageUrlIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}
			if (!StringUtils.isNullOrEmpty(updateRosyOrder.getProductName())) {
				updateTableSQL = updateTableSQL + "productName = ?, ";

				counter++;
				productNameIdx = counter;
			} else {
				System.out.println(counter + " value is null");
			}

			updateTableSQL = updateTableSQL + "validAddress = ? ";

			counter++;
			validAddressIdx = counter;

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

			boolean va = validateDeliveryAddress(updateRosyOrder.getRecipientStreetAddress1(),
					updateRosyOrder.getRecipientCity(), updateRosyOrder.getRecipientState(),
					updateRosyOrder.getRecipientPostalCode());

			counter++;
			preparedStatement.setBoolean(counter, va);

			preparedStatement.setBoolean(validAddressIdx, va);

			System.out.println(preparedStatement.toString());

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

		// now update rosyCustomer

		Map<String, String> custMap = new HashMap<String, String>();

		if (updateRosyOrder.getRosyCustomerId() > 0) {
			custMap.put("customerId", StringUtils.fromInteger(updateRosyOrder.getRosyCustomerId()));
		}
		if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerPhone())) {
			custMap.put("customerPhone", updateRosyOrder.getCustomerPhone());
		}
		if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerEmail())) {
			custMap.put("customerEmail", updateRosyOrder.getCustomerEmail());
		}
		if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerFullName())) {
			custMap.put("customerFullName", updateRosyOrder.getCustomerFullName());
		}

		if (custMap.size() > 1) {
			System.out.println("updateding email:" + custMap.get("customerEmail"));
			RosyCustomerService rcs = new RosyCustomerService();
			int updCustId = rcs.updateCustomer(custMap);
		}

		return this.getRosyOrder(updateRosyOrder.getId().toString(), "OrderId");
	}

	public RosyOrder updateRosyOrderFromJSON(String jsonRosyOrder) {

		System.out.println(jsonRosyOrder);
		RosyOrder updateRosyOrder = new RosyOrder();
		// print all the input parms, neet to match to order fields
		ObjectMapper mapper = new ObjectMapper();
		// JSON from String to Object
		try {
			// System.out.println(jsonRosyOrder);
			updateRosyOrder = mapper.readValue(jsonRosyOrder, RosyOrder.class);
			// System.out.println(mapper.writeValueAsString(updateRosyOrder));
		} catch (IOException e1) {
			System.out.println("ERROR WITH MAPPING JSON INPUT TO ROSYORDER CLASS");
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerPhone())){
		String custPhone = updateRosyOrder.getCustomerPhone();
		custPhone = custPhone.replaceAll("[^\\d]", "");
		if (custPhone.startsWith("1")) {
			custPhone = custPhone.substring(1);
		}
		if (custPhone.length() > 20) {
			custPhone = custPhone.substring(0, 20);
		}
		updateRosyOrder.setCustomerPhone(custPhone);
		}
		// System.out.println(updateRosyOrder.toString());

		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);

			String updateTableSQL = "UPDATE RosyOrder set ";

			updateTableSQL = updateTableSQL + "customerFullName = ?, ";
			updateTableSQL = updateTableSQL + "customerPhone = ?, ";
			updateTableSQL = updateTableSQL + "customerEmail = ?, ";
			updateTableSQL = updateTableSQL + "recipientFullName = ?, ";
			updateTableSQL = updateTableSQL + "recipientStreetAddress1 = ?, ";
			updateTableSQL = updateTableSQL + "recipientStreetAddress2 = ?, ";
			updateTableSQL = updateTableSQL + "recipientCity = ?, ";
			updateTableSQL = updateTableSQL + "recipientState = ?, ";
			updateTableSQL = updateTableSQL + "recipientPostalCode = ?, ";
			updateTableSQL = updateTableSQL + "recipientCountry = ?, ";
			updateTableSQL = updateTableSQL + "recipientPhone = ?, ";
			updateTableSQL = updateTableSQL + "deliveryDate = ?, ";
			updateTableSQL = updateTableSQL + "cardMessage = ?, ";
			updateTableSQL = updateTableSQL + "specialInstructions = ?, ";
			updateTableSQL = updateTableSQL + "validAddress = ? ";

			String whereClause = "  where id='" + updateRosyOrder.getId().toString() + "'";
			updateTableSQL = updateTableSQL + whereClause;

			preparedStatement = conn.prepareStatement(updateTableSQL);
			int counter = 1;
			preparedStatement.setString(counter, updateRosyOrder.getCustomerFullName());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getCustomerPhone());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getCustomerEmail());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getRecipientFullName());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getRecipientStreetAddress1());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getRecipientStreetAddress2());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getRecipientCity());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getRecipientState());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getRecipientPostalCode());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getRecipientCountry());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getRecipientPhone());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getDeliveryDate());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getCardMessage());
			counter++;
			preparedStatement.setString(counter, updateRosyOrder.getSpecialInstructions());

			boolean va = validateDeliveryAddress(updateRosyOrder.getRecipientStreetAddress1(),
					updateRosyOrder.getRecipientCity(), updateRosyOrder.getRecipientState(),
					updateRosyOrder.getRecipientPostalCode());

			counter++;
			preparedStatement.setBoolean(counter, va);

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

		// now update rosyCustomer

		Map<String, String> custMap = new HashMap<String, String>();

		if (updateRosyOrder.getRosyCustomerId() > 0) {
			custMap.put("customerId", StringUtils.fromInteger(updateRosyOrder.getRosyCustomerId()));
		}
		if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerPhone())) {
			custMap.put("customerPhone", updateRosyOrder.getCustomerPhone());
		}
		if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerEmail())) {
			custMap.put("customerEmail", updateRosyOrder.getCustomerEmail());
		}
		if (!StringUtils.isNullOrEmpty(updateRosyOrder.getCustomerFullName())) {
			custMap.put("customerFullName", updateRosyOrder.getCustomerFullName());
		}

		if (custMap.size() > 1) {
			RosyCustomerService rcs = new RosyCustomerService();
			int updCustId = rcs.updateCustomer(custMap);
		}

		return this.getRosyOrder(updateRosyOrder.getId().toString(), "OrderId");
	}

	public RosyOrder updateRosyOrderToFulfillment(String orderId, String fulfillmentId, String paymentId) {

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
			String updateTableSQL = "UPDATE RosyOrder set rosyOrderStatus ='FULFILLED', rosyOrderId ='" + fulfillmentId
					+ "', paymentId='" + paymentId + "'";

			String whereClause = "  where id='" + orderId + "'";
			updateTableSQL = updateTableSQL + whereClause;

			stmt = conn.createStatement();

			stmt.execute(updateTableSQL);

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

		return this.getRosyOrder(orderId, "OrderId");
	}

	public String fulfillF1Order(RosyOrder ro) {
		String externalOrderId = null;

		// now create Florist One order
		F1Service f1req = new F1Service();
		// base f1 url is:
		// https://www.floristone.com/api/rest/flowershop
		f1req.setUrlExtension("/placeorder");
		f1req.setPayloadFromRosyOrder(ro);
		String response = f1req.post();

		// if error, just create new
		if (response.equals("ERROR")) {
			// this should not happen, but if it does, create order with rosy
			// order instead of f1 order number. this is because they've already
			// paid!!!
			response = "F1-";
			Random generator = new Random();
			int i = generator.nextInt(100000) + 1;
			response = response + StringUtils.fromInteger(i);
		}

		// get orderid from response here

		return response;
	}

	// returns Rosy Order ID
	public String fulfillGlobalRoseOrder(String id) {
		String externalOrderId = null;

		// now create Florist One order
	//	F1Service f1req = new F1Service();
		// base f1 url is:
		// https://www.floristone.com/api/rest/flowershop
	//	f1req.setUrlExtension("/placeorder");
	//	f1req.setPayloadFromRosyOrder(ro);
		// String response = f1req.post();

		String response = "GR-";
		Random generator = new Random();
		int i = generator.nextInt(100000) + 1;
		response = response + StringUtils.fromInteger(i);

		// get orderid from response here

		return response;
	}

	public String createRosyGlobalRoseOrder(Map<String, String> rosyOrderMap) {

		// print all the input parms, neet to match to order fields

		System.out.println(rosyOrderMap.toString());

		// rosyOrderMap.put("s_recipientPhone", "555-555-5555");
		rosyOrderMap.put("s_country", "US");
		rosyOrderMap.put("s_specialInstructions", "");

		String custPhone = rosyOrderMap.get("s_incomingPhoneNumber");
		custPhone = custPhone.replaceAll("[^\\d]", "");
		if (custPhone.startsWith("1")) {
			custPhone = custPhone.substring(1);
		}
		if (custPhone.length() > 20) {
			custPhone = custPhone.substring(0, 20);
		}
		// first create customer if new customer, then create order
		int createdCustId = 0;

		if (rosyOrderMap.get("existingCustomerFlag").equals("false")) {
			Map<String, String> custMap = new HashMap<String, String>();
			custMap.put("customerId", rosyOrderMap.get("existingCustomerId"));
			custMap.put("customerPhone", custPhone);
			custMap.put("customerFullName", rosyOrderMap.get("s_customerFullName"));
			custMap.put("customerEmail", rosyOrderMap.get("s_customerEmail"));
			RosyCustomerService rcs = new RosyCustomerService();
			// change to only update because create always happens at intro
			createdCustId = rcs.updateCustomer(custMap);
		} else {
			createdCustId = Integer.parseInt(rosyOrderMap.get("existingCustomerId"));
		}

		RosyOrder ro = new RosyOrder();

		UUID newUuid = UUID.randomUUID();

		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
String createdKey = "";
		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);

			String insertTableSQL = "INSERT INTO RosyOrder"
					+ "(id, rosyOrderStatus, customerFullName,  customerPhone,  "
					+ "customerEmail,  rosyProductId,  productCode,  "
					+ "recipientFullName,  recipientStreetAddress1, recipientStreetAddress2, "
					+ "recipientCity,  recipientState,  "
					+ "recipientPostalCode,   recipientCountry,  recipientPhone,  "
					+ "orderTotal, price,  deliveryDate,  cardMessage,  "
					+ "specialInstructions, productImageUrl, productName, rosyCustomerId, occasion, validAddress) VALUES"
					+ "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			preparedStatement = conn.prepareStatement(insertTableSQL, PreparedStatement.RETURN_GENERATED_KEYS);
			// uuid
			preparedStatement.setString(1, newUuid.toString());
			// rosyId is auto set by db on insert (not yet, working on it).
			// rosyOrderStatus,
			preparedStatement.setString(2, "DRAFT");
			// rosyCustomerId gets assigned after order checkout
			// customerFullName
			preparedStatement.setString(3, rosyOrderMap.get("s_customerFullName"));
			// customerPhone,
			preparedStatement.setString(4, custPhone);
			// customerEmail,
			preparedStatement.setString(5, rosyOrderMap.get("s_customerEmail"));
			// rosyProductId,
			preparedStatement.setString(6, rosyOrderMap.get("s_rosyProductId"));
			// productCode,
			preparedStatement.setString(7, rosyOrderMap.get("s_globalRoseProdId"));
			// orderDateTime - automatically set timestamp by mysql
			// recipientFullName,
			preparedStatement.setString(8, rosyOrderMap.get("s_recipientFullName"));
			// recipientStreetAddress1,
			preparedStatement.setString(9, rosyOrderMap.get("s_streetAddress1"));
			// recipientStreetAddress2,
			preparedStatement.setString(10, rosyOrderMap.get("s_streetAddress2"));
			// recipientCity,
			preparedStatement.setString(11, rosyOrderMap.get("s_city"));
			// recipientState,
			preparedStatement.setString(12, rosyOrderMap.get("s_state"));
			// recipientPostalCode,
			preparedStatement.setString(13, rosyOrderMap.get("s_postalCode"));
			// recipientCountry,
			if(StringUtils.isNullOrEmpty(rosyOrderMap.get("s_country"))){
				preparedStatement.setString(14, "USA");
				
			}else{
			preparedStatement.setString(14, rosyOrderMap.get("s_country"));
			}
			// recipientPhone,
			preparedStatement.setString(15, rosyOrderMap.get("s_recipientPhone"));
			// orderTotalce,
			Double price = Double.parseDouble(rosyOrderMap.get("s_price"));
			if (rosyOrderMap.get("s_satDeliveryInd").equals("Y")) {
				Double shipFee = Double.parseDouble(SAT_SHIPPING_FEE);
				Double orderTot = price + shipFee;
				Double truncatedDouble = BigDecimal.valueOf(orderTot).setScale(3, RoundingMode.HALF_UP).doubleValue();
				preparedStatement.setDouble(16, truncatedDouble);
			} else {
				preparedStatement.setDouble(16, price);
			}

			// price
			preparedStatement.setDouble(17, price);

			// deliveryDate,
			preparedStatement.setString(18, rosyOrderMap.get("s_deliveryDate"));
			// cardMessage,
			preparedStatement.setString(19, rosyOrderMap.get("s_noteText"));
			// specialInstructions
			preparedStatement.setString(20, rosyOrderMap.get("s_specialInstructions"));
			// productImageUrl
			preparedStatement.setString(21, rosyOrderMap.get("s_productImageUrl"));
			// productName
			preparedStatement.setString(22, rosyOrderMap.get("s_productName"));
			// customerId
			preparedStatement.setString(23, StringUtils.fromInteger(createdCustId));

			preparedStatement.setString(24, rosyOrderMap.get("s_occasion"));
			
			boolean va = validateDeliveryAddress(rosyOrderMap.get("s_streetAddress1"), rosyOrderMap.get("s_city"),
					rosyOrderMap.get("s_state"), rosyOrderMap.get("s_postalCode"));
System.out.println("Valid Address on create:" + va);

			preparedStatement.setBoolean(25, va);

			System.out.println(preparedStatement.toString());
			// execute insert SQL stetement
			preparedStatement.executeUpdate(); 
			
			
				createdKey = newUuid.toString();

			

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("SQLException error");
			createdKey = "ERROR";
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

		return createdKey;
	}

	private boolean validateDeliveryAddress(String str1, String city, String state, String postal) {

		
		
		
		
		InputStream inputStream = null;
		String jsonOutputString = "";
		boolean boolResponse = false;
		
		if(StringUtils.isNullOrEmpty(str1) || StringUtils.isNullOrEmpty(postal)){
			System.out.println("street or postal empty, no call to smarty.");
			return false;
		}else{
			System.out.println("calling smarty.");
			
			
			
		try {
			// create request url
			String smartyRequestUrl = SMARTY_URL + 
					"?auth-id="	+ URLEncoder.encode(SMARTY_AUTH_ID, "UTF-8") + 
					"&auth-token=" + URLEncoder.encode(SMARTY_AUTH_TOKEN, "UTF-8") +
					//"?street=" + URLEncoder.encode(str1, "UTF-8") +
					"&city=" + URLEncoder.encode(city, "UTF-8") +
					"&state=" + state +
					"&zipcode=" + URLEncoder.encode(postal, "UTF-8");
	
			
			

			//System.out.println("Sent url:" + smartyRequestUrl);

			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(smartyRequestUrl);
			// request.addHeader(headerAuth);
			HttpResponse response;
			response = client.execute(request);
			HttpEntity he = response.getEntity();

			inputStream = he.getContent();
			StringWriter writer = new StringWriter();
			String encoding = StandardCharsets.UTF_8.name();
			IOUtils.copy(inputStream, writer, encoding);
			jsonOutputString = writer.toString();
		//	System.out.println(jsonOutputString);
String firstFiddy = jsonOutputString.substring(0,50);


			if(firstFiddy.contains("status")){
				//System.out.println("invalid response status");
				boolResponse = false;
			}else{
				//System.out.println("valid address");
				boolResponse = true;
			}
			
		
	
			// String status = root.get("status").asText();

		} catch (JsonProcessingException e) {
			System.out.println("SMARTY JsonProcessing ERROR!!!!!!!");
			// TODO Auto-generated catch block
			
			e.printStackTrace();
			boolResponse = true;
		} catch (ClientProtocolException e) {
			System.out.println("SMARTY ClientProtocolException ERROR!!!!!!!");
			// TODO Auto-generated catch block
			e.printStackTrace();
			boolResponse = false;
		} catch (IOException e) {
			System.out.println("SMARTY IO ERROR!!!!!!!");
			// TODO Auto-generated catch block
			e.printStackTrace();
			boolResponse = false;

		}

		return boolResponse;
		}

	}

}
