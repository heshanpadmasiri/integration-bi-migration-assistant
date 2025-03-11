import ballerina/http;
import ballerina/xslt;

configurable string host = ?;
listener http:Listener creditapp_module_ExperianScore_listener = new (8082, {host: "localhost"});

service / on creditapp_module_ExperianScore_listener {
    resource function post creditscore(GiveNewSchemaNameHere input) returns ExperianResponseSchemaElement|http:NotFound|http:InternalServerError {
        return creditapp_module_ExperianScore_start(input);
    }
}

function activityExtension_10(xml input, map<xml> context) returns xml {
    xml var0 = checkpanic xslt:transform(input, xml `<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tns6="/y54cuadtcxtfstqs3rux2gfdaxppoqgc/T1535409245354Converted/JsonSchema" xmlns:tns="http://tns.tibco.com/bw/activity/jsonRender/xsd/input/55832ae5-2a37-4b37-8392-a64537f49367" version="2.0">
    <xsl:param name="Start"/>
    <xsl:template name="RenderJSON-input" match="/">
        <tns:InputElement>
            <tns:dob>
                <xsl:value-of select="$Start/tns6:DOB"/>
            </tns:dob>
            <tns:firstName>
                <xsl:value-of select="$Start/tns6:FirstName"/>
            </tns:firstName>
            <tns:lastName>
                <xsl:value-of select="$Start/tns6:LastName"/>
            </tns:lastName>
            <tns:ssn>
                <xsl:value-of select="$Start/tns6:SSN"/>
            </tns:ssn>
        </tns:InputElement>
    </xsl:template>
</xsl:stylesheet>`, context);
    context["RenderJSON"] = var0;
    return var0;
}

function activityExtension_11(xml input, map<xml> context) returns xml {
    xml var0 = checkpanic xslt:transform(input, xml `<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tns5="activity.jsonParser.input+a3fa07a6-0270-48b7-ba84-7de6924acb3d+ActivityInputType" version="2.0"><xsl:param name="SendHTTPRequest"/><xsl:template name="ParseJSON-input" match="/"><tns5:ActivityInputClass><jsonString><xsl:value-of select="$SendHTTPRequest/asciiContent"/></jsonString></tns5:ActivityInputClass></xsl:template></xsl:stylesheet>`, context);
    context["ParseJSON"] = var0;
    return var0;
}

function activityExtension_8(xml input, map<xml> context) returns xml {
    xml var0 = checkpanic xslt:transform(input, xml `<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tns3="http://tns.tibco.com/bw/json/1535671685533" version="2.0"><xsl:param name="ParseJSON"/><xsl:template name="End-input" match="/"><tns3:ExperianResponseSchemaElement><xsl:if test="$ParseJSON/tns3:fiCOScore"><tns3:fiCOScore><xsl:value-of select="$ParseJSON/tns3:fiCOScore"/></tns3:fiCOScore></xsl:if><xsl:if test="$ParseJSON/tns3:rating"><tns3:rating><xsl:value-of select="$ParseJSON/tns3:rating"/></tns3:rating></xsl:if><xsl:if test="$ParseJSON/tns3:noOfInquiries"><tns3:noOfInquiries><xsl:value-of select="$ParseJSON/tns3:noOfInquiries"/></tns3:noOfInquiries></xsl:if></tns3:ExperianResponseSchemaElement></xsl:template></xsl:stylesheet>`, context);
    return var0;
}

function activityExtension_9(xml input, map<xml> context) returns xml {
    xml var0 = checkpanic xslt:transform(input, xml `<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tns4="http://tns.tibco.com/bw/activity/sendhttprequest/input+255a70f6-2bf4-4f72-928d-3fe2a72ce7a0+RequestActivityInput" xmlns:tns6="/y54cuadtcxtfstqs3rux2gfdaxppoqgc/T1535409245354Converted/JsonSchema" version="2.0"><xsl:param name="RenderJSON"/><xsl:param name="Start"/><xsl:template name="SendHTTPRequest-input" match="/"><tns4:RequestActivityInput><Method><xsl:value-of select="'POST'"/></Method><RequestURI><xsl:value-of select="'/creditscore'"/></RequestURI><PostData><xsl:value-of select="$RenderJSON/jsonString"/></PostData><Headers><Accept><xsl:value-of select="'application/json'"/></Accept><Content-Type><xsl:value-of select="'application/json'"/></Content-Type></Headers><parameters><xsl:if test="$Start/tns6:SSN"><ssn><xsl:value-of select="$Start/tns6:SSN"/></ssn></xsl:if></parameters></tns4:RequestActivityInput></xsl:template></xsl:stylesheet>`, context);
    HTTPRequestConfig var1 = convertToHTTPRequestConfig(var0);
    http:Client var2 = checkpanic new (host);
    string var3 = getRequestPath(var1);
    json var4 = checkpanic var2->/var3.post(var1.PostData, var1.Headers);
    xml var5 = fromJson(var4);
    context["SendHTTPRequest"] = var5;
    return var5;
}

function creditapp_module_ExperianScore_start(GiveNewSchemaNameHere input) returns ExperianResponseSchemaElement {
    xml inputXML = toXML(input);
    xml xmlResult = process_creditapp_module_ExperianScore(inputXML);
    ExperianResponseSchemaElement result = convertToExperianResponseSchemaElement(xmlResult);
    return result;
}

function process_creditapp_module_ExperianScore(xml input) returns xml {
    map<xml> context = {};
    worker start_worker {
        xml result0 = receiveEvent_7(input, context);
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
        xml output = activityExtension_10(combinedInput, context);
        output -> RenderJSONToSendHTTPRequest;
    }
    worker activityExtension_11_worker {
        xml input0 = <- SendHTTPRequestToEnd;
        xml combinedInput = input0;
        xml output = activityExtension_11(combinedInput, context);
        output -> ParseJSONToEnd;
    }
    worker activityExtension_8_worker {
        xml input0 = <- ParseJSONToEnd;
        xml combinedInput = input0;
        xml output = activityExtension_8(combinedInput, context);
        output -> function;
    }
    worker activityExtension_9_worker {
        xml input0 = <- RenderJSONToSendHTTPRequest;
        xml combinedInput = input0;
        xml output = activityExtension_9(combinedInput, context);
        output -> SendHTTPRequestToEnd;
    }
    xml result0 = <- activityExtension_8_worker;
    xml result = result0;
    return result;
}

function receiveEvent_7(xml input, map<xml> context) returns xml {
    return input;
}
