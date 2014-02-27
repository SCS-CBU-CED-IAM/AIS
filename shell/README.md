AIS: Shell Scripts
============

Shell command line scripts to invoke:

* Timestamp Request (RFC3161): ais-timestamp.sh
* Signing Request (RFC5652): ais-sign.sh
* Detached signature verification: ais-verify.sh

### Usage

```
Usage: ./ais-timestamp.sh <options> file method pkcs7
  -t value  - message type (SOAP, XML, JSON), default SOAP
  -v        - verbose output
  -d        - debug mode
  file      - file to be signed
  method    - digest method (SHA256, SHA384, SHA512)
  pkcs7     - output file with detached PKCS#7 (Cryptographic Message Syntax) signature

  Examples ./ais-timestamp.sh -v myfile.txt SHA256 myfile.p7s
           ./ais-timestamp.sh -v -t JSON -v myfile.txt SHA256 myfile.p7s
```

```
Usage: ./ais-sign.sh <options> file method pkcs7 [dn] [[msisdn]] [[msg]] [[lang]]
  -t value   - message type (SOAP, XML, JSON), default SOAP
  -v         - verbose output
  -d         - debug mode
  file       - file to be signed
  method     - digest method (SHA256, SHA384, SHA512)
  pkcs7      - output file with detached PKCS#7 (Cryptographic Message Syntax) signature
  [dn]       - optional distinguished name for on-demand certificate signing
  [[msisdn]] - optional Mobile ID authentication when [dn] is present
  [[msg]]    - optional Mobile ID message when [dn] is present
  [[lang]]   - optional Mobile ID language (en, de, fr, it) when [dn] is present

  Example ./ais-sign.sh -v myfile.txt SHA256 myfile.p7s
          ./ais-sign.sh -v myfile.txt SHA256 myfile.p7s 'cn=Hans Muster,o=ACME,c=CH'
          ./ais-sign.sh -v -t JSON myfile.txt SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH'
          ./ais-sign.sh -v myfile.txt SHA256 myfile.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350
          ./ais-sign.sh -v myfile.txt SHA256 myfile.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'myserver.com: Sign?' en
```

```
Usage: ./ais-verify.sh <options> pkcs7
  -v         - verbose output
  -d         - debug mode
  file       - file to verify
  pkcs7      - file containing the detached PKCS#7 signature

  Example ./ais-verify.sh -v myfile.txt myfile.p7s
```

### Configuration

The files `mycert.crt`and `mycert.key` are PLACEHOLDERS without any valid content. Be sure to adjust them with your client certificate content in order to connect to the AIS service.

Each script contains a configuration section on the top where at least following variables are relevant: CUSTOMER, KEY_STATIC, KEY_ONDEMAND.
Those values will be provided by Swisscom to each customer in order to use the AIS service.

### Results

Example of verbose outputs for signature requests:
```
OK on myfile.txt with following details:
 Digest       : KZT9M2tBnx5Snfp/y60TOxC/G3SiuSKmTp/L4QLUYqw=
 Result major : urn:oasis:names:tc:dss:1.0:resultmajor:Success with exit 0
```

```
FAILED on myfile.txt with following details:
 Result major   : urn:com:swisscom:dss:1.0:resultmajor:SubsystemError with exit 1
 Result minor   : urn:com:swisscom:dss:1.0:resultminor:subsystem:MobileID:service
 Result message : mss:_105
```

Example of verbose outputs for signature verifications:
```
OK on myfile.p7s with following details:
 Signer       : subject= CN=Hans Muster,O=ACME,C=CH
                issuer= C=ch,O=Swisscom,OU=Digital Certificate Services,CN=Swisscom TEST Saphir CA 2
                validity= notBefore=Jan 22 08:48:10 2014 GMT notAfter=Jan 22 08:58:10 2014 GMT
                OCSP check= good
 Embedded OCSP: Yes
 Embedded TSA : Yes
```

```
FAILED on myfile.p7s with following details:
>> CMS verification details <<
Verification failure
2710938092:error:2E09A09E:CMS routines:CMS_SignerInfo_verify_content:verification failure:cms_sd.c:887:
2710938092:error:2E09D06D:CMS routines:CMS_verify:content verify error:cms_smime.c:425:
>> TSA verification details <<
2710938092:error:2F094086:time stamp routines:PKCS7_to_TS_TST_INFO:detached content:ts_asn1.c:296:
Verification: FAILED
```


### Known issues

**OS X 10.x: Requests always fail with MSS error 104: _Wrong SSL credentials_.**

The `curl` shipped with OS X uses their own Secure Transport engine, which broke the --cert option, see: http://curl.haxx.se/mail/archive-2013-10/0036.html

Install curl from Mac Ports `sudo port install curl` or home-brew: `brew install curl && brew link --force curl`.
