AIS: iText
============

Java source code and command line tool to sign PDF with iText.

### Usage

````
Usage: com.swisscom.ais.itext.SignPDF [OPTIONS]

Options:
  -v                - set verbose output
  -d                - set debug mode
  -config=VALUE     - custom path to properties file which will overwrite default path
  -type=VALUE       - signature type, values: timestamp, sign
  -infile=VALUE     - source PDF file to be signed
  -outfile=VALUE    - target PDF file that will be signed
  -reason=VALUE     - signing reason
  -location=VALUE   - signing location
  -contact=VALUE    - signing contact
  -dn=VALUE         - distinguished name for OnDemand certificate signing
  -msisdn=VALUE     - Mobile ID step up MSISDN (requires -dn -msg -lang)
  -msg=VALUE        - Mobile ID step up message (requires -dn -msisdn -lang)
  -lang=VALUE       - Mobile ID step up language, values: en, de, fr, it (requires -dn -msisdn -msg)

Examples:
  java com.swisscom.ais.itext.SignPDF -v -type=timestamp -infile=sample.pdf -outfile=signed.pdf
  java com.swisscom.ais.itext.SignPDF -v -config=/tmp/signpdf.properties -type=sign -infile=sample.pdf -outfile=signed.pdf -reason=Approved -location=CH -contact=alice@example.com
  java com.swisscom.ais.itext.SignPDF -v -type=sign -infile=sample.pdf -outfile=signed.pdf -dn='cn=Hans Muster,o=ACME,c=CH'
  java com.swisscom.ais.itext.SignPDF -v -type=sign -infile=sample.pdf -outfile=signed.pdf -dn='cn=Hans Muster,o=ACME,c=CH' -msisdn=41792080350 -msg='service.com: Sign?' -lang=en
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
