AIS: iText
============

Java source code and command line tool to sign PDF with iText.

### Usage

````
Usage: com.swisscom.ais.itext.SignPDF [OPTIONS]

OPTIONS

  [mandatory]
  -type=VALUE       - signature type
                       supported values:
                       - timestamp (add timestamp only)
                       - sign      (add cms signature incl. timestamp)
  -infile=VALUE     - input filename of the pdf to be signed
  -outfile=VALUE    - output filename for the signed pdf

  [optional]
  -v                - verbose output
  -vv               - more verbose output
  -config=VALUE     - custom path to properties file, overwrites default path
  -reason=VALUE     - signing reason
  -location=VALUE   - signing location
  -contact=VALUE    - signing contact
  -certlevel=VALUE  - certify the pdf, at most one certification per pdf is allowed
                       supported values:
                       - 1 (no changes allowed)
                       - 2 (form filling and further signing allowed)
                       - 3 (form filling, annotations and further signing allowed)
  -dn=VALUE         - distinguished name, for personal on demand certificate signing
                       supported attributes, separated by commas:
                       [mandatory]
                       - cn / commonname 
                       - c / countryname
                       [optional]
                       - emailaddress
                       - givenname
                       - l / localityname
                       - ou / organizationalunitname
                       - o / organizationname
                       - serialnumber
                       - st / stateorprovincename
                       - sn / surname
  -msisdn=VALUE     - mobileid step up phone number            (requires -dn -msg -lang)
  -msg=VALUE        - mobileid step up message to be displayed (requires -dn -msisdn -lang)
                      A placeholder #TRANSID# may be used anywhere in the message to include a unique transaction id.
  -lang=VALUE       - mobileid step up language                (requires -dn -msisdn -msg)
                       supported values:
                       - en (english)
                       - de (deutsch)
                       - fr (français)
                       - it (italiano)

EXAMPLES

  [timestamp]
    java com.swisscom.ais.itext.SignPDF -type=timestamp -infile=sample.pdf -outfile=signed.pdf
    java com.swisscom.ais.itext.SignPDF -v -type=timestamp -infile=sample.pdf -outfile=signed.pdf

  [sign with static certificate]
    java com.swisscom.ais.itext.SignPDF -type=sign -infile=sample.pdf -outfile=signed.pdf
    java com.swisscom.ais.itext.SignPDF -v -config=/tmp/signpdf.properties -type=sign -infile=sample.pdf -outfile=signed.pdf -reason=Approved -location=Berne -contact=alice@acme.com

  [sign with on demand certificate]
    java com.swisscom.ais.itext.SignPDF -type=sign -infile=sample.pdf -outfile=signed.pdf -dn='cn=Alice Smith,o=ACME,c=CH'
    java com.swisscom.ais.itext.SignPDF -v -type=sign -infile=sample.pdf -outfile=signed.pdf -dn='cn=Alice Smith,o=ACME,c=CH' -certlevel=1

  [sign with OnDemand certificate and Mobile ID step up]
    java com.swisscom.ais.itext.SignPDF -v -type=sign -infile=sample.pdf -outfile=signed.pdf -dn='cn=Alice Smith,o=ACME,c=CH' -msisdn=41792080350 -msg='acme.com: Sign the PDF? (#TRANSID#)' -lang=en
```

#### Dependencies

This java application has external dependencies (libraries). They are located in the `./lib` subfolder.
The latest version may be downloaded from the following source:

1: http://mvnrepository.com/artifact/com.google.code.findbugs/jsr305

Version 2.0.2 has been successfully tested

2: http://sourceforge.net/projects/itext

Version 5.4.5 has been successfully tested

3: http://www.bouncycastle.org/latest_releases.html

bcprov-jdk15on-150.jar has been successfully tested
bcpkix-jdk15on-150.jar has been successfully tested

#### Paths & Placeholders

The following placeholder will be used in this README (see sections below)
```
<JAR>   = Path to the ./jar subfolder containing the latest Java Archive, e.g. ./AIS/itext/jar
<SRC>   = Path to the ./src subfolder containing the *.java source files, e.g. ./AIS/itext/src/swisscom/com/ais/itext
<LIB>   = Path to the ./lib subfolder containing the libraries, e.g. ./AIS/itext/lib
<CLASS> = Path to the directory where class files will be created, e.g. ./AIS/itext/class
<CFG>   = Path to the signpdf.properties file, e.g. ./AIS/itext/signpdf.properties
<DOC>   = Path to the ./doc subfolder containing the JavaDoc, e.g. ./AIS/itext/doc
```

#### Configuration

Refer to `signpdf.properties` configuration file and modify the configuration properties accordingly.

#### Run the JAR archive

You may use the latest Java Archive (JAR) `signpdf-x.y.z.jar` located in the `./jar` subfolder.

Run the JAR (Unix/OSX): `java -cp "<JAR>/signpdf-x.y.z.jar:<LIB>/*" com.swisscom.ais.itext.SignPDF`

Run the JAR (Unix/OSX) with custom path to the properties file:
`java -DpropertyFile.path=<CFG> -cp "<JAR>/signpdf-x.y.z.jar:<LIB>/*" com.swisscom.ais.itext.SignPDF`

Run the JAR (Unix/OSX) with DEBUG enabled:
`java -Djavax.net.debug=all -Djava.security.debug=certpath -cp "<JAR>/signpdf.jar:<LIB>/*" com.swisscom.ais.itext.SignPDF`

Create the latest JAR: `jar cfe <JAR>/signpdf-x.y.z.jar com.swisscom.ais.itext.SignPDF -C <CLASS> .`

If you're on Windows then use a semicolon ; instead of the colon : 

#### Compile & Run the Java Classes

The source files can be compiled as follows. 

Compile the sources: `javac -d <CLASS> -cp "<LIB>/*" <SRC>/*.java`

Note: The class files are generated in a directory hierarchy which reflects the given package structure: `<CLASS>/swisscom/com/ais/itext/*.class`

The compiled application can be run as follows.

Run the application (Unix/OSX):
`java -cp "<CLASS>:<LIB>/*" com.swisscom.ais.itext.SignPDF`

Run the application (Unix/OSX) with custom path to the properties file:
`java -DpropertyFile.path=<CFG> -cp "<CLASS>:<LIB>/*" com.swisscom.ais.itext.SignPDF`

Run the application (Unix/OSX) with DEBUG enabled:
`java -Djavax.net.debug=all -Djava.security.debug=certpath -cp "<CLASS>:<LIB>/*" com.swisscom.ais.itext.SignPDF`

If you're on Windows then use a semicolon ; instead of the colon : 

#### JavaDoc

The latest JavaDoc is located in the `./doc` subfolder.

Create the latest JavaDoc: `javadoc -windowtitle "Swisscom All-in Signing Service vx.y.z" -doctitle "<h1>Swisscom All-in Signing Service vx.y.z</h1>" -footer "Swisscom All-in Signing Service vx.y.z" -d <DOC> -private -sourcepath <SRC> com.swisscom.ais.itext`
