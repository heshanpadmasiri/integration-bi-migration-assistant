import ballerina/log;

public type Context record {|
    anydata payload = ();
|};

public function inboundVmFlow(Context ctx) {
    log:printInfo(string `Received a message: ${ctx.payload.toString()}`);
}

public function outboundVmFlow(Context ctx) {
    worker W returns error? {
        // VM Inbound Endpoint
        anydata receivedPayload = <- function;
        ctx.payload = receivedPayload;
        inboundVmFlow(ctx);
    }

    // set payload
    string payload0 = "Hello World";
    ctx.payload = payload0;

    // VM Outbound Endpoint
    ctx.payload -> W;
}
