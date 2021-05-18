package com.amazonaws.lambda.demo;

import java.io.File;
import java.io.IOException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.StringUtils;

public class LambdaFunctionHandler implements RequestHandler<DynamodbEvent, Integer> {

	private final String FILE_NAME = "filename";
	private final String MIME_TYPE = "mime-type";
    private final String FULLTEXT_REF = "fulltext-ref";
    private final String DYNAMODB_RECORD_ID = "dynamodb-record-id";
    private final String BUCKET_NAME = "mybucket-05-2021";
	
	@Override
	public Integer handleRequest(DynamodbEvent event, Context context) {
		context.getLogger().log("Received event: " + event);
		for (DynamodbStreamRecord record : event.getRecords()) {
			context.getLogger().log(record.getEventID());
			context.getLogger().log(record.getEventName());
			context.getLogger().log(record.getDynamodb().toString());
			String fileName = record.getDynamodb().getNewImage().get(FILE_NAME).getS();
			String mimeType = record.getDynamodb().getNewImage().get(MIME_TYPE).getS();
			String fullTextRef = record.getDynamodb().getNewImage().get(FULLTEXT_REF).getS();
			uploadImageToS3(fileName,mimeType,fullTextRef,context);
			context.getLogger().log("filename ::" + fileName + "mimeType ::" + mimeType + " fullTextRef :" + fullTextRef);
		}
		return event.getRecords().size();
	}

	private void uploadImageToS3(String fileName, String mimeType,String fullTextRef,Context context) {
		 Regions clientRegion = Regions.US_EAST_1;
	     String fileObjKeyName = getFilePath(fileName);
	     //String filePath = fileObjKeyName;
	     try {
	            AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion).build();
	            // Upload a file as a new object with ContentType and title specified.
	            String fileArray[] = fileName.split("\\.");
	            File file = File.createTempFile(fileArray[0], fileArray[1]);
	            PutObjectRequest request = new PutObjectRequest(BUCKET_NAME, fileObjKeyName, file);
	            ObjectMetadata metadata = new ObjectMetadata();
	            metadata.setContentType(mimeType);
	            metadata.addUserMetadata(DYNAMODB_RECORD_ID, fullTextRef);
	            request.setMetadata(metadata);
	            s3Client.putObject(request);
	        } catch (AmazonServiceException e) {
	        	context.getLogger().log("AWS Exception ::"+e.getStackTrace()[0] +":"+ e.getMessage());
	        } catch (SdkClientException e) {
	        	context.getLogger().log("AWS Exception ::"+e.getStackTrace()[0] +":"+ e.getMessage());
	        } catch (IOException e) {
	        	context.getLogger().log("IO Exception ::"+e.getStackTrace()[0] +":"+ e.getMessage());
	        }   
	    }

	private String getFilePath(String fileName) {
		if(StringUtils.isNullOrEmpty(fileName)) {
			return "other/"+fileName;
		}	
		if(fileName.contains("html")) {
			return "html/"+fileName;
		}
		if(fileName.contains("xml")) {
			return "xml/"+fileName;
		}	
		return "other/"+fileName;
	}
}