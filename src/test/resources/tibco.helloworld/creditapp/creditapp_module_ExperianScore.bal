import ballerina/data.xmldata;
import ballerina/http;

listener http:Listener LISTENER = new (8082, {host: "localhost"});

service / on LISTENER {
    resource function post creditscore(GiveNewSchemaNameHere input) returns ExperianResponseSchemaElement|http:NotFound|http:InternalServerError {
        return creditapp_module_ExperianScore_start(input);
    }
}

function activityExtension(xml input) returns xml {
}

function activityExtension_2(xml input) returns xml {
}

function activityExtension_3(xml input) returns xml {
}

function activityExtension_4(xml input) returns xml {
}

public function creditapp_module_ExperianScore_start(GiveNewSchemaNameHere input) returns ExperianResponseSchemaElement {
    xml inputXML = toXML(input);
    xml xmlResult = process_creditapp_module_ExperianScore(inputXML);
    ExperianResponseSchemaElement result = convertToExperianResponseSchemaElement(xmlResult);
    return result;
}

function process_creditapp_module_ExperianScore(xml input) returns xml {
    worker start_worker {
        xml output = activityExtension(input);
        output -> ParseJSONToEnd;
    }
    worker SendHTTPRequestToEnd {
        xml v0 = <- ParseJSONToEnd;
        xml output0 = activityExtension_2(v0);
        output0 -> RenderJSONToSendHTTPRequest;
    }
    worker RenderJSONToSendHTTPRequest {
        xml v0 = <- SendHTTPRequestToEnd;
        xml output0 = activityExtension_3(v0);
        output0 -> StartToSendHTTPRequest;
    }
    worker ParseJSONToEnd {
        xml v0 = <- start_worker;
        xml output0 = activityExtension_4(v0);
        output0 -> SendHTTPRequestToEnd;
    }
    worker StartToSendHTTPRequest {
        xml v0 = <- RenderJSONToSendHTTPRequest;
        xml output0 = receiveEvent(v0);
        output0 -> function;
    }
    xml result0 = <- StartToSendHTTPRequest;
    return result0;
}

function receiveEvent(xml input) returns xml {
}

function toXML(map<anydata> data) returns xml {
    return checkpanic xmldata:toXml(data);
}

function convertToExperianResponseSchemaElement(xml input) returns ExperianResponseSchemaElement {
    return checkpanic xmldata:parseAsType(input);
}
