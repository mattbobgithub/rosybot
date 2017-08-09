package com.rosythebot.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.UUID;

import org.joda.time.DateTime;

import com.amazonaws.util.StringUtils;
import com.rosythebot.models.RosyOrder;
import com.rosythebot.models.Enums.RosyOrderStatus;

public class RosyOrderService {

	private static final String mySqlConnStr = System.getenv("DB_CONN_STR");
	private static final String DB_USER = "mattbob";
	private static final String DB_PSWD = "Bronco20";

	private static final String F1_SHIPPING_FEE = System.getenv("F1_SHIPPING_FEE");

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
			if (queryType.equals("OrderId")) {
				rs = stmt.executeQuery("Select * from RosyOrder where id=\"" + requestedId + "\"");
			} else {
				rs = stmt.executeQuery("Select * from RosyOrder where rosyCustomerId=" + requestedId
						+ " order by modifiedDate desc limit 1");
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

	public String createRosyOrder(Map<String, String> rosyOrderMap) {

		// print all the input parms, neet to match to order fields

		System.out.println(rosyOrderMap.toString());

		// hardcode but need to get from twilio

		rosyOrderMap.put("s_customerEmail", null);
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
			//change to only update because create always happens at intro
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
					+ "specialInstructions, productImageUrl, productName, rosyCustomerId) VALUES"
					+ "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

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
			System.out.println("setting price to:" + rosyOrderMap.get("s_price"));
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
}