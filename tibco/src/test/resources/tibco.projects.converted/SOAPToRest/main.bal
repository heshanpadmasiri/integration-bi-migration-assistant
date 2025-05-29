import ballerina/http;

public listener http:Listener MainConnection_sharedhttp = new (9090, {host: "localhost"});

service on MainConnection_sharedhttp {
    resource function 'default [string... path](xml input) returns xml {
        xml inputVal = xml `<root>
    <item>
        ${input}
    </item>
</root>`;
        map<xml> paramXML = {post: inputVal};
        xml result = start_Processes_Main_process(input, paramXML);
        return result;
    }
}

xmlns "http://xmlns.tibco.com/bw/process/2003" as pd;
xmlns "http://www.w3.org/1999/XSL/Transform" as xsl;
xmlns "http://www.tibco.com/pe/EngineTypes" as ns;
xmlns "http://www.tibco.com/namespaces/tnt/plugins/json" as ns1;
xmlns "http://www.w3.org/2001/XMLSchema" as xsd;
