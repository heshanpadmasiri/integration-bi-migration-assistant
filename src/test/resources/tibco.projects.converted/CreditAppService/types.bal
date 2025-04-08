type ExperianResponseSchemaElement anydata;

type HTTPRequestConfig record {
    string Method;
    string RequestURI;
    json PostData = "";
    map<string> Headers = {};
    map<string> parameters = {};
};

type SuccessSchema anydata;

type GiveNewSchemaNameHere anydata;

type CreditScoreSuccessSchema anydata;

@xmldata:Namespace {uri: "http://xmlns.example.com/y54cuadtcxtfstqs3rux2gfdaxppoqgc/parameters"}
public type SequenceGroup17 record {|
@xmldata:Namespace {uri: "http://xmlns.example.com/y54cuadtcxtfstqs3rux2gfdaxppoqgc/parameters"}
@xmldata:SequenceOrder {value: 1}
boolean skipvalidation?;
|};
