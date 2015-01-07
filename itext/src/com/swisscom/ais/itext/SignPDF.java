/**
 * Created:
 * 18.12.13 KW 51 10:42
 * </p>
 * Last Modification:
 * 18.02.2014 13:47
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
 * </p>
 * **********************************************************************************************************
 * This is a wrapper class for the 'Soap' class                                                             *
 * Only program arguments will be handled                                                                   *
 * At least 'Soap' will be called with arguments                                                        *
 * **********************************************************************************************************
 */

package com.swisscom.ais.itext;

import javax.annotation.*;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;

public class SignPDF {

    /**
     * The value is used to decide if verbose information should be print
     */
    static boolean verboseMode = false;

    /**
     * The value is used to decide if debug information should be print
     */
    static boolean debugMode = false;

    /**
     * The signature type. E.g. timestamp, sign, ...
     */
    Include.Signature signature = null;

    /**
     * Path to pdf which get a signature
     */
    String pdfToSign = null;

    /**
     * Path to output document with generated signature
     */
    String signedPDF = null;

    /**
     * Reason for signing a document.
     */
    String signingReason = null;

    /**
     * Location where a document was signed
     */
    String signingLocation = null;

    /**
     * Person who signed the document
     */
    String signingContact = null;
    
    /**
     * Certification Level
     */
    int certificationLevel = 0;

    /**
     * Distinguished name contains information about signer. Needed for ondemand signature
     */
    String distinguishedName = null;

    /**
     * Mobile phone number to send a message when signing a document. Needed for signing with mobile id
     */
    String msisdn = null;

    /**
     * Message which will be send to mobile phone with mobile id. Needed for signing with mobile id.
     */
    String msg = null;

    /**
     * Language of the message which will be send to mobile phone with mobile id. Needed for signing with mobile id.
     */
    String language = null;

    /**
     * Path for properties file. Needed if standard path will not be used.
     */
    String propertyFilePath = null;
    
    /**
     * Main method to start AIS. This will parse given parameters e.g. input file, output file etc. and start signature
     * process. Furthermore this method prints error message if signing failed. See usage part in README to know how to
     * use it.
     *
     * @param args Arguments that will be parsed. See useage part in README for more details.
     */
    public static void main(String[] args) {

        SignPDF ais = new SignPDF();
        ais.runSigning(args);

    }
    
    /**
     * Parse given parameters, check if all necessary parameters exist and if there are not unnecessary parameters.
     * If there are problems with parameters application will abort with exit code 1.
     * After all checks are done signing process will start.
     *
     * @param params
     */
    private void runSigning(String[] params) {

        parseParameters(params);
        checkNecessaryParams();
        checkUnnecessaryParams();

        try {
            //parse signature
            if (signature.equals(Include.Signature.SIGN) && distinguishedName != null) {
                signature = Include.Signature.ONDEMAND;
            } else if (signature.equals(Include.Signature.SIGN) && distinguishedName == null) {
                signature = Include.Signature.STATIC;
            }

            //start signing
            Soap dss_soap = new Soap(verboseMode, debugMode, propertyFilePath);
            dss_soap.sign(signature, pdfToSign, signedPDF, signingReason, signingLocation, signingContact, certificationLevel, distinguishedName, msisdn, msg, language);
        } catch (Exception e) {
            if (debugMode || verboseMode) {
                printError(e.getMessage().replaceAll("java.lang.Exception", "").length() > 0 ? e.getMessage() : "");
            }
            System.exit(1);
        }
    }
    
    private void printUsage() {
    	printUsage(null);
    }

