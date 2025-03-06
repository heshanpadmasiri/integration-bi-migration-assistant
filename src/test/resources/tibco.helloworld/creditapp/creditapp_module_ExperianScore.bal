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

function activityExtension_8(xml input) returns xml {
    return input;
}

function activityExtension_9(xml input) returns xml {
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
        xml result0 = receiveEvent_7(input);
        result0 -> StartToSendHTTPRequest;
    }
    worker ParseJSONToEnd {
        xml result0 = <- activityExtension_11_worker;
        result0 -> activityExtension_8_worker;
    }
    worker RenderJSONToSendHTTPRequest {
        xml result0 = <- activityExtension_10_worker;
        result0 -> activityExtension_9_worker;
    }
    worker SendHTTPRequestToEnd {
        xml result0 = <- activityExtension_9_worker;
        result0 -> activityExtension_11_worker;
    }
    worker StartToSendHTTPRequest {
        xml result0 = <- start_worker;
        result0 -> activityExtension_10_worker;
    }
    worker activityExtension_10_worker {
        xml input0 = <- StartToSendHTTPRequest;
        xml combinedInput = input0;
        xml output = activityExtension_10(combinedInput);
        output -> RenderJSONToSendHTTPRequest;
    }
    worker activityExtension_11_worker {
        xml input0 = <- SendHTTPRequestToEnd;
        xml combinedInput = input0;
        xml output = activityExtension_11(combinedInput);
        output -> ParseJSONToEnd;
    }
    worker activityExtension_8_worker {
        xml input0 = <- ParseJSONToEnd;
        xml combinedInput = input0;
        xml output = activityExtension_8(combinedInput);
        output -> function;
    }
    worker activityExtension_9_worker {
        xml input0 = <- RenderJSONToSendHTTPRequest;
        xml combinedInput = input0;
        xml output = activityExtension_9(combinedInput);
        output -> SendHTTPRequestToEnd;
    }
    xml result0 = <- activityExtension_8_worker;
    xml result = result0;
    return result;
}

function receiveEvent_7(xml input) returns xml {
    return input;
}
