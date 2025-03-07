type JSONParserExceptionType record {
    *JSONActivityException;
};

type JSONRenderExceptionType record {
    *JSONActivityException;
};

type HttpClientExceptionType record {
    *HttpResponseExceptionType;
};

type JSONActivityException record {
    string msg;
    string msgCode;
};

type ActivityTimedOutExceptionType record {
    *ActivityExceptionType;
};

type RequestActivityOutput record {|
    string Header;
    statusLineType statusLine;
    int binaryContent;
    string asciiContent;
    string filePath;
    outputHeadersType Headers;
    DynamicHeaders DynamicHeaders;
    anydata...;
|};

type GiveNewSchemaNameHere record {
    string DOB;
    string FirstName;
    string LastName;
    string SSN;
};

type CorrelationValue string;

type mimeEnvelopeElementType record {
    mimePartType mimePart;
};

type ProcessContext record {
    string JobId;
    string ApplicationName;
    string EngineName;
    string ProcessInstanceId;
    string CustomJobId;
    string TrackingInfo;
};

type HttpCommunicationExceptionType record {
    *BaseExceptionType;
};

type ErrorReport record {
    string StackTrace;
    string Msg;
    string FullClass;
    string Class;
    string ProcessStack;
    string MsgCode;
    anydata Data;
};

type BaseExceptionType record {
    string msg;
    string msgCode;
};

type ActivityInputClass ActivityInputClassType;

type statusLineType record {
    string httpVersion;
    int statusCode;
    string reasonPhrase;
};

type HttpClientException HttpClientExceptionType;

type JSONTransformExceptionType record {
    *JSONActivityException;
};

type RequestActivityInput record {|
    string Host;
    int Port;
    string Method;
    string RequestURI;
    string PostData;
    string QueryString;
    int Timeout;
    inputHeadersType Headers;
    DynamicHeaders DynamicHeaders;
    parametersType parameters;
    anydata...;
|};

type HttpCommunicationException HttpCommunicationExceptionType;

type HTTPRequestConfig record {
    string Method;
    string RequestURI;
    json PostData = "";
    map<string> Headers = {};
    map<string> parameters = {};
};

type ActivityException ActivityExceptionType;

type UnsupportedEncodingException UnsupportedEncodingExceptionType;

type ExperianResponseSchemaElementType record {
    int fiCOScore;
    string rating;
    int noOfInquiries;
};

type JSONTransformException JSONTransformExceptionType;

type outputHeadersType record {
    string Allow;
    string Content_Type;
    string Content_Length;
    string Content_Encoding;
    string Date;
    string Location;
    string Set_Cookie;
    string Pragma;
};

type HttpMessageType record {
    HeadersType headers;
    int binaryContent;
    string asciiContent;
};

type JSONParserException JSONParserExceptionType;

type Header record {
    string Name;
    string Value;
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

type inputHeadersType record {
    string Accept;
    string Accept_Charset;
    string Accept_Encoding;
    string Content_Type;
    string Cookie;
    string Pragma;
};

type DynamicHeaders record {
    Header Header;
};

type SuccessSchema record {
    int FICOScore;
    int NoOfInquiries;
    string Rating;
};

type StatusLineType record {
    string httpVersion;
    int statusCode;
    string reasonPhrase;
};

type HttpExceptionType record {
    *BaseExceptionType;
};

type ActivityOutputClassType record {
    string jsonString;
};

type ActivityTimedOutException BaseExceptionType;

type mimeHeadersType record {|
    string content_disposition;
    string content_type;
    string content_transfer_encoding;
    string content_id;
    anydata...;
|};

type mimePartType record {|
    mimeHeadersType mimeHeaders;
    int|string...;
|};

type ValidationException ValidationExceptionType;

type JSONRenderException JSONRenderExceptionType;

type DuplicateKeyExceptionType record {
    *ActivityExceptionType;
    string duplicateKey;
    string previousJobID;
};

type ValidationExceptionType record {
    *JSONActivityException;
};

type HttpResponseExceptionType record {
    *BaseExceptionType;
    StatusLineType statusLine;
    HttpMessageType httpMessage;
};

type HeadersType record {|
    anydata...;
|};

type HttpResponseException HttpResponseExceptionType;

type ActivityInputClassType record {
    string jsonString;
};

type JSONRestException JSONRestExceptionType;

type ExperianResponseSchemaElement ExperianResponseSchemaElementType;

type ActivityErrorDataType JSONParserException|();

type OptionalErrorReport record {
    string StackTrace;
    string Msg;
    string FullClass;
    string Class;
    string ProcessStack;
    string MsgCode;
    anydata Data;
};

type HttpException HttpExceptionType;

type mimeEnvelopeElement mimeEnvelopeElementType;

type HttpServerException HttpServerExceptionType;

type parametersType record {
    string ssn;
};

type ActivityExceptionType record {
    string msg;
    string msgCode;
};

type JSONRestExceptionType record {
    *JSONActivityException;
};

type ActivityOutputClass ActivityOutputClassType;

type HttpServerExceptionType record {
    *HttpResponseExceptionType;
};

type DuplicateKeyException DuplicateKeyExceptionType;

type CreditScoreSuccessSchema record {
    SuccessSchema EquifaxResponse;
    SuccessSchema ExperianResponse;
    SuccessSchema TransUnionResponse;
};

type UnsupportedEncodingExceptionType record {
    *JSONActivityException;
    string encoding;
};

type ActivityErrorData ActivityErrorDataType;
