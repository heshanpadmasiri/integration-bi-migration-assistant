import ballerina/data.xmldata;

@xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
public type Response record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup sequenceGroup;
|};

@xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
public type SuccessSchema record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup sequenceGroup;
|};

@xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
public type Request record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup1 sequenceGroup1;
|};

@xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
public type RequestType record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup1 sequenceGroup1;
|};

@xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
public type SequenceGroup record {|
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 1}
    int FICOScore?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 2}
    string Rating?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 3}
    int NoOfInquiries?;
|};

@xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
public type SequenceGroup1 record {|
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 1}
    string SSN?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 2}
    string FirstName?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 3}
    string LastName?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 4}
    string DOB?;
|};

@xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
public type SequenceGroup2 record {|
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 1}
    int FICOScore?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 2}
    string Rating?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 3}
    int NoOfInquiries?;
|};

@xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
public type SequenceGroup3 record {|
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 1}
    string SSN?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 2}
    string FirstName?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 3}
    string LastName?;
    @xmldata:Namespace {uri: "/T1535753828744Converted/JsonSchema"}
    @xmldata:SequenceOrder {value: 4}
    string DOB?;
|};

@xmldata:Namespace {uri: "http://www.example.com/namespaces/tns/1535845694732"}
public type Element record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup4 sequenceGroup4;
|};

@xmldata:Namespace {uri: "http://www.example.com/namespaces/tns/1535845694732"}
public type SequenceGroup4 record {|
    @xmldata:Namespace {uri: "http://www.example.com/namespaces/tns/1535845694732"}
    @xmldata:SequenceOrder {value: 1}
    string ssn;
|};

@xmldata:Namespace {uri: "http://www.example.com/namespaces/tns/1535845694732"}
public type SequenceGroup5 record {|
    @xmldata:Namespace {uri: "http://www.example.com/namespaces/tns/1535845694732"}
    @xmldata:SequenceOrder {value: 1}
    string ssn;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type messageBody record {|
    string \#content;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type tmessageBody string;

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type httpTransportHeaders record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup6 sequenceGroup6;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type httpTransportResponseHeaders record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup7 sequenceGroup7;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type httpTransportFaultHeaders record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup8 sequenceGroup8;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type dynamicHeadersTypeDetails record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup9 sequenceGroup9;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type dynamicHeadersType record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup10 sequenceGroup10;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type httpHeaders record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup6 sequenceGroup6;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type httpResponseHeaders record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup7 sequenceGroup7;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type httpFaultHeaders record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup8 sequenceGroup8;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type statusLineType record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup11 sequenceGroup11;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type statusLine record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup11 sequenceGroup11;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type client4XXErrorType record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup12 sequenceGroup12;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type client4XXError record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup12 sequenceGroup12;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type server5XXErrorType record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup13 sequenceGroup13;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type server5XXError record {|
    @xmldata:Sequence {minOccurs: 1, maxOccurs: 1}
    SequenceGroup13 sequenceGroup13;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup6 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    string Accept?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string Accept_Charset?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 3}
    string Accept_Encoding?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 4}
    string Content_Type?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 5}
    string Content_Length?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 6}
    string Connection?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 7}
    string Cookie?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 8}
    string Pragma?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 9}
    string Authorization?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 10}
    dynamicHeadersType DynamicHeaders?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup7 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    string Content_Length?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string Connection?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 3}
    string Pragma?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 4}
    string StatusLine?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 5}
    string Location?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 6}
    string Set_Cookie?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 7}
    string Content_Type?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 8}
    dynamicHeadersType DynamicHeaders?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup8 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    string Content_Length?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string Connection?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 3}
    string Pragma?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 4}
    string StatusLine?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 5}
    string Location?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 6}
    string Set_Cookie?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 7}
    string Content_Type?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 8}
    dynamicHeadersType DynamicHeaders?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup9 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    string Name;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string Value;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup10 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    dynamicHeadersTypeDetails[] Header?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup11 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    int statusCode;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup12 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    int statusCode;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string message?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup13 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    int statusCode;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string message?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type tmessageBody1 string;

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup14 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    string Accept?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string Accept_Charset?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 3}
    string Accept_Encoding?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 4}
    string Content_Type?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 5}
    string Content_Length?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 6}
    string Connection?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 7}
    string Cookie?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 8}
    string Pragma?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 9}
    string Authorization?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 10}
    dynamicHeadersType DynamicHeaders?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup15 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    string Content_Length?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string Connection?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 3}
    string Pragma?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 4}
    string StatusLine?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 5}
    string Location?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 6}
    string Set_Cookie?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 7}
    string Content_Type?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 8}
    dynamicHeadersType DynamicHeaders?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup16 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    string Content_Length?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string Connection?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 3}
    string Pragma?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 4}
    string StatusLine?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 5}
    string Location?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 6}
    string Set_Cookie?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 7}
    string Content_Type?;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 8}
    dynamicHeadersType DynamicHeaders?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup17 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    string Name;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string Value;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup18 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    dynamicHeadersTypeDetails[] Header?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup19 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    int statusCode;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup20 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    int statusCode;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string message?;
|};

@xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
public type SequenceGroup21 record {|
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 1}
    int statusCode;
    @xmldata:Namespace {uri: "http://tns.tibco.com/bw/REST"}
    @xmldata:SequenceOrder {value: 2}
    string message?;
|};
