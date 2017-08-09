package com.rosythebot.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.util.StringUtils;
import com.rosythebot.models.RosyProduct;
import com.rosythebot.models.Enums.RosyCategory;

public class RosyProductService {

	private static final Regions AWS_REGION = Regions.US_EAST_1;
	private static final String mySqlConnStr = System.getenv("DB_CONN_STR");
	private static final int OFFER_COUNT_LIMIT = Integer.parseInt(System.getenv("OFFER_COUNT_LIMIT"));
	private static final String DB_USER = "mattbob";
	private static final String DB_PSWD = "Bronco20";

	public RosyProductService() {

	}

	// get product offering from occasion
	public RosyProduct fromOccasion(String occasion, int offerCountInt) {

		return determineOffer(getRosyCategoryFromOccasion(occasion), offerCountInt);
	}

	public RosyProduct fromProductCode(String productCode) {
			
		
		RosyProduct rp = new RosyProduct();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
	
		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("Select * from RosyProduct where code=\"" + productCode + "\"");
			// get the first and only row
			rs.next();

			rp.setId(rs.getInt("id"));
			rp.setName(rs.getString("name"));
			rp.setCode(rs.getString("code"));
			rp.setPrice(rs.getString("price"));
			rp.setImageURL(rs.getString("imageUrl"));
			rp.setCategory(rs.getString("category"));
			rp.setRosyCategory(RosyCategory.valueOf(rs.getString("rosyCategory")));
			rp.setRosyOfferOrder(rs.getInt("rosyOfferOrder"));

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {

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

		return rp;
	}

	private RosyProduct determineOffer(RosyCategory rc, int offerCountInt) {

		RosyProduct currentOffer = new RosyProduct();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {

			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("Select * from RosyProduct where rosyCategory=\"" + rc + "\" and rosyOfferOrder <="
					+ StringUtils.fromInteger(OFFER_COUNT_LIMIT));
			if (!rs.isBeforeFirst()) {
				// if no rows returned, get offers for EVERYDAT rosyCategory
				System.out.println("No data");
				currentOffer = this.getBackupOffer("EVERYDAY", offerCountInt);
			} else {
				// get the first and only row
				rs.absolute(offerCountInt);

				currentOffer.setId(rs.getInt("id"));
				currentOffer.setName(rs.getString("name"));
				currentOffer.setCode(rs.getString("code"));
				currentOffer.setPrice(rs.getString("price"));
				currentOffer.setImageURL(rs.getString("imageUrl"));
				currentOffer.setCategory(rs.getString("category"));
				currentOffer.setRosyCategory(RosyCategory.valueOf(rs.getString("rosyCategory")));
				currentOffer.setRosyOfferOrder(rs.getInt("rosyOfferOrder"));
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
return currentOffer;
	}

	
	
public RosyProduct getBackupOffer(String rosyCat, int offerCountInt) {
			
		
		RosyProduct backupOfferProduct = new RosyProduct();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
	
		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("Select * from RosyProduct where rosyCategory=\"" + rosyCat + "\" and rosyOfferOrder<=" + StringUtils.fromInteger(OFFER_COUNT_LIMIT));
			// get the first and only row
			rs.absolute(offerCountInt);

			backupOfferProduct.setId(rs.getInt("id"));
			backupOfferProduct.setName(rs.getString("name"));
			backupOfferProduct.setCode(rs.getString("code"));
			backupOfferProduct.setPrice(rs.getString("price"));
			backupOfferProduct.setImageURL(rs.getString("imageUrl"));
			backupOfferProduct.setCategory(rs.getString("category"));
			backupOfferProduct.setRosyCategory(RosyCategory.valueOf(rs.getString("rosyCategory")));
			backupOfferProduct.setRosyOfferOrder(rs.getInt("rosyOfferOrder"));

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {

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

		return backupOfferProduct;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	private RosyCategory getRosyCategoryFromOccasion(String occasion) {

		occasion = occasion.toLowerCase();

		// returns exact match category or default determination of Everyday
		RosyCategory rc = checkForExactMatch(occasion);

		// check for obvious incomplete strings
		if (occasion.contains("bir")) {
			rc = RosyCategory.BIRTHDAY;

		}
		if (occasion.contains("anni")) {
			rc = RosyCategory.ROMANCE;

		}
		if (occasion.contains("fune")) {
			rc = RosyCategory.FUNERAL;

		}
		if (occasion.contains("sick")) {
			rc = RosyCategory.GETWELL;

		}
		if (occasion.contains("thank")) {
			rc = RosyCategory.THANKYOU;
		}
		if (occasion.contains("becau")) {
			rc = RosyCategory.EVERYDAY;

		}
		if (occasion.contains("baby")) {
			rc = RosyCategory.NEWBABY;

		}
		if (occasion.contains("lov")) {
			rc = RosyCategory.ROMANCE;

		}
		if (occasion.contains("luv")) {
			rc = RosyCategory.ROMANCE;
		}
		if (occasion.contains("valen")) {
			rc = RosyCategory.ROMANCE;
		}
		if (occasion.contains("sorr")) {
			rc = RosyCategory.NOTFUNERALSYMPATHY;
		}

		return rc;

	}

	private RosyCategory checkForExactMatch(String occasion) {
		// set default to Everyday
		RosyCategory rc = RosyCategory.EVERYDAY;

		occasion = occasion.toUpperCase();
		RosyCategory[] allRosyCategories = RosyCategory.values();

		for (int i = 0; i < allRosyCategories.length - 1; i++) {
			String thisCategory = allRosyCategories[i].toString();
			if (thisCategory.contains(occasion)) {
				rc = RosyCategory.valueOf(thisCategory);
			}

		}

		return rc;
	}

}