    /**
     * Prints usage and exits
     */
    private void printUsage(String error) {
    	if (error != null && (debugMode || verboseMode)) {
    		printError(error);
    	}
        System.out.println("\nUsage: com.swisscom.ais.itext.SignPDF [OPTIONS]");
        System.out.println();
        System.out.println("OPTIONS");
        System.out.println();
        System.out.println("  [mandatory]");
        System.out.println("  -type=VALUE       - signature type");
        System.out.println("                       supported values:");
        System.out.println("                       - timestamp (add timestamp only)");
        System.out.println("                       - sign      (add cms signature incl. timestamp)");
        System.out.println("  -infile=VALUE     - input filename of the pdf to be signed");
        System.out.println("  -outfile=VALUE    - output filename for the signed pdf");
        System.out.println();
        System.out.println("  [optional]");
        System.out.println("  -v                - verbose output");
        System.out.println("  -vv               - more verbose output");
        System.out.println("  -config=VALUE     - custom path to properties file, overwrites default path");
        System.out.println("  -reason=VALUE     - signing reason");
        System.out.println("  -location=VALUE   - signing location");
        System.out.println("  -contact=VALUE    - signing contact");
        System.out.println("  -certlevel=VALUE  - certify the pdf, at most one certification per pdf is allowed");
        System.out.println("                       supported values:");
        System.out.println("                       - 1 (no changes allowed)");
        System.out.println("                       - 2 (form filling and further signing allowed)");
        System.out.println("                       - 3 (form filling, annotations and further signing allowed)");    
        System.out.println("  -dn=VALUE         - distinguished name, for personal on demand certificate signing");
        System.out.println("                       supported attributes, separated by commas:");
        System.out.println("                       [mandatory]");
        System.out.println("                       - cn / commonname ");
        System.out.println("                       - c / countryname");
        System.out.println("                       [optional]");
        System.out.println("                       - emailaddress");
        System.out.println("                       - givenname");
        System.out.println("                       - l / localityname");
        System.out.println("                       - ou / organizationalunitname");
        System.out.println("                       - o / organizationname");
        System.out.println("                       - serialnumber");
        System.out.println("                       - st / stateorprovincename");
        System.out.println("                       - sn / surname");
        System.out.println("  -msisdn=VALUE     - mobileid step up phone number            (requires -dn -msg -lang)");
        System.out.println("  -msg=VALUE        - mobileid step up message to be displayed (requires -dn -msisdn -lang)");
        System.out.println("                      A placeholder #TRANSID# may be used anywhere in the message to include a unique transaction id.");
        System.out.println("  -lang=VALUE       - mobileid step up language                (requires -dn -msisdn -msg)");
        System.out.println("                       supported values:");
        System.out.println("                       - en (english)");
        System.out.println("                       - de (deutsch)");
        System.out.println("                       - fr (français)");
        System.out.println("                       - it (italiano)");
        System.out.println();
        System.out.println("EXAMPLES");
        System.out.println();
        System.out.println("  [timestamp]");
        System.out.println("    java com.swisscom.ais.itext.SignPDF -type=timestamp -infile=sample.pdf -outfile=signed.pdf");
        System.out.println("    java com.swisscom.ais.itext.SignPDF -v -type=timestamp -infile=sample.pdf -outfile=signed.pdf");
        System.out.println();
        System.out.println("  [sign with static certificate]");
        System.out.println("    java com.swisscom.ais.itext.SignPDF -type=sign -infile=sample.pdf -outfile=signed.pdf");
        System.out.println("    java com.swisscom.ais.itext.SignPDF -v -config=/tmp/signpdf.properties -type=sign -infile=sample.pdf -outfile=signed.pdf -reason=Approved -location=Berne -contact=alice@acme.com");
        System.out.println();
        System.out.println("  [sign with on demand certificate]");
        System.out.println("    java com.swisscom.ais.itext.SignPDF -type=sign -infile=sample.pdf -outfile=signed.pdf -dn='cn=Alice Smith,o=ACME,c=CH'");
        System.out.println("    java com.swisscom.ais.itext.SignPDF -v -type=sign -infile=sample.pdf -outfile=signed.pdf -dn='cn=Alice Smith,o=ACME,c=CH' -certlevel=1");
        System.out.println();
        System.out.println("  [sign with OnDemand certificate and Mobile ID step up]");
        System.out.println("    java com.swisscom.ais.itext.SignPDF -v -type=sign -infile=sample.pdf -outfile=signed.pdf -dn='cn=Alice Smith,o=ACME,c=CH' -msisdn=41792080350 -msg='acme.com: Sign the PDF? (#TRANSID#)' -lang=en");
        System.exit(1);
    }

    /**
     * Prints error message
     *
     * @param error Message that should print
     */
    private void printError(@Nonnull String error) {
    	// do not use error output stream to ensure proper order of all println's
    	if (error != null && error != "")
    		System.out.println("Error: " + error);
    }

