AIS: SoapUI Project
======

#### Introduction

SoapUI is an open source web service testing application for service-oriented architectures (SOA). Its functionality covers web service inspection, invoking, development, simulation and mocking, functional testing, load and compliance testing. SoapUI has been given a number of awards.

A SoapUI Project has been created that contains example requests to invoke:
* Timestamp Request (RFC3161) Request
* Organization Signature (RFC5652) Request
* OnDemand Signature (RFC5652) Request


#### How To

* Download & Install SoapUI (Windows/Mac/Linux) from http://sourceforge.net/projects/soapui
* Checkout the complete GitHub Repository (including the `services` folder which contains the `*.wadl`/`*.wsdl` files)
* Import the `ais-soapui-project.xml` file: "File" > "Import Project"
* Increase the Socket Timeout to 300'000 ms: "File" > "Preferences" > "HTTP Settings" > "Socket Timeout (ms)"
* Configure the SSL KeyStore: "File" > "Preferences" > "SSL Settings". Note: You need your personal SSL certificate to establish a connection to AIS. This certificate must be signed by the Swisscom CA. Please get in contact with Swisscom to order a certificate.
* [optional] Configure the Proxy: "File" > "Preferences" > "Proxy Settings"
* Configure the Test Suite Properties: Select "Regression Test Suite" > "Custom Properties". Note: Each customer will need their own AP_ID. This identification is provided by Swisscom.
* Finally, just double click the "Regression Test Suite" and execute it


#### Regression Test Suite

This SoapUI Project contains a Test Suite for Regression Tests against the AIS Service.
It supports SOAP as well as RESTful (XML/JSON) interface.

###### Regression Test Suite Properties:

Double click "Regression Test Suite" to adapt the test properties.

| Property | Description |
| :------------- | :------------- |
${#TestSuite#BASEURL}|Base URL for the Endpoint. Use https://ais.swisscom.com
${#TestSuite#AP_ID}|Your ClaimedIdentity Customer ID (AP_ID)
${#TestSuite#STATIC_ID}|Your ClaimedIdentity Key ID for Static Keys
${#TestSuite#ONDEMAND_QUALIFIED}|Your ClaimedIdentity Key ID for OnDemand Keys and enforced MID Auth
${#TestSuite#ONDEMAND_ADVANCED}|Your ClaimedIdentity Key ID for OnDemand Keys and optional MID Auth
${#TestSuite#MSISDN}|The Mobile Subscriber Number
${#TestSuite#USER_ALERT}|Turn on ('true') or off ('false') user alert messages before any Mobile ID authentication
${#TestSuite#SHA256}|URI For SHA-256 Algorithm
${#TestSuite#SHA384}|URI For SHA-384 Algorithm
${#TestSuite#SHA512}|URI For SHA-512 Algorithm
${#TestSuite#DIGEST_256}|Base64 encoded binary hash (SHA-256) value of any document
${#TestSuite#DIGEST_384}|Base64 encoded binary hash (SHA-384) value of any document
${#TestSuite#DIGEST_512}|Base64 encoded binary hash (SHA-512) value of any document
${#TestSuite#_tmp}|This property is used for temporary session data only

#### Benchmark

This SoapUI Project contains a Test Suite for load and performance testing against the AIS Service.
It supports the RESTful (XML) interface.

* File>Preferences>UI Settings: Enable "Do not disable the groovy log when running load tests"
* Test Steps: Each test step contains a Script Assertion to log all Error Responses to soapui.log
* Load Tests: Each load test contains a "Setup Script" as well as a "TearDown Script" to log the load test statistics.

Log Examples:
```
$ tail -0f soapui.log | grep "\[log\]"
2014-03-13 18:28:20,945 INFO  [log] '01 Timestamp' >> '01 Timestamp 1T100' : Sleep 1000ms
2014-03-13 18:28:21,945 INFO  [log] '01 Timestamp' >> '01 Timestamp 1T100' : STARTED
2014-03-13 18:28:30,125 ERROR [log] '01 Timestamp' >> '01 Timestamp 1T100' : 200 ErrorResponse=<?xml version="1.0" encoding="UTF-8" standalone="yes"?><SignResponse xmlns="urn:oasis:names:tc:dss:1.0:core:schema" xmlns:dsig="http://www.w3.org/2000/09/xmldsig#" xmlns:async="urn:oasis:names:tc:dss:1.0:profiles:asynchronousprocessing:1.0" xmlns:sc="http://ais.swisscom.ch/1.0/schema" RequestID="2014-03-13T16:43:17.833+0100" Profile="http://ais.swisscom.ch/1.0"><Result><ResultMajor>urn:oasis:names:tc:dss:1.0:resultmajor:ResponderError</ResultMajor><ResultMinor>http://ais.swisscom.ch/1.0/resultminor/CantServeTimely</ResultMinor><ResultMessage xml:lang="en"/></Result></SignResponse>
2014-03-13 18:28:34,880 INFO  [log] '01 Timestamp' >> '01 Timestamp 1T100' : FINISHED | EndTime 18:28:34 | TimeTaken (ms) 12855 | Errors 1

$ grep "13 Static 100T10" soapui.log | grep CantServeTimely | wc -l
15
```

###### Benchmark Test Suite Properties:

Double click "Benchmark" to adapt the test properties.

| Property | Description |
| :------------- | :------------- |
${#TestSuite#BASEURL}|Base URL for the Endpoint. Use https://ais.swisscom.com
${#TestSuite#AP_ID}|Your ClaimedIdentity Customer ID (AP_ID)
${#TestSuite#STATIC_ID}|Your ClaimedIdentity Key ID for Static Keys
${#TestSuite#ONDEMAND_ADVANCED}|Your ClaimedIdentity Key ID for OnDemand Keys and optional MID Auth
${#TestSuite#SHA256}|URI For SHA-256 Algorithm
${#TestSuite#DIGEST_256}|Base64 encoded binary hash (SHA-256) value of any document
${#TestSuite#Delay}|Delay in ms before each load test case
${#TestSuite#tmp_TestRunID}|This property is used for temporary session data only

#### Known Issues

Older versions of the Groovy library may throw a _StackOverflowError_-Exception when running the Test Suite. Follow the steps below to replace the old library with the latest one:
* Download latest Groovy Library v2.x from http://groovy.codehaus.org/Download
* Put the ```groovy-all-2.x.x.jar``` file into your SoapUI installation /lib directory
* Remove the original/old Groovy library version from your SoapUI installation /lib directory
