/**
 * Creates SOAP requests with hash from a pdf document and send it to a server. If server sends a response with a signature
 * this will be add to a pdf.
 *
 * Created:
 * 03.12.13 KW49 14:51
 * </p>
 * Last Modification:
 * 17.02.2014 15:13
 * <p/>
 * Version:
 * 1.0.0
 * </p>
 * Copyright:
 * Copyright (C) 2013. All rights reserved.
 * </p>
 * License:
 * GNU General Public License version 2 or later; see LICENSE.md
 * </p>
 * Author:
 * Swisscom (Schweiz) AG
 */

package com.swisscom.ais.itext;

import com.itextpdf.text.pdf.codec.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class Soap {

    /**
     * Constant for timestamp urn
     */
    private static final String _TIMESTAMP_URN = "urn:ietf:rfc:3161";

    /**
     * Constant for mobile id type
     */
    private static final String _MOBILE_ID_TYPE = "http://ais.swisscom.ch/1.0/auth/mobileid/1.0";

    /**
     * Path to configuration file. Can also set in constructor
     */
    private String _cfgPath = "signpdf.properties";

    /**
     * Properties from properties file
     */
    private Properties properties;

    /**
     * File path of private key
     */
    private String _privateKeyName;

    /**
     * File path of server certificate
     */
    private String _serverCertPath;

    /**
     * File patj of client certificate
     */
    private String _clientCertPath;

    /**
     * Url of dss server
     */
    private String _url;

    /**
     * Connection timeout in seconds
     */
    private int _timeout;

    /**
     * If set to true debug information will be print otherwise not
     */
    public static boolean _debugMode = false;

    /**
     * If set to true verbose information will be print otherwise not
     */
    public static boolean _verboseMode = false;

    /**
     * Constructor. Set parameter and load properties from file. Connection properties will be set and check if all needed
     * files exist
     *
     * @param verboseOutput    If true verbose information will be print out
     * @param debugMode        If true debug information will be print out
     * @param propertyFilePath Path of property file
     * @throws FileNotFoundException If a file do not exist. E.g. property file, certificate, input pdf etc
     */
    public Soap(boolean verboseOutput, boolean debugMode, @Nullable String propertyFilePath) throws FileNotFoundException {

        Soap._verboseMode = verboseOutput;
        Soap._debugMode = debugMode;

        if (propertyFilePath != null) {
            _cfgPath = propertyFilePath;
        }

        properties = new Properties();

        try {
            properties.load(new FileReader(_cfgPath));
        } catch (IOException e) {
            throw new FileNotFoundException(("Could not load property file"));
        }

        setConnectionProperties();
        checkFilesExistsAndIsFile(new String[]{this._clientCertPath, this._privateKeyName, this._serverCertPath});

    }

    /**
     * Set connection properties from property file. Also convert timeout from seconds to milliseconds. If timeout can not
     * be readed from properties file it will use standard value 90 seconds
     */
    private void setConnectionProperties() {

        this._clientCertPath = properties.getProperty("CERT_FILE");
        this._privateKeyName = properties.getProperty("CERT_KEY");
        this._serverCertPath = properties.getProperty("SSL_CA");
        this._url = properties.getProperty("URL");
        try {
            this._timeout = Integer.parseInt(properties.getProperty("TIMEOUT_CON"));
            this._timeout *= 1000;
        } catch (NumberFormatException e) {
            this._timeout = 90 * 1000;
        }

    }

    /**
     * Read signing options from properties. Depending on parameters here will be decided which type of signature will be used.
     *
     * @param signatureType     Type of signature e.g. timestamp, ondemand or static
     * @param fileIn            File path of input pdf document
     * @param fileOut           File path of output pdf document which will be the signed one
     * @param signingReason     Reason for signing a document
     * @param signingLocation   Location where a document was signed
     * @param signingContact    Person who signed document
     * @param distinguishedName Information about signer e.g. name, country etc.
     * @param msisdn            Mobile id for sending message to signer
     * @param msg               Message which will be send to signer if msisdn is set
     * @param language          Language of message
     * @throws Exception If parameters are not set or signing failed
     */
    public void sign(@Nonnull Include.Signature signatureType, @Nonnull String fileIn, @Nonnull String fileOut,
                     @Nullable String signingReason, @Nullable String signingLocation, @Nullable String signingContact,
                     @Nullable String distinguishedName, @Nullable String msisdn, @Nullable String msg, @Nullable String language)
            throws Exception {

        Include.HashAlgorithm hashAlgo = Include.HashAlgorithm.valueOf(properties.getProperty("DIGEST_METHOD").trim().toUpperCase());

        String claimedIdentity = properties.getProperty("CUSTOMER");
        String claimedIdentityPropName = signatureType.equals(Include.Signature.ONDEMAND) ?
                "KEY_ONDEMAND" : signatureType.equals(Include.Signature.STATIC) ? "KEY_STATIC" : null;
        if (claimedIdentityPropName != null) {
            claimedIdentity = claimedIdentity.concat(":" + properties.getProperty(claimedIdentityPropName));
        }

        PDF pdf = new PDF(fileIn, fileOut, null, signingReason, signingLocation, signingContact);

        try {
            String requestId = getRequestId();

            if (msisdn != null && msg != null && language != null && signatureType.equals(Include.Signature.ONDEMAND)) {
                if (_debugMode) {
                    System.out.println("Going to sign ondemand with mobile id");
                }
                Calendar signingTime = Calendar.getInstance();
                // Add 3 Minutes to move signing time within the OnDemand Certificate Validity
                // This is only relevant in case the signature does not include a timestamp
                signingTime.add(Calendar.MINUTE, 3); 
                signDocumentOnDemandCertMobileId(new PDF[]{pdf}, signingTime, hashAlgo, _url, claimedIdentity, distinguishedName, msisdn, msg, language, requestId);
            } else if (signatureType.equals(Include.Signature.ONDEMAND)) {
                if (_debugMode) {
                    System.out.println("Going to sign with ondemand");
                }
                Calendar signingTime = Calendar.getInstance();
                // Add 3 Minutes to move signing time within the OnDemand Certificate Validity
                // This is only relevant in case the signature does not include a timestamp
                signingTime.add(Calendar.MINUTE, 3);
                signDocumentOnDemandCert(new PDF[]{pdf}, hashAlgo, signingTime, _url, true,
                        distinguishedName, claimedIdentity, requestId);
            } else if (signatureType.equals(Include.Signature.TIMESTAMP)) {
                if (_debugMode) {
                    System.out.println("Going to sign only with timestamp");
                }
                signDocumentTimestampOnly(new PDF[]{pdf}, hashAlgo, Calendar.getInstance(), _url, claimedIdentity,
                        requestId);
            } else if (signatureType.equals(Include.Signature.STATIC)) {
                if (_debugMode) {
                    System.out.println("Going to sign with static cert");
                }
                signDocumentStaticCert(new PDF[]{pdf}, hashAlgo, Calendar.getInstance(), _url, claimedIdentity, requestId);
            } else {
                throw new Exception("Wrong or missing parameters. Can not find a signature type.");
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    /**
     * Create SOAP request message and sign document with on demand certificate and authenticate with mobile id
     *
     * @param pdfs              Pdf input files
     * @param signDate          Date when document(s) will be signed
     * @param hashAlgo          Hash algorithm to use for signature
     * @param serverURI         Server uri where to send the request
     * @param claimedIdentity   Signers identity
     * @param distinguishedName Information about signer e.g. name, country etc.
     * @param phoneNumber       Number of phone when mobile id is used
     * @param certReqMsg        Message which the signer get on his phone
     * @param certReqMsgLang    Language of message
     * @param requestId         An id for the request
     * @throws Exception If hash or request can not be generated or document can not be signed.
     */
    private void signDocumentOnDemandCertMobileId(@Nonnull PDF pdfs[], @Nonnull Calendar signDate, @Nonnull Include.HashAlgorithm hashAlgo,
                                                  @Nonnull String serverURI, @Nonnull String claimedIdentity,
                                                  @Nonnull String distinguishedName, @Nonnull String phoneNumber, @Nonnull String certReqMsg,
                                                  @Nonnull String certReqMsgLang, String requestId) throws Exception {
        String[] additionalProfiles;

        if (pdfs.length > 1) {
            additionalProfiles = new String[2];
            additionalProfiles[1] = Include.AdditionalProfiles.BATCH.getProfileName();
        } else {
            additionalProfiles = new String[1];
        }
        additionalProfiles[0] = Include.AdditionalProfiles.ON_DEMAND_CERTIFCATE.getProfileName();

        int estimatedSize = getEstimatedSize(false);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), false);
        }

        SOAPMessage sigReqMsg = createRequestMessage(Include.RequestType.SignRequest, hashAlgo.getHashUri(), true,
                pdfHash, additionalProfiles,
                claimedIdentity, Include.SignatureType.CMS.getSignatureType(), distinguishedName, _MOBILE_ID_TYPE, phoneNumber,
                certReqMsg, certReqMsgLang, null, requestId);

        signDocumentSync(sigReqMsg, serverURI, pdfs, estimatedSize, "Base64Signature");

    }

    /**
     * create SOAP request message and sign document with ondemand certificate but without mobile id
     *
     * @param pdfs               Pdf input files
     * @param hashAlgo           Hash algorithm to use for signature
     * @param signDate           Date when document(s) will be signed
     * @param serverURI          Server uri where to send the request
     * @param mobileIDStepUp     certificate request profile
     * @param distinguishedName  Information about signer e.g. name, country etc.
     * @param claimedIdentity    Signers identity
     * @param requestId          An id for the request
     * @throws Exception If hash or request can not be generated or document can not be signed.
     */
    private void signDocumentOnDemandCert(@Nonnull PDF[] pdfs, @Nonnull Include.HashAlgorithm hashAlgo, Calendar signDate, @Nonnull String serverURI,
                                          @Nonnull boolean mobileIDStepUp, @Nonnull String distinguishedName, @Nonnull String claimedIdentity, String requestId)
            throws Exception {

        String[] additionalProfiles;
        if (pdfs.length > 1) {
            additionalProfiles = new String[2];
            additionalProfiles[1] = Include.AdditionalProfiles.BATCH.getProfileName();
        } else {
            additionalProfiles = new String[1];
        }
        additionalProfiles[0] = Include.AdditionalProfiles.ON_DEMAND_CERTIFCATE.getProfileName();

        int estimatedSize = getEstimatedSize(false);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), false);
        }

        SOAPMessage sigReqMsg = createRequestMessage(Include.RequestType.SignRequest, hashAlgo.getHashUri(), true,
                pdfHash, additionalProfiles,
                claimedIdentity, Include.SignatureType.CMS.getSignatureType(), distinguishedName, null, null, null, null, null, requestId);

        signDocumentSync(sigReqMsg, serverURI, pdfs, estimatedSize, "Base64Signature");
    }

    /**
     * Create SOAP request message and sign document with static certificate
     *
     * @param pdfs              Pdf input files
     * @param hashAlgo          Hash algorithm to use for signature
     * @param signDate          Date when document(s) will be signed
     * @param serverURI         Server uri where to send the request
     * @param claimedIdentity   Signers identity
     * @param requestId         An id for the request
     * @throws Exception If hash or request can not be generated or document can not be signed.
     */
    private void signDocumentStaticCert(@Nonnull PDF[] pdfs, @Nonnull Include.HashAlgorithm hashAlgo, Calendar signDate, @Nonnull String serverURI,
                                        @Nonnull String claimedIdentity, String requestId)
            throws Exception {

        String[] additionalProfiles = null;
        if (pdfs.length > 1) {
            additionalProfiles = new String[1];
            additionalProfiles[0] = Include.AdditionalProfiles.BATCH.getProfileName();
        }

        int estimatedSize = getEstimatedSize(false);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), false);
        }

        SOAPMessage sigReqMsg = createRequestMessage(Include.RequestType.SignRequest, hashAlgo.getHashUri(), false,
                pdfHash, additionalProfiles,
                claimedIdentity, Include.SignatureType.CMS.getSignatureType(), null, null, null, null, null, null, requestId);

        signDocumentSync(sigReqMsg, serverURI, pdfs, estimatedSize, "Base64Signature");
    }

    /**
     * Create SOAP request message and add a timestamp to pdf
     *
     * @param pdfs              Pdf input files
     * @param hashAlgo          Hash algorithm to use for signature
     * @param signDate          Date when document(s) will be signed
     * @param serverURI         Server uri where to send the request
     * @param claimedIdentity   Signers identity
     * @param requestId         An id for the request
     * @throws Exception If hash or request can not be generated or document can not be signed.
     */
    private void signDocumentTimestampOnly(@Nonnull PDF[] pdfs, @Nonnull Include.HashAlgorithm hashAlgo, Calendar signDate,
                                           @Nonnull String serverURI, @Nonnull String claimedIdentity, String requestId)
            throws Exception {

        Include.SignatureType signatureType = Include.SignatureType.TIMESTAMP;

        String[] additionalProfiles;
        if (pdfs.length > 1) {
            additionalProfiles = new String[2];
            additionalProfiles[1] = Include.AdditionalProfiles.BATCH.getProfileName();
        } else {
            additionalProfiles = new String[1];
        }
        additionalProfiles[0] = Include.AdditionalProfiles.TIMESTAMP.getProfileName();

        int estimatedSize = getEstimatedSize(true);

        byte[][] pdfHash = new byte[pdfs.length][];
        for (int i = 0; i < pdfs.length; i++) {
            pdfHash[i] = pdfs[i].getPdfHash(signDate, estimatedSize, hashAlgo.getHashAlgorythm(), true);
        }

        SOAPMessage sigReqMsg = createRequestMessage(Include.RequestType.SignRequest, hashAlgo.getHashUri(), false,
                pdfHash, additionalProfiles, claimedIdentity, signatureType.getSignatureType(),
                null, null, null, null, null, null, requestId);

        signDocumentSync(sigReqMsg, serverURI, pdfs, estimatedSize, "RFC3161TimeStampToken");
    }

    /**
     * Send SOAP request to server and sign document if server send signature
     *
     * @param sigReqMsg     SOAP request message which will be send to the server
     * @param serverURI     Uri of server
     * @param pdfs          Pdf input file
     * @param estimatedSize Estimated size of external signature
     * @param signNodeName  Name of node where to find the signature
     * @throws Exception If hash can not be generated or document can not be signed.
     */
    private void signDocumentSync(@Nonnull SOAPMessage sigReqMsg, @Nonnull String serverURI, @Nonnull PDF[] pdfs,
                                  int estimatedSize, String signNodeName) throws Exception {

        String sigResponse = sendRequest(sigReqMsg, serverURI);
        ArrayList<String> responseResult = getTextFromXmlText(sigResponse, "ResultMajor");
        boolean singingSuccess = sigResponse != null && responseResult != null && Include.RequestResult.Success.getResultUrn().equals(responseResult.get(0));

        if (_debugMode || _verboseMode) {
            //Getting pdf input file names for message output
            String pdfNames = "";
            for (int i = 0; i < pdfs.length; i++) {
                pdfNames = pdfNames.concat(new File(pdfs[i].getInputFilePath()).getName());
                if (pdfs.length > i + 1)
                    pdfNames = pdfNames.concat(", ");
            }

            if (!singingSuccess) {
                System.err.print("FAILED to sign " + pdfNames);
            } else {
                System.out.print("OK signing " + pdfNames);
            }

            if (sigResponse != null) {

                ArrayList<String> resultMinor = null;
                ArrayList<String> errorMsg = null;

                if (_verboseMode) {
                    resultMinor = getTextFromXmlText(sigResponse, "ResultMinor");
                    errorMsg = getTextFromXmlText(sigResponse, "ResultMessage");

                    if (responseResult != null || resultMinor != null || errorMsg != null) {
                        if (!singingSuccess) {
                            System.err.println(" with following details:");
                        } else {
                            System.out.println(" with following details:");
                        }
                    }

                    if (responseResult != null) {
                        for (String s : responseResult) {
                            if (s.length() > 0) {
                                if (!singingSuccess) {
                                    System.err.println(" Result major: " + s);
                                } else {
                                    System.out.println(" Result major: " + s);
                                }
                            }
                        }
                    }

                    if (resultMinor != null) {
                        for (String s : resultMinor) {
                            if (s.length() > 0) {
                                if (!singingSuccess) {
                                    System.err.println(" Result minor: " + s);
                                } else {
                                    System.out.println(" Result minor: " + s);
                                }
                            }
                        }
                    }

                    if (errorMsg != null) {
                        for (String s : errorMsg) {
                            if (s.length() > 0) {
                                if (!singingSuccess) {
                                    System.err.println(" Result message: " + s);
                                } else {
                                    System.out.println(" Result message: " + s);
                                }
                            }
                        }
                    }
                }
            }
            //we need a line break
            if (!singingSuccess) {
                System.err.println("");
            } else {
                System.out.println("");
            }
        }

        if (!singingSuccess) {
            throw new Exception();
        }
        
        // Retrieve the Revocation Information (OCSP/CRL validation information)
        ArrayList<String> crl = getTextFromXmlText(sigResponse, "sc:CRL");
        ArrayList<String> ocsp = getTextFromXmlText(sigResponse, "sc:OCSP");

        ArrayList<String> signHashes = getTextFromXmlText(sigResponse, signNodeName);
        signDocuments(signHashes, ocsp, crl, pdfs, estimatedSize);
    }

    /**
     * Add signature to pdf
     *
     * @param signHashes    Arraylist with Base64 encoded signatures
     * @param ocsp          Arraylist with Base64 encoded ocsp responses
     * @param crl           Arraylist with Base64 encoded crl responses
     * @param pdfs          Pdf which will be signed
     * @param estimatedSize Estimated size of external signature
     * @throws Exception If adding signature to pdf failed.
     */
    private void signDocuments(@Nonnull ArrayList<String> signHashes, ArrayList<String> ocsp, ArrayList<String> crl, @Nonnull PDF[] pdfs, int estimatedSize) throws Exception {
        int counter = 0;

        for (String signatureHash : signHashes) {
            try {
                pdfs[counter].createSignedPdf(Base64.decode(signatureHash), estimatedSize);
            } catch (Exception e) {
                if (_debugMode) {
                    System.err.println("Could not add Signature to document");
                }
            }
            
            try {
                pdfs[counter].addValidationInformation(ocsp, crl); // Add revocation information to enable Long Term Validation (LTV) in Adobe Reader
            } catch (Exception e) {
                if (_debugMode) {
                    System.err.println("Could not add revocation information to document");
                }
            }

            counter++;
        }
    }

    /**
     * Get text from a node from a xml text
     *
     * @param soapResponseText Text where to search
     * @param nodeName         Name of the node which text should be returned
     * @return If nodes with searched node names exist it will return an array list containing text from nodes
     * @throws IOException                  If any IO errors occur
     * @throws SAXException                 If any parse errors occur
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created which satisfies the configuration requested
     */
    @Nullable
    private ArrayList<String> getTextFromXmlText(String soapResponseText, String nodeName) throws IOException, SAXException, ParserConfigurationException {
        Element element = getNodeList(soapResponseText);

        return getNodesFromNodeList(element, nodeName);
    }

    /**
     * Get nodes text content
     *
     * @param element
     * @param nodeName
     * @return if nodes with searched node names exist it will return an array list containing text from value from nodes
     */
    @Nullable
    private ArrayList<String> getNodesFromNodeList(@Nonnull Element element, @Nonnull String nodeName) {
        ArrayList<String> returnlist = null;
        NodeList nl = element.getElementsByTagName(nodeName);

        for (int i = 0; i < nl.getLength(); i++) {
            if (nodeName.equals(nl.item(i).getNodeName())) {
                if (returnlist == null) {
                    returnlist = new ArrayList<String>();
                }
                returnlist.add(nl.item(i).getTextContent());
            }

        }

        return returnlist;
    }

    /**
     * Get a xml string as an xml element object
     *
     * @param xmlString String to convert e.g. a server request or response
     * @return org.w3c.dom.Element from XML String
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created which satisfies the configuration requested
     * @throws IOException                  If any IO errors occur
     * @throws SAXException                 If any parse errors occur
     */
    private Element getNodeList(@Nonnull String xmlString) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        ByteArrayInputStream bis = new ByteArrayInputStream(xmlString.getBytes());
        Document doc = db.parse(bis);

        return doc.getDocumentElement();
    }

    /**
     * Create SOAP message object for server request. Will print debug information if debug is set to true
     *
     * @param reqType                  Type of request message e.g. singing or pending request
     * @param digestMethodAlgorithmURL Uri of hash algorithm
     * @param mobileIDStepUp           certificate request profile. Only necessary when on demand certificate is needed
     * @param hashList                 Hashes from documents which should be signed
     * @param additionalProfiles       Urn of additional profiles e.g. ondemand certificate, timestamp signature, batch process etc.
     * @param claimedIdentity          Signers identity / profile
     * @param signatureType            Urn of signature type e.g. signature type cms or timestamp
     * @param distinguishedName        Information about signer e.g. name, country etc.
     * @param mobileIdType             Urn of mobile id type
     * @param phoneNumber              Mobile id for on demand certificates with mobile id request
     * @param certReqMsg               Message which will be send to phone number if set
     * @param certReqMsgLang           Language from message which will be send to mobile id
     * @param responseId               Only necessary when asking the signing status on server
     * @param requestId                Request id to identify signature in response
     * @return SOAP response from server. Depending on request profile it can be a signarure, signing status information or an error
     * @throws SOAPException If there is an error creating SOAP message
     * @throws IOException   If there is an error writing debug information
     */
    private SOAPMessage createRequestMessage(@Nonnull Include.RequestType reqType, @Nonnull String digestMethodAlgorithmURL,
                                             boolean mobileIDStepUp, @Nonnull byte[][] hashList,
                                             String[] additionalProfiles, String claimedIdentity,
                                             @Nonnull String signatureType, String distinguishedName,
                                             String mobileIdType, String phoneNumber, String certReqMsg, String certReqMsgLang,
                                             String responseId, String requestId) throws SOAPException, IOException {

        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.removeNamespaceDeclaration("SOAP-ENV");
        envelope.setPrefix("soap");
        envelope.addAttribute(new QName("xmlns"), "urn:oasis:names:tc:dss:1.0:core:schema");
        envelope.addNamespaceDeclaration("dsig", "http://www.w3.org/2000/09/xmldsig#");
        envelope.addNamespaceDeclaration("sc", "http://ais.swisscom.ch/1.0/schema");
        envelope.addNamespaceDeclaration("ais", "http://service.ais.swisscom.com/");

        //SOAP Header
        SOAPHeader soapHeader = envelope.getHeader();
        soapHeader.removeNamespaceDeclaration("SOAP-ENV");
        soapHeader.setPrefix("soap");

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        soapBody.removeNamespaceDeclaration("SOAP-ENV");
        soapBody.setPrefix("soap");

        SOAPElement signElement = soapBody.addChildElement("sign", "ais");

        SOAPElement requestElement = signElement.addChildElement(reqType.getRequestType());
        requestElement.addAttribute(new QName("Profile"), reqType.getUrn());
        requestElement.addAttribute(new QName("RequestID"), requestId);
        SOAPElement inputDocumentsElement = requestElement.addChildElement("InputDocuments");

        SOAPElement digestValueElement;
        SOAPElement documentHashElement;
        SOAPElement digestMethodElement;

        for (int i = 0; i < hashList.length; i++) {
            documentHashElement = inputDocumentsElement.addChildElement("DocumentHash");
            if (hashList.length > 1)
                documentHashElement.addAttribute(new QName("ID"), String.valueOf(i));
            digestMethodElement = documentHashElement.addChildElement("DigestMethod", "dsig");
            digestMethodElement.addAttribute(new QName("Algorithm"), digestMethodAlgorithmURL);
            digestValueElement = documentHashElement.addChildElement("DigestValue", "dsig");

            String s = com.itextpdf.text.pdf.codec.Base64.encodeBytes(hashList[i], Base64.DONT_BREAK_LINES);
            digestValueElement.addTextNode(s);
        }

        if (additionalProfiles != null || claimedIdentity != null || signatureType != null) {
            SOAPElement optionalInputsElement = requestElement.addChildElement("OptionalInputs");

            SOAPElement additionalProfileelement;
            if (additionalProfiles != null) {
                for (String additionalProfile : additionalProfiles) {
                    additionalProfileelement = optionalInputsElement.addChildElement("AdditionalProfile");
                    additionalProfileelement.addTextNode(additionalProfile);
                }
            }

            if (claimedIdentity != null) {
                SOAPElement claimedIdentityElement = optionalInputsElement.addChildElement(new QName("ClaimedIdentity"));
                SOAPElement claimedIdNameElement = claimedIdentityElement.addChildElement("Name");
                claimedIdNameElement.addTextNode(claimedIdentity);
            }

            if (mobileIDStepUp) {
                SOAPElement certificateRequestElement = optionalInputsElement.addChildElement("CertificateRequest", "sc");
                if (distinguishedName != null) {
                    SOAPElement distinguishedNameElement = certificateRequestElement.addChildElement("DistinguishedName", "sc");
                    distinguishedNameElement.addTextNode(distinguishedName);
                    if (phoneNumber != null) {
                        SOAPElement stepUpAuthorisationElement = certificateRequestElement.addChildElement("StepUpAuthorisation", "sc");
                        if (mobileIdType != null) {
                            SOAPElement mobileIdElement = stepUpAuthorisationElement.addChildElement("MobileID", "sc");
                            mobileIdElement.addAttribute(new QName("Type"), _MOBILE_ID_TYPE);
                            SOAPElement msisdnElement = mobileIdElement.addChildElement("MSISDN", "sc");
                            msisdnElement.addTextNode(phoneNumber);
                            SOAPElement certReqMsgElement = mobileIdElement.addChildElement("Message", "sc");
                            certReqMsgElement.addTextNode(certReqMsg);
                            SOAPElement certReqMsgLangElement = mobileIdElement.addChildElement("Language", "sc");
                            certReqMsgLangElement.addTextNode(certReqMsgLang);
                        }
                    }
                }
            }

            if (signatureType != null) {
                SOAPElement signatureTypeElement = optionalInputsElement.addChildElement("SignatureType");
                signatureTypeElement.addTextNode(signatureType);
            }

            
            if (!signatureType.equals(_TIMESTAMP_URN)) {
                SOAPElement addTimeStampelement = optionalInputsElement.addChildElement("AddTimestamp");
                addTimeStampelement.addAttribute(new QName("Type"), _TIMESTAMP_URN);
            }

            // Always add revocation information
            // Type="BOTH" means PADES+CADES
            // PADES = signed attribute according to PAdES
            // CADES = unsigned attribute according to CAdES
            // PADES-attributes are signed and cannot be post-added to an already signed RFC3161-TimeStampToken
            // So the RevocationInformation (RI) of a trusted timestamp will be delivered via OptionalOutputs
         	// and they shall be added to the Adobe DSS in order to enable LTV for a Timestamp
			SOAPElement addRevocationElement = optionalInputsElement.addChildElement("AddRevocationInformation", "sc");
			addRevocationElement.addAttribute(new QName("Type"), "BOTH"); // CADES + PADES attributes

            if (responseId != null) {
                SOAPElement responseIdElement = optionalInputsElement.addChildElement("ResponseID");
                responseIdElement.addTextNode(responseId);
            }
        }

        soapMessage.saveChanges();

        if (_debugMode) {
            System.out.print("\nRequest SOAP Message:\n");
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            soapMessage.writeTo(ba);
            String msg = new String(ba.toByteArray());
            System.out.println(getPrettyFormatedXml(msg, 2));
        }

        return soapMessage;
    }

    /**
     * Creating connection object and send request to server. If debug is set to true it will print response message.
     *
     * @param soapMsg Message which will be send to server
     * @param urlPath Url of server where to send the request
     * @return Server response
     * @throws Exception If creating connection ,sending request or reading response failed
     */
    @Nullable
    private String sendRequest(@Nonnull SOAPMessage soapMsg, @Nonnull String urlPath) throws Exception {

        URLConnection conn = new Connect(urlPath, _privateKeyName, _serverCertPath, _clientCertPath, _timeout, _debugMode).getConnection();
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setRequestMethod("POST");
        }

        conn.setAllowUserInteraction(true);
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setDoOutput(true);

        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        soapMsg.writeTo(baos);
        String msg = baos.toString();

        out.write(msg);
        out.flush();
        if (out != null) {
            out.close();
        }

        String line = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String response = "";
        while ((line = in.readLine()) != null) {
            response = response.length() > 0 ? response + " " + line : response + line;
        }

        if (in != null) {
            in.close();
        }

        if (_debugMode) {
            System.out.println("\nSOAP response message:\n" + getPrettyFormatedXml(response, 2));
        }

        return response;
    }

    /**
     * Calculate size of signature
     *
     * @param sigType     Signature Type (TIMESTAMP,STATIC,ONDEMAND
     * @return Calculated size of external signature as int
     */
    private int getEstimatedSize(boolean isTimestampOnly) {
    	if (isTimestampOnly)
    		return 10000;
    	else 
    		return 22000;
    }

    /**
     * Check if given files exist and are files
     *
     * @param filePaths Files to check
     * @throws FileNotFoundException If file will not be found or is not readable
     */
    private void checkFilesExistsAndIsFile(@Nonnull String[] filePaths) throws FileNotFoundException {

        File file;
        for (String filePath : filePaths) {
            file = new File(filePath);
            if (!file.isFile() || !file.canRead()) {
                throw new FileNotFoundException("File not found or is not a file or not readable: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Convert a xml text which is not formated to a pretty format
     *
     * @param input  Input text
     * @param indent Set indent from left
     * @return Pretty formated xml
     */
    public String getPrettyFormatedXml(@Nonnull String input, int indent) {

        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);

            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            return input;
        }
    }

    /**
     * Generate a new request id with actually time and a 3 digit random number. Output looks like 22.01.2014 17:10:26:0073122
     *
     * @return Request id as String
     */
    public String getRequestId() {
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss:SSSS");
        int randomNumber = (int) (Math.random() * 1000);
        return (df.format(new Date()).concat(String.valueOf(randomNumber)));
    }

}

