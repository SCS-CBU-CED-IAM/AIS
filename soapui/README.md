AIS: SoapUI Project
======

#### Introduction

SoapUI is an open source web service testing application for service-oriented architectures (SOA). Its functionality covers web service inspection, invoking, development, simulation and mocking, functional testing, load and compliance testing. SoapUI has been given a number of awards.

A SoapUI Project has been created that contains example requests to invoke:
* Timestamp Request (RFC3161) Request
* Organization Signature (RFC5652) Request
* OnDemand Signature (RFC5652) Request

#### Configuration

* AP_ID: Each customer will need their own AP_ID. This identification is provided by Swisscom.
* SSL KeyStore: You need your personal SSL certificate to establish a connection to AIS. This certificate must be signed by the Swisscom CA. Please get in contact with Swisscom to order a certificate.

#### Instructions

* Download & Install SoapUI (Windows/Mac/Linux) from http://sourceforge.net/projects/soapui
* Checkout the complete GitHub Repository (including the `services` folder which contains the `*.wadl`/`*.wsdl` files)
* Import the `ais-soapui-project.xml` file (File > Import Project)
* Configure the SSL KeyStore (File > Preferences > SSL Settings)
* [optional] Configure the Proxy (File > Preferences > Proxy Settings)
* Configure the Test Suite Properties (Select "Regression Test Suite" > "Custom Properties")
* Double click the "Regression Test Suite" and run it


#### Regression Test Suite

This SoapUI Project contains a Test Suite for Regression Tests against the AIS Service.
It supports SOAP as well as RESTful (XML/JSON) interface.

###### Test Suite Properties:

| Property | Description |
| :------------- | :------------- |
${#TestSuite#BASEURL}|Base URL for the Endpoint. Use https://ais.swisscom.com
${#TestSuite#AP_ID}|Your ClaimedIdentity Customer ID (AP_ID)
${#TestSuite#STATIC_ID}|Your ClaimedIdentity Key ID for Static Keys
${#TestSuite#ONDEMAND_QUALIFIED}|Your ClaimedIdentity Key ID for OnDemand Keys and enforced MID Auth
${#TestSuite#ONDEMAND_ADVANCED}|Your ClaimedIdentity Key ID for OnDemand Keys and optional MID Auth
${#TestSuite#MSISDN}|The Mobile Subscriber Number
${#TestSuite#SHA256}|URI For SHA-256 Algorithm
${#TestSuite#SHA384}|URI For SHA-384 Algorithm
${#TestSuite#SHA512}|URI For SHA-512 Algorithm
${#TestSuite#DIGEST_256}|Base64 encoded binary hash (SHA-256) value of any document
${#TestSuite#DIGEST_384}|Base64 encoded binary hash (SHA-384) value of any document
${#TestSuite#DIGEST_512}|Base64 encoded binary hash (SHA-512) value of any document
${#TestSuite#_tmp}|This property is used for temporary session data only

#### Known Issues

Older versions of the Groovy library may throw a _StackOverflowError_-Exception when running the Test Suite. Follow the steps below to replace the old library with the latest one:
* Download latest Groovy Library v2.x from http://groovy.codehaus.org/Download
* Put the ```groovy-all-2.x.x.jar``` file into your SoapUI installation /lib directory
* Remove the original/old Groovy library version from your SoapUI installation /lib directory
