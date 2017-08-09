package com.rosythebot.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.rosythebot.models.RosyCustomer;;

public class RosyCustomerService {

	private static final String mySqlConnStr = System.getenv("DB_CONN_STR");
	private static final String DB_USER = "mattbob";
	private static final String DB_PSWD = "Bronco20";

	public RosyCustomerService() {

	}

	public RosyCustomer getRosyCustomerByPhone(String phone) {

		String custPhoneTransformed = phone;
		custPhoneTransformed = custPhoneTransformed.replaceAll("[^\\d]", "");

		RosyCustomer foundCust = new RosyCustomer();

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		foundCust = null;
		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
			stmt = conn.createStatement();
			// System.out.println("Select id, phone, email, firstName, lastName
			// from RosyCustomer where phone=\"" + custPhoneTransformed + "\"
			// ");
			rs = stmt.executeQuery("Select id, phone, email, firstName, lastName from RosyCustomer where phone=\""
					+ custPhoneTransformed + "\" ");
			while (rs.next()) {
				foundCust = new RosyCustomer();

				foundCust.setId(rs.getInt(1));
				foundCust.setPhone(rs.getString(2));
				foundCust.setEmail(rs.getString(3));
				foundCust.setFirstName(rs.getString(4));
				foundCust.setLastName(rs.getString(5));

			}
			;

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

		return foundCust;
	}

	public int createCustomer(Map<String, String> customerMap) {

		System.out.println(customerMap.toString());

		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		int createdCustId = 0;

		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);

			String insertTableSQL = "INSERT INTO RosyCustomer (phone, firstName,  lastName, email) VALUES (?,?,?,?)";

			preparedStatement = conn.prepareStatement(insertTableSQL, Statement.RETURN_GENERATED_KEYS);

			preparedStatement.setString(1, customerMap.get("customerPhone"));
			String[] custNameArray = this.separateCustomerFullName(customerMap.get("customerFullName"));
			preparedStatement.setString(2, custNameArray[0]);
			preparedStatement.setString(3, custNameArray[1]);
			preparedStatement.setString(4, customerMap.get("customerEmail"));

			System.out.println(preparedStatement.toString());

			preparedStatement.executeUpdate();
			rs = preparedStatement.getGeneratedKeys();
			if (rs.next()) {
				createdCustId = rs.getInt(1);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return createdCustId;
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

		return createdCustId;
	}

	public int createCustomerByPhone(String phoneNumber) {
		int createdCustId = 0;
		// check if cust exists
		RosyCustomer rc = this.getRosyCustomerByPhone(phoneNumber);
		if (rc == null) {

			Connection conn = null;
			PreparedStatement preparedStatement = null;
			ResultSet rs = null;
		

			try {
				conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);

				String insertTableSQL = "INSERT INTO RosyCustomer (phone) VALUES (?)";

				preparedStatement = conn.prepareStatement(insertTableSQL, Statement.RETURN_GENERATED_KEYS);

				preparedStatement.setString(1, phoneNumber);

				System.out.println(preparedStatement.toString());

				preparedStatement.executeUpdate();
				rs = preparedStatement.getGeneratedKeys();
				if (rs.next()) {
					createdCustId = rs.getInt(1);
				}

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return createdCustId;
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
		}else{
			createdCustId = rc.getId();
		}
		return createdCustId;

	}

	public String[] separateCustomerFullName(String customerFullName) {

		customerFullName = customerFullName.toLowerCase().trim();
		String[] inputNameArray = customerFullName.split(" ");
		String[] outputNameArray = new String[2];
		// first see if there's just 2 names and take the simple route
		if (inputNameArray.length == 1) {
			inputNameArray[0] = inputNameArray[0];
			outputNameArray[1] = inputNameArray[0];
			return outputNameArray;
		}

		if (inputNameArray.length == 2) {
			return inputNameArray;
		}

		if (inputNameArray.length == 3) {

			String customerLastName = customerFullName.substring(customerFullName.lastIndexOf(" "),
					customerFullName.length());
			// remove suffixes
			if (customerLastName.contains("jr") || customerLastName.contains("sr") || customerLastName.contains("II")
					|| customerLastName.contains("m.d") || customerLastName.contains("md")
					|| customerLastName.contains("d.d.s") || customerLastName.contains("dds")
					|| customerLastName.contains("r.n") || customerLastName.contains("rn")) {
				outputNameArray[0] = inputNameArray[0];
				outputNameArray[1] = inputNameArray[1] + " " + inputNameArray[2];
				return outputNameArray;

			} else {

				// must be a middle name or something
				outputNameArray[0] = inputNameArray[0] + " " + inputNameArray[1];
				outputNameArray[1] = inputNameArray[2];
				return outputNameArray;

			}

		}
		return outputNameArray;
	}

}