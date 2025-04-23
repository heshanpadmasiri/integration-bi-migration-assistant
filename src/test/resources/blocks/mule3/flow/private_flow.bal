import ballerina/http;
import ballerina/log;

public type InboundProperties record {|
    http:Response response;
    http:Request request;
    map<string> uriParams;
|};

public type Context record {|
    anydata payload;
    InboundProperties inboundProperties;
|};

public listener http:Listener config = new (8081, {host: "0.0.0.0"});

service /mule3 on config {
    Context ctx;

    function init() {
        self.ctx = {payload: (), inboundProperties: {response: new, request: new, uriParams: {}}};
    }

    resource function get .(http:Request request) returns http:Response|error {
        self.ctx.inboundProperties.request = request;
        return _invokeEndPoint0_(self.ctx);
    }
}

public function _invokeEndPoint0_(Context ctx) returns http:Response|error {
    log:printInfo("xxx: logger invoked via http end point");
    demoPrivateFlow(ctx);
    log:printInfo("xxx: end of main flow");

    ctx.inboundProperties.response.setPayload(ctx.payload);
    return ctx.inboundProperties.response;
}

public function demoPrivateFlow(Context ctx) {
    log:printInfo("xxx: private flow invoked");
}
