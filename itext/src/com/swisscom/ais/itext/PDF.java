/**
 * Class to modify or get information from pdf document
 *
 * Created:
 * 19.12.13 KW51 08:04
 * </p>
 * Last Modification:
 * 22.01.2014 13:58
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

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.codec.Base64;
import com.itextpdf.text.pdf.security.*;

import javax.annotation.Nonnull;

import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPResp;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;

public class PDF {

    /**
     * Save file path from input file
     */
    private String inputFilePath;

    /**
     * Save file path from output file
     */
    private String outputFilePath;

    /**
     * Save password from pdf
     */
    private String pdfPassword;

    /**
     * Save signing reason
     */
    private String signReason;

    /**
     * Save signing location
     */
    private String signLocation;

    /**
     * Save signing contact
     */
    private String signContact;

    /**
     * Save signature appearance from pdf
     */
    private PdfSignatureAppearance pdfSignatureAppearance;

    /**
     * Save pdf signature
     */
    private PdfSignature pdfSignature;

    /**
     * Save byte array outputstream for writing pdf file
     */
    private ByteArrayOutputStream byteArrayOutputStream;

    /**
     * Set parameters
     *
     * @param inputFilePath  Path from input file
     * @param outputFilePath Path from output file
     * @param pdfPassword    Password form pdf
     * @param signReason     Reason from signing
     * @param signLocation   Location for frOn signing
     * @param signContact    Contact for signing
     */
    PDF(@Nonnull String inputFilePath, @Nonnull String outputFilePath, String pdfPassword, String signReason, String signLocation, String signContact) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.pdfPassword = pdfPassword;
        this.signReason = signReason;
        this.signLocation = signLocation;
        this.signContact = signContact;
    }

    /**
     * Get file path of pdf to sign
     *
     * @return Path from pdf to sign
     */
    public String getInputFilePath() {
        return inputFilePath;
    }

    /**
     * Add signature information (reason for signing, location, contact, date) and create hash from pdf document
     *
     * @param signDate        Date of signing
     * @param estimatedSize   The estimated size for signatures
     * @param hashAlgorithm   The hash algorithm which will be used to sign the pdf
     * @param isTimestampOnly If it is a timestamp signature. This is necessary because the filter is an other one compared to a "standard" signature
     * @return Hash of pdf as bytes
     * @throws IOException              If the input file can not be readed
     * @throws DocumentException        If PdfStamper can not create the signature or signature appearance can not be preclosed
     * @throws NoSuchAlgorithmException If no Provider supports a MessageDigest implementation for the specified algorithm.
     */
    public byte[] getPdfHash(@Nonnull Calendar signDate, int estimatedSize, @Nonnull String hashAlgorithm, boolean isTimestampOnly)
            throws IOException, DocumentException, NoSuchAlgorithmException {

        PdfReader pdfReader = null;
        pdfReader = new PdfReader(inputFilePath, pdfPassword != null ? pdfPassword.getBytes() : null);
        AcroFields acroFields = pdfReader.getAcroFields();
        boolean hasSignature = acroFields.getSignatureNames().size() > 0;
        byteArrayOutputStream = new ByteArrayOutputStream();
        PdfStamper pdfStamper = PdfStamper.createSignature(pdfReader, byteArrayOutputStream, '\0', null, hasSignature);
        pdfStamper.setXmpMetadata(pdfReader.getMetadata());

        pdfSignatureAppearance = pdfStamper.getSignatureAppearance();
        pdfSignature = new PdfSignature(PdfName.ADOBE_PPKLITE, isTimestampOnly ? PdfName.ETSI_RFC3161 : PdfName.ADBE_PKCS7_DETACHED);
        pdfSignature.setReason(signReason);
        pdfSignature.setLocation(signLocation);
        pdfSignature.setContact(signContact);
        pdfSignature.setDate(new PdfDate(signDate));
        pdfSignatureAppearance.setCryptoDictionary(pdfSignature);

        HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
        exc.put(PdfName.CONTENTS, new Integer(estimatedSize * 2 + 2));

        pdfSignatureAppearance.preClose(exc);

        MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
        InputStream rangeStream = pdfSignatureAppearance.getRangeStream();
        int i;
        while ((i = rangeStream.read()) != -1) {
            messageDigest.update((byte) i);
        }

        return messageDigest.digest();
    }

    /**
     * Add a signature to pdf document
     *
     * @param externalSignature  The extern generated signature
     * @param estimatedSize      Size of external signature
     * @throws IOException       If estimated size is to small, signature appearance can not be closed or output file can not be written
     * @throws DocumentException If signature appearance can not be closed
     */
    public void createSignedPdf(@Nonnull byte[] externalSignature, int estimatedSize) throws IOException, DocumentException {

    	if (Soap._debugMode) {
    		System.out.println("\nEstimated SignatureSize: " + estimatedSize);
    		System.out.println("Actual    SignatureSize: " + externalSignature.length);
        	System.out.println("Remaining Size         : " + (estimatedSize-externalSignature.length));
    	}
		
        if (estimatedSize < externalSignature.length) {
            throw new IOException("\nNot enough space for signature (" + (estimatedSize-externalSignature.length) + " bytes)");
        }

        PdfLiteral pdfLiteral = (PdfLiteral) pdfSignature.get(PdfName.CONTENTS);
        byte[] outc = new byte[(pdfLiteral.getPosLength() - 2) / 2];

        Arrays.fill(outc, (byte) 0);

        System.arraycopy(externalSignature, 0, outc, 0, externalSignature.length);
        PdfDictionary dic2 = new PdfDictionary();
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        pdfSignatureAppearance.close(dic2);
           
        // Save to file
        OutputStream outputStream = new FileOutputStream(outputFilePath);
        byteArrayOutputStream.writeTo(outputStream);
        
        if (Soap._debugMode) {
	    	System.out.println("\nOK writing to " + outputFilePath);
	    }
        
        byteArrayOutputStream.close();
        outputStream.close();
    }
	
	/**
	 * @param ocspArr List of OCSP Responses as base64 encoded String
	 * @param crlArr  List of CRLs as base64 encoded String
	 * @throws IOException
	 * @throws DocumentException
	 * @throws GeneralSecurityException
	 * @throws OCSPException
	 */
	public void addValidationInformation(ArrayList<String> ocspArr, ArrayList<String> crlArr) throws IOException, DocumentException, GeneralSecurityException, OCSPException {
		if (ocspArr == null && crlArr == null)
			return;
		
		Collection<byte[]> ocspColl = new ArrayList<byte[]>();
		Collection<byte[]> crlColl = new ArrayList<byte[]>();

		OCSPResp ocspResp = null;
		BasicOCSPResp basicResp = null;
		
		// Decode each OCSP Response (String of base64 encoded form) and add it to the Collection (byte[])
		if (ocspArr != null) {
			for (String ocspBase64 : ocspArr) {
				ocspResp = new OCSPResp(new ByteArrayInputStream(Base64.decode(ocspBase64)));
				basicResp = (BasicOCSPResp) ocspResp.getResponseObject();
				
				if (Soap._debugMode) {
					System.out.println("\nEmbedding OCSP Response...");
					System.out.println("Status                : " + ((ocspResp.getStatus() == 0) ? "GOOD" : "BAD"));
					System.out.println("Produced at           : " + basicResp.getProducedAt());
					System.out.println("This Update           : " + basicResp.getResponses()[0].getThisUpdate());
					System.out.println("Next Update           : " + basicResp.getResponses()[0].getNextUpdate());
					System.out.println("X509 Cert Issuer      : " + basicResp.getCerts()[0].getIssuer());
					System.out.println("X509 Cert Subject     : " + basicResp.getCerts()[0].getSubject());
					System.out.println("Responder ID X500Name : " + basicResp.getResponderId().toASN1Object().getName());
					System.out.println("Certificate ID        : " + basicResp.getResponses()[0].getCertID().getSerialNumber().toString() + 
							" (" + basicResp.getResponses()[0].getCertID().getSerialNumber().toString(16).toUpperCase() + ")");
				}
					
				ocspColl.add(basicResp.getEncoded()); // Add Basic OCSP Response to Collection (ASN.1 encoded representation of this object) 
			}			
		}
		
		
		X509CRL x509crl = null;
		
		// Decode each CRL (String of base64 encoded form) and add it to the Collection (byte[])
		if (crlArr != null) {
			for (String crlBase64 : crlArr) {		
				x509crl = (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(Base64.decode(crlBase64)));
				
				if (Soap._debugMode) {
					System.out.println("\nEmbedding CRL...");
					System.out.println("IssuerDN                    : " + x509crl.getIssuerDN());
					System.out.println("This Update                 : " + x509crl.getThisUpdate());
					System.out.println("Next Update                 : " + x509crl.getNextUpdate());
					System.out.println("No. of Revoked Certificates : " + ((x509crl.getRevokedCertificates() == null) ? "0" : x509crl.getRevokedCertificates().size()));
				}
				
				crlColl.add(x509crl.getEncoded()); // Add CRL to Collection (ASN.1 DER-encoded form of this CRL)
			}
		}
		
		PdfReader reader = new PdfReader(outputFilePath);

		byteArrayOutputStream = new ByteArrayOutputStream();
		PdfStamper stamper = new PdfStamper(reader, byteArrayOutputStream, '\0', true);
	    LtvVerification validation = stamper.getLtvVerification();
	    
	    // Add the CRL/OCSP validation information to the DSS Dictionary
	    for (String sigName : stamper.getAcroFields().getSignatureNames()){
	        validation.addVerification(
	        	sigName,  // Signature Name
	        	ocspColl, // OCSP
	        	crlColl,  // CRL
	        	null      // certs
	        );
	    }
	    
	    validation.merge(); // Merges the validation with any validation already in the document or creates a new one.
	    
	    stamper.close();
	    reader.close();
	    
	    // Save to (same) file
	    OutputStream outputStream = new FileOutputStream (outputFilePath); 
	    byteArrayOutputStream.writeTo(outputStream);	    
	    
	    if (Soap._debugMode) {
	    	System.out.println("\nOK merging LTV validation information to " + outputFilePath);
	    }
	    
	    byteArrayOutputStream.close();
	    outputStream.close();  
	}

}
