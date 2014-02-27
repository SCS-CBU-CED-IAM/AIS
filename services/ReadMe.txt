
WSDL Code Generation with Apache CXF:

<apache-cxf>/bin/wsdl2java -d wsdl/src_gen -catalog catalog.xml -client wsdl/aisService.wsdl

WADL Code Generation with Apache CXF:

<apache-cxf>/bin/wadl2java -d wadl/src_gen -catalog catalog.xml -interface -impl -p com.swisscom.ais.service -resource AisService wadl/aisService.wadl

