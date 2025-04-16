import ballerina/data.xmldata;

type QueryData1 record {
    int noOfPulls;
    string ssn;
};

type QueryData0 record {
    string ssn;
};

@xmldata:Name {
    value: "Record"
}
type QueryResult0 record {
    string firstname?;
    string lastname?;
    string ssn?;
    string dateofBirth?;
    int ficoscore?;
    string rating?;
    int numofpulls?;
};
 