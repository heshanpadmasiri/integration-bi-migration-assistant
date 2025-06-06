import ballerina/http;
import ballerina/log;

public type InboundProperties record {|
    http:Request request;
    http:Response response;
    map<string> uriParams = {};
|};

public type Context record {|
    anydata payload = ();
    InboundProperties inboundProperties;
|};

public listener http:Listener config = new (8081);

service /mule3 on config {
    resource function get .(http:Request request) returns http:Response|error {
        Context ctx = {inboundProperties: {request, response: new}};

        // TODO: UNSUPPORTED MULE BLOCK ENCOUNTERED. MANUAL CONVERSION REQUIRED.
        // ------------------------------------------------------------------------
        // <db:select-unsupported config-ref="MySQL_Configuration" xmlns:doc="http://www.mulesoft.org/schema/mule/documentation" doc:name="Database" xmlns:db="http://www.mulesoft.org/schema/mule/db">
        //             <db:parameterized-query><![CDATA[SELECT * from users;]]></db:parameterized-query>
        //         </db:select-unsupported>
        // ------------------------------------------------------------------------

        // TODO: UNSUPPORTED MULE BLOCK ENCOUNTERED. MANUAL CONVERSION REQUIRED.
        // ------------------------------------------------------------------------
        // <json:object-to-json-transformer-unsupported xmlns:doc="http://www.mulesoft.org/schema/mule/documentation" doc:name="Object to JSON" xmlns:json="http://www.mulesoft.org/schema/mule/json"/>
        // ------------------------------------------------------------------------

        log:printInfo(string `Users details: ${ctx.payload.toString()}`);

        ctx.inboundProperties.response.setPayload(ctx.payload);
        return ctx.inboundProperties.response;
    }
}

// TODO: UNSUPPORTED MULE BLOCK ENCOUNTERED. MANUAL CONVERSION REQUIRED.
// ------------------------------------------------------------------------
// <db:mysql-config-unsupported database="test_db" xmlns:doc="http://www.mulesoft.org/schema/mule/documentation" doc:name="MySQL Configuration" host="localhost" name="MySQL_Configuration" password="admin123" port="3306" user="root" xmlns:db="http://www.mulesoft.org/schema/mule/db"/>
// ------------------------------------------------------------------------

