import ballerina/http;

type ErrorReport record {
    string StackTrace;
    string Msg;
    string FullClass;
    string Class;
    string ProcessStack;
    string MsgCode;
    anydata Data;
};

type OptionalErrorReport record {
    string StackTrace;
    string Msg;
    string FullClass;
    string Class;
    string ProcessStack;
    string MsgCode;
    anydata Data;
};

type FaultDetail record {
    string ActivityName;
    anydata Data;
    string Msg;
    string MsgCode;
    string ProcessStack;
    string StackTrace;
    string FullClass;
    string Class;
};

type ProcessContext record {
    string JobId;
    string ApplicationName;
    string EngineName;
    string ProcessInstanceId;
    string CustomJobId;
    string TrackingInfo;
};

type CorrelationValue string;

type ActivityExceptionType record {
    string msg;
    string msgCode;
};

type ActivityTimedOutExceptionType record {
    *ActivityExceptionType;
};

type DuplicateKeyExceptionType record {
    *ActivityExceptionType;
    string duplicateKey;
    string previousJobID;
};

type ActivityException ActivityExceptionType;

type ActivityTimedOutException ActivityTimedOutExceptionType;

type DuplicateKeyException DuplicateKeyExceptionType;

type ActivityErrorDataType ActivityTimedOutException|()|http:NotFound|http:InternalServerError;

type ActivityErrorData ActivityErrorDataType;

type SuccessSchema record {
    int FICOScore;
    int NoOfInquiries;
    string Rating;
};

type GiveNewSchemaNameHere record {
    string DOB;
    string FirstName;
    string LastName;
    string SSN;
};

type CreditScoreSuccessSchema record {
    SuccessSchema EquifaxResponse;
    SuccessSchema ExperianResponse;
    SuccessSchema TransUnionResponse;
};

const string client_404_RecordNotFound = "Record Not Found";
listener http:Listener LISTENER = new (8080, {host: "localhost"});

service /y54cuadtcxtfstqs3rux2gfdaxppoqgc on LISTENER {
    resource function post creditscore(GiveNewSchemaNameHere input) returns SuccessSchema|http:NotFound|http:InternalServerError|client_404_RecordNotFound {
    }
}

service / on LISTENER {
    resource function post creditscore(GiveNewSchemaNameHere input) returns SuccessSchema|http:NotFound|http:InternalServerError {
    }
}
