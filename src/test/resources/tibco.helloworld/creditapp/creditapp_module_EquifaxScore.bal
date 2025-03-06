import ballerina/data.xmldata;
import ballerina/http;

const string client_404_RecordNotFound = "Record Not Found";
listener http:Listener creditapp_module_EquifaxScore_listener = new (8081, {host: "localhost"});

service /y54cuadtcxtfstqs3rux2gfdaxppoqgc on creditapp_module_EquifaxScore_listener {
    resource function post creditscore(GiveNewSchemaNameHere input) returns SuccessSchema|http:NotFound|http:InternalServerError|client_404_RecordNotFound {
        return creditapp_module_EquifaxScore_start(input);
    }
}

function activityExtension(xml input) returns xml {
}

public function creditapp_module_EquifaxScore_start(GiveNewSchemaNameHere input) returns SuccessSchema {
    xml inputXML = toXML(input);
    xml xmlResult = process_creditapp_module_EquifaxScore(inputXML);
    SuccessSchema result = convertToSuccessSchema(xmlResult);
    return result;
}

function invoke(xml input) returns xml {
}

function process_creditapp_module_EquifaxScore(xml input) returns xml {
    worker start_worker {
        xml output = activityExtension(input);
        output -> postToEnd;
    }
    worker postToEnd {
        xml v0 = <- start_worker;
        xml output0 = invoke(v0);
        output0 -> StartTopost;
    }
    worker StartTopost {
        xml v0 = <- postToEnd;
        xml output0 = receiveEvent(v0);
        output0 -> function;
    }
    xml result0 = <- StartTopost;
    return result0;
}

function receiveEvent(xml input) returns xml {
}

function toXML(map<anydata> data) returns xml {
    return checkpanic xmldata:toXml(data);
}

function convertToSuccessSchema(xml input) returns SuccessSchema {
    return checkpanic xmldata:parseAsType(input);
}