    /**
     * Parse given parameters. If an error occurs application with exit with code 1. If debug and/or verbose mode is set
     * an error message will be shown
     * @param args
     */
    private void parseParameters(String[] args) {
    	
    	// args can never be null. It would just be of size zero.

        String param;
        boolean type = false, infile = false, outfile = false; 
        for (int i = 0; i < args.length; i++) {

            param = args[i].toLowerCase();

            if (param.contains("-type=")) {
                String signatureString = null;
                try {
                    signatureString = args[i].substring(args[i].indexOf("=") + 1).trim().toUpperCase();
                    signature = Include.Signature.valueOf(signatureString);
                    type = true;
                } catch (IllegalArgumentException e) {
                    if (debugMode || verboseMode) {
                        printError(signatureString + " is not a valid signature.");
                    }
                    printUsage();
                }
            } else if (param.contains("-infile=")) {
                pdfToSign = args[i].substring(args[i].indexOf("=") + 1).trim();
                File pdfToSignFile = new File(pdfToSign);
                if (!pdfToSignFile.isFile() || !pdfToSignFile.canRead()) {
                    if (debugMode || verboseMode) {
                        printError("File " + pdfToSign + " is not a file or can not be read.");
                    }
                    System.exit(1);
                }
                infile = true;
			} else if (param.contains("-outfile=")) {
				signedPDF = args[i].substring(args[i].indexOf("=") + 1).trim();
				String errorMsg = null;
				if (signedPDF.equals(pdfToSign)) {
					errorMsg = "Source file equals target file.";
				} else if (new File(signedPDF).isFile()) {
					errorMsg = "Target file exists.";
				} else {
					try {
						new File(signedPDF);
					} catch (Exception e) {
						errorMsg = "Can not create target file in given path.";
					}
				}
				if (errorMsg != null) {
					if (debugMode || verboseMode) {
						printError(errorMsg);
					}
					System.exit(1);
                }
				outfile = true;
            } else if (param.contains("-reason")) {
                signingReason = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-location")) {
                signingLocation = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-contact")) {
                signingContact = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-certlevel")) {
                try {
                	certificationLevel = Integer.parseInt(args[i].substring(args[i].indexOf("=") + 1).trim());
    				if (certificationLevel < 1 || certificationLevel > 3)
    					throw new Exception();
    			} catch (Exception e) {
    				if (debugMode || verboseMode) {
                        printUsage("-certlevel value not between 1..3");
                    }
    			}
            } else if (param.contains("-dn=")) {
                distinguishedName = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-msisdn=")) {
                msisdn = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-msg=")) {
                msg = args[i].substring(args[i].indexOf("=") + 1).trim();
                String transId = getNewTransactionId();
                msg = msg.replaceAll("#TRANSID#", transId);
            } else if (param.contains("-lang=")) {
                language = args[i].substring(args[i].indexOf("=") + 1).trim();
            } else if (param.contains("-config=")) {
                propertyFilePath = args[i].substring(args[i].indexOf("=") + 1).trim();
                File propertyFile = new File(propertyFilePath);
                if (!propertyFile.isFile() || !propertyFile.canRead()) {
                    if (debugMode || verboseMode) {
                        printError("Property file path is set but file does not exist or can not read it.");
                    }
                    System.exit(1);
                }
            } else if (args[i].toLowerCase().contains("-vv")) {
            	debugMode = true;
            } else if (param.contains("-v")) {
            	verboseMode = true;
            }
        }
        
        // Check existence of mandatory arguments
        if (!type) {
        	printUsage("Mandatory option -type is missing");
        } else if (!infile) {
        	printUsage("Mandatory option -infile is missing");
        } else if (!outfile) {
        	printUsage("Mandatory option -outfile is missing");
        } 
        
    }

    /**
     * Check if needed parameters are given. If not method will print an error and exit with code 1
     */
    private void checkNecessaryParams() {

        if (pdfToSign == null) {
            if (debugMode || verboseMode) {
                printError("Input file does not exist.");
            }
            System.exit(1);
        }

        if (signedPDF == null) {
            if (debugMode || verboseMode) {
                printError("Output file does not exist.");
            }
            System.exit(1);
        }
    }

    /**
     * This method checks if there are unnecessary parameters. If there are some it will print the usage of parameters
     * and exit with code 1 (e.g. DN is given for signing with timestamp)
     */
    private void checkUnnecessaryParams() {

        if (signature.equals(Include.Signature.TIMESTAMP)) {
            if (distinguishedName != null || msisdn != null || msg != null || language != null) {
                if (debugMode || verboseMode) {
                    printUsage();
                }
            }
        } else {
            if (!(distinguishedName == null && msisdn == null && msg == null && language == null ||
                    distinguishedName != null && msisdn == null && msg == null && language == null ||
                    distinguishedName != null && msisdn != null && msg != null && language != null)) {
                if (debugMode || verboseMode) {
                    printUsage();
                }
            }
        }
    }
    
    /**
     * Return a unique transaction id
     * @return transaction id
     */
    private String getNewTransactionId() {
    	// secure, easy but a little bit more expensive way to get a random alphanumeric string
        return new BigInteger(30, new SecureRandom()).toString(32);
    }   

}
