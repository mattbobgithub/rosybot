package com.rosy.dbloader.dbloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.Request;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.*;

import com.opencsv.CSVReader;

public class RosyProductLoader implements RequestHandler<InputClass, String> {

	private static final Regions AWS_REGION = Regions.US_EAST_1;
	private String ResponseMessage;
	
	private static final String mySqlConnStr = System.getenv("DB_CONN_STR");
	private static final int OFFER_COUNT_LIMIT = 3;
	private static final String DB_USER = "mattbob";
	private static final String DB_PSWD = "Bronco20";

	private enum RosyCategory {
		FUNERAL, NOTFUNERALSYMPATHY, BIRTHDAY, GETWELL, THANKYOU, EVERYDAY, ROMANCE, NEWBABY
	};

	public String handleRequest(InputClass input, Context context) {
		LambdaLogger ll = context.getLogger();
		String bucketName = input.getBucketName();
		String keyName = input.getKeyName();
		String tableName = input.getTableName();

		context.getLogger().log("bucketName:" + bucketName);
		context.getLogger().log(" keyName:" + keyName);
		context.getLogger().log(" tableName:" + tableName);

		// get s3object

		// AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

		AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
		try {
			System.out.println("Downloading an object");

			GetObjectRequest rangeObjectRequest = new GetObjectRequest(bucketName, keyName);
			S3Object fileObject = s3Client.getObject(rangeObjectRequest);

			int deleteCounter = 0;
			// Get time from DB server
			Connection conn = null;
			Statement stmt = null;
			Statement stmt2 = null;
			try {
	
				ll.log("getting mysql aurora connection now");
				conn = DriverManager.getConnection(mySqlConnStr, DB_USER, DB_PSWD);
				stmt = conn.createStatement();
				stmt.executeUpdate("TRUNCATE TABLE " + tableName);
				ll.log("TABLE TRUCATED");
				// now read each line from csv

				InputStreamReader isr = new InputStreamReader(fileObject.getObjectContent());
				BufferedReader br = new BufferedReader(isr);
				CSVReader CSVreader = new CSVReader(br);

				String[] nextLine;
				int counter = 0;
				CSVreader.readNext();
				while ((nextLine = CSVreader.readNext()) != null) {
					String[] prodLine = nextLine[0].split("\t");

					int prodLineLength = prodLine.length;
					ll.log(StringUtils.fromInteger(prodLineLength));
					String insertStatment = null;
				
						insertStatment = "INSERT INTO RosyProduct (name, code, price, imageUrl, category, rosyCategory, rosyOfferOrder) values(";

					

					if (!StringUtils.isNullOrEmpty(prodLine[0])) {
						// name
						prodLine[0] = prodLine[0].replace('\"', ' ');
						insertStatment = insertStatment + "\"" + prodLine[0] + "\",";
					}
					if (!StringUtils.isNullOrEmpty(prodLine[1])) {
						// code
						insertStatment = insertStatment + "\"" + prodLine[1] + "\",";
					}
					if (!StringUtils.isNullOrEmpty(prodLine[2])) {
						// price
						insertStatment = insertStatment + "\"" + prodLine[2] + "\",";
					}
					if (!StringUtils.isNullOrEmpty(prodLine[3])) {
						// imageUrl
						insertStatment = insertStatment + "\"" + prodLine[3] + "\",";
					}
					if (!StringUtils.isNullOrEmpty(prodLine[4])) {
						// category
						insertStatment = insertStatment + "\"" + prodLine[4] + "\",";
					}
					if (!StringUtils.isNullOrEmpty(prodLine[5])) {
						// rosyCategory
						insertStatment = insertStatment + "\"" + prodLine[5] + "\"";
					}

					if (prodLine.length > 6) {
						if (!StringUtils.isNullOrEmpty(prodLine[6])) {
							// rosyOfferOrder
							insertStatment = insertStatment + "," + prodLine[6];
						}
					}else
					{
						insertStatment = insertStatment + "," + "99";
					}

					insertStatment = insertStatment + ");";

					//ll.log(insertStatment);
					stmt2 = conn.createStatement();
					stmt2.executeUpdate(insertStatment);
					counter++;
				}
				ResponseMessage = StringUtils.fromInteger(counter) + " ROWS UPDATED IN TABLE:" + tableName
						+ " FROM TAB DELIMITED S3 FILE: bucketName:" + bucketName + "  keyName:" + keyName;
				
			} catch (Exception e) {
				e.printStackTrace();
				ll.log("Caught exception: " + e.getMessage());
			} finally{
				try {
					conn.close();
					stmt.close();
					stmt2.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which" + " means your request made it "
					+ "to Amazon S3, but was rejected with an error response" + " for some reason.");
			System.out.println("bucketName:" + bucketName + "  keyName:" + keyName);
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means" + " the client encountered "
					+ "an internal error while trying to " + "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}

		return ResponseMessage;
	}

}
