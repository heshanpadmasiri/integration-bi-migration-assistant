import ballerina/http;

listener http:Listener creditapp_module_ExperianScore_listener = new (8082, {host: "localhost"});

service / on creditapp_module_ExperianScore_listener {
    resource function post creditscore(GiveNewSchemaNameHere input) returns ExperianResponseSchemaElement|http:NotFound|http:InternalServerError {
        return creditapp_module_ExperianScore_start(input);
    }
}

function activityExtension_10(xml input) returns xml {
    return input;
}

function activityExtension_11(xml input) returns xml {
    return input;
}

function activityExtension_12(xml input) returns xml {
    return input;
}

function activityExtension_13(xml input) returns xml {
    return input;
}

function creditapp_module_ExperianScore_start(GiveNewSchemaNameHere input) returns ExperianResponseSchemaElement {
    xml inputXML = toXML(input);
    xml xmlResult = process_creditapp_module_ExperianScore(inputXML);
    ExperianResponseSchemaElement result = convertToExperianResponseSchemaElement(xmlResult);
    return result;
}

function process_creditapp_module_ExperianScore(xml input) returns xml {
    worker start_worker {
        xml output = activityExtension_10(input);
        output -> ParseJSONToEnd;
    }
    worker SendHTTPRequestToEnd {
        xml v0 = <- ParseJSONToEnd;
        xml output0 = activityExtension_11(v0);
        output0 -> RenderJSONToSendHTTPRequest;
    }
    worker RenderJSONToSendHTTPRequest {
        xml v0 = <- SendHTTPRequestToEnd;
        xml output0 = activityExtension_12(v0);
        output0 -> StartToSendHTTPRequest;
    }
    worker ParseJSONToEnd {
        xml v0 = <- start_worker;
        xml output0 = activityExtension_13(v0);
        output0 -> SendHTTPRequestToEnd;
    }
    worker StartToSendHTTPRequest {
        xml v0 = <- RenderJSONToSendHTTPRequest;
        xml output0 = receiveEvent_9(v0);
        output0 -> function;
    }
    xml result0 = <- StartToSendHTTPRequest;
    return result0;
}

function receiveEvent_9(xml input) returns xml {
    return input;
}
