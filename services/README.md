AIS: Service Description
============

WSDL Code Generation with Apache CXF:

```
<apache-cxf>/bin/wsdl2java -V -d wsdl/src_gen -catalog catalog.xml -client wsdl/aisService.wsdl
```

WADL Code Generation with Apache CXF:

```
<apache-cxf>/bin/wadl2java -V -d wadl/src_gen -catalog catalog.xml -interface -impl -p com.swisscom.ais.service -resource AisService wadl/aisService.wadl
```
NOTE:

Add the following commandline arguments to the wadl2java/wsdl2java Batch (Windows) file / Shell (Linux) script (located <apache-cxf>/bin) within the call to the java main class WADLToJava / WSDLtoJava:

```
-Djavax.xml.accessExternalSchema=all -Djavax.xml.accessExternalDTD=all
```
