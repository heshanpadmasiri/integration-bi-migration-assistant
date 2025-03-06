import ballerina/http;

const string client_404_RecordNotFound = "Record Not Found";
listener http:Listener creditapp_module_EquifaxScore_listener = new (8081, {host: "localhost"});

service /y54cuadtcxtfstqs3rux2gfdaxppoqgc on creditapp_module_EquifaxScore_listener {
    resource function post creditscore(GiveNewSchemaNameHere input) returns SuccessSchema|http:NotFound|http:InternalServerError|client_404_RecordNotFound {
        return creditapp_module_EquifaxScore_start(input);
    }
}

function activityExtension(xml input) returns xml {
    return input;
}

function creditapp_module_EquifaxScore_start(GiveNewSchemaNameHere input) returns SuccessSchema {
    xml inputXML = toXML(input);
    xml xmlResult = process_creditapp_module_EquifaxScore(inputXML);
    SuccessSchema result = convertToSuccessSchema(xmlResult);
    return result;
}

function invoke(xml input) returns xml {
    http:Client var0 = checkpanic new ("/");
    json var1 = checkpanic var0->post("/creditscore", input);
    return fromJson(var1);
}

function process_creditapp_module_EquifaxScore(xml input) returns xml {
    worker start_worker {
        xml result0 = receiveEvent(input);
        result0 -> StartTopost;
    }
    worker StartTopost {
        xml result0 = <- start_worker;
        result0 -> invoke_worker;
    }
    worker postToEnd {
        xml result0 = <- invoke_worker;
        result0 -> activityExtension_worker;
    }
    worker activityExtension_worker {
        xml input0 = <- postToEnd;
        xml combinedInput = input0;
        xml output = activityExtension(combinedInput);
        output -> function;
    }
    worker invoke_worker {
        xml input0 = <- StartTopost;
        xml combinedInput = input0;
        xml output = invoke(combinedInput);
        output -> postToEnd;
    }
    xml result0 = <- activityExtension_worker;
    xml result = result0;
    return result;
}

function receiveEvent(xml input) returns xml {
    return input;
}
