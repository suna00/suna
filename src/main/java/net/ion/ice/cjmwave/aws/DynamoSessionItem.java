package net.ion.ice.cjmwave.aws;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.nio.ByteBuffer;

@DynamoDBTable(tableName = "MWAVE_SESSION")
public class DynamoSessionItem {

    public static final String SESSION_ID_ATTRIBUTE_NAME = "sessionId";
    public static final String SESSION_DATA_ATTRIBUTE_NAME = "sessionData";

    private String sessionId;
    private ByteBuffer sessionData;

    public DynamoSessionItem() {
    }

    public DynamoSessionItem(String id) {
        this.sessionId = id;
    }

    @DynamoDBHashKey(attributeName = SESSION_ID_ATTRIBUTE_NAME)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @DynamoDBAttribute(attributeName = SESSION_DATA_ATTRIBUTE_NAME)
    public ByteBuffer getSessionData() {
        return sessionData;
    }

    public void setSessionData(ByteBuffer sessionData) {
        this.sessionData = sessionData;
    }

}