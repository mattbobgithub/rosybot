package com.rosybot.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.regions.Regions;
import com.amazonaws.util.StringUtils;
import com.rosybot.models.RosyProduct;
import com.rosybot.models.Enums.RosyCategory;

public class RosyProductService {

	private static final Regions AWS_REGION = Regions.US_EAST_1;
	private static  String mySqlConnStr = System.getenv("DB_CONN_STR");
	private static final String myDevSqlConnStr = System.getenv("DEV_DB_CONN_STR");
	private static final String prodInd = System.getenv("PROD_IND");
	private static final int OFFER_COUNT_LIMIT = Integer.parseInt(System.getenv("OFFER_COUNT_LIMIT"));
	private static final String DB_USER = "mattbob";
	private static final String DB_PSWD = "Bronco20";
	
	Map<String,String >allKeywords;


	public RosyProductService() {
		if(prodInd.toLowerCase().equals("false")){
			mySqlConnStr = myDevSqlConnStr;
		}
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
			System.out.println("Select * from RosyProduct where code=\"" + productCode + "\"");
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
			rs = stmt.executeQuery("Select * from RosyProduct where rosyCategory=\"" + rc + "\" and rosyOfferOrder="+ StringUtils.fromInteger(offerCountInt));
		//	System.out.println("Select * from RosyProduct where rosyCategory=\"" + rc + "\" and rosyOfferOrder="+ StringUtils.fromInteger(offerCountInt));
			
			if (!rs.isBeforeFirst()) {
				// if no rows returned, get offers for EVERYDAY rosyCategory
			//	System.out.println("No offers found for category, offercount of " + StringUtils.fromInteger(offerCountInt) + ", get backup offer");
				currentOffer = this.getBackupOffer("EVERYDAY", offerCountInt);
			} else {
				// get the first and only row
				rs.absolute(1);

				currentOffer.setId(rs.getInt("id"));
				currentOffer.setName(rs.getString("name"));
				currentOffer.setCode(rs.getString("code"));
				currentOffer.setPrice(rs.getString("price"));
				currentOffer.setImageURL(rs.getString("imageUrl"));
				currentOffer.setCategory(rs.getString("category"));
				currentOffer.setRosyCategory(RosyCategory.valueOf(rs.getString("rosyCategory")));
				currentOffer.setRosyOfferOrder(rs.getInt("rosyOfferOrder"));
				
			//	System.out.println("OFFER RETURNED- category: " + currentOffer.getRosyCategory() + " offercount:" + offerCountInt + "  name:" + currentOffer.getName());
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
			rs = stmt.executeQuery("Select * from RosyProduct where rosyCategory=\"" + rosyCat
					+ "\" and rosyOfferOrder<=" + StringUtils.fromInteger(OFFER_COUNT_LIMIT));
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

		return backupOfferProduct;
	}

	private RosyCategory getRosyCategoryFromOccasion(String occasion) {

		occasion = occasion.toLowerCase();

		// returns exact match category or default determination of Everyday
		RosyCategory rc = checkForExactMatch(occasion);

		if (allKeywords==null) {
			allKeywords = new HashMap<String, String>();
			this.setKeywords();
		}

		for (Map.Entry<String, String> entry : allKeywords.entrySet()) {
		//	System.out.println("category:" + entry.getKey() + "  keyword:" + entry.getValue());
			if (occasion.contains(entry.getKey())) {
				rc = RosyCategory.valueOf(entry.getValue());
			//	System.out.println("MATCH FOUND - category:" + entry.getKey() + "  keyword:" + entry.getValue());
				
				break;
			}
		}

		if (rc == null) {	
			//System.out.println("NO MATCH FOUND for input occasion:" + occasion);
			rc = RosyCategory.EVERYDAY;
		}

		// check for obvious incomplete strings
		/*
		 * if (occasion.contains("bir")) { rc = RosyCategory.BIRTHDAY;
		 * 
		 * } if (occasion.contains("anni")) { rc = RosyCategory.ANNIVERSARY;
		 * 
		 * } if (occasion.contains("fune")) { rc = RosyCategory.FUNERAL;
		 * 
		 * } if (occasion.contains("sick")) { rc = RosyCategory.GETWELL;
		 * 
		 * } if (occasion.contains("health")) { rc = RosyCategory.GETWELL;
		 * 
		 * } if (occasion.contains("get well")) { rc = RosyCategory.GETWELL;
		 * 
		 * } if (occasion.contains("thank")) { rc = RosyCategory.THANKYOU; } if
		 * (occasion.contains("becau")) { rc = RosyCategory.EVERYDAY;
		 * 
		 * } if (occasion.contains("baby")) { rc = RosyCategory.NEWBABY;
		 * 
		 * } if (occasion.contains("lov")) { rc = RosyCategory.ROMANCE;
		 * 
		 * } if (occasion.contains("luv")) { rc = RosyCategory.ROMANCE; } if
		 * (occasion.contains("valen")) { rc = RosyCategory.ROMANCE; } if
		 * (occasion.contains("sorr")) { rc = RosyCategory.NOTFUNERALSYMPATHY; }
		 */

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
			//	System.out.println("EXACT MATCH WITH" + rc.name());
			}

		}

		return rc;
	}

	private void setKeywords() {

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("Select * from RosyCategoryKeywords");

			while (rs.next()) {
				
				allKeywords.put(rs.getString("keyword").toLowerCase(),rs.getString("category"));
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

	}

}
