/**
 * This class defines enums.
 *
 * Created:
 * 19.12.13 KW51 08:04
 * </p>
 * Last Modification:
 * 22.01.2014 16:34
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

/**
 * Defines enums
 */
public class Include {

    /**
     * Enumeration for different hash algorithm names amd its uri
     */
    public enum HashAlgorithm {

        /**
         * Hash algorithm SHA 256
         */
        SHA256("SHA-256", "http://www.w3.org/2001/04/xmlenc#sha256"),

        /**
         * Hash algorithm SHA 384
         */
        SHA384("SHA-384", "http://www.w3.org/2001/04/xmldsig-more#sha384"),

        /**
         * Hash algorithm SHA 512
         */
        SHA512("SHA-512", "http://www.w3.org/2001/04/xmlenc#sha512");

        /**
         * Name of hash algorithm
         */
        private String hashAlgo;

        /**
         * Uri of hash algorithm
         */
        private String hashUri;

        /**
         * Set name and uri of hash algorithm
         *
         * @param hashAlgo Name of hash algorithm
         * @param hashUri  Uri of hash algorithm
         */
        HashAlgorithm(String hashAlgo, String hashUri) {
            this.hashAlgo = hashAlgo;
            this.hashUri = hashUri;
        }

        /**
         * Get name of hash algorithm
         *
         * @return String with name of hash algorithm
         */
        public String getHashAlgorythm() {
            return this.hashAlgo;
        }

        /**
         * Get uri of hash algorithm
         *
         * @return String with uri of hash algorithm
         */
        public String getHashUri() {
            return this.hashUri;
        }

    }

    /**
     * Enumeration for different request status on server
     */
    public enum RequestResult {

        /**
         * Urn for pending request
         */
        Pending("urn:oasis:names:tc:dss:1.0:profiles:asynchronousprocessing:resultmajor:Pending"),

        /**
         * URN for successfully request
         */
        Success("urn:oasis:names:tc:dss:1.0:resultmajor:Success");

        /**
         * Urn of request result
         */
        private String resultUrn;

        /**
         * Set urn of request result
         *
         * @param urn Urn of request result
         */
        RequestResult(String urn) {
            this.resultUrn = urn;
        }

        /**
         * Get urn of request result
         *
         * @return String with urn of request result
         */
        public String getResultUrn() {
            return this.resultUrn;
        }
    }

    /**
     * Enumeration for different request types
     */
    public enum RequestType {

        /**
         * Request type sign request with name and urn
         */
        SignRequest("SignRequest", "http://ais.swisscom.ch/1.0"),

        /**
         * Request type pending request with name and urn
         */
        PendingRequest("PendingRequest", "http://ais.swisscom.ch/1.0");

        /**
         * Urn of request type
         */
        private String urn;

        /**
         * Name of request type
         */
        private String requestType;

        /**
         * Set name and urn off request type
         *
         * @param reqType
         * @param urn
         */
        RequestType(String reqType, String urn) {
            this.requestType = reqType;
            this.urn = urn;
        }

        /**
         * Get name of request type
         *
         * @return String with name of request type
         */
        public String getRequestType() {
            return this.requestType;
        }

        /**
         * Get urn of request type
         *
         * @return String with urn of request type
         */
        public String getUrn() {
            return this.urn;
        }

    }

    /**
     * Enumeration for signatures.
     */
    public enum Signature {

        /**
         * Signature type sign
         */
        SIGN("sign"),

        /**
         * Signature type timestamp
         */
        TIMESTAMP("timestamp"),

        /**
         * Signature type static
         */
        STATIC("static"),

        /**
         * Signature type ondemand
         */
        ONDEMAND("ondemand");

        /**
         * Name of signature
         */

        /**
         * Set name of signature
         *
         * @param signature Name of signature
         */
        Signature(String signature) {
            ; //nothing to do
        }

    }

    /**
     * Enumeration for signature type
     */
    public enum SignatureType {

        /**
         * Signature type cms with urn
         */
        CMS("urn:ietf:rfc:3369"),

        /**
         * Signature type timestamp with urn
         */
        TIMESTAMP("urn:ietf:rfc:3161");

        /**
         * Urn of signature type
         */
        private String signatureType;

        /**
         * Set urn of signature
         *
         * @param signatureType Urn of signature type
         */
        SignatureType(String signatureType) {
            this.signatureType = signatureType;
        }

        /**
         * Get urn of signature
         *
         * @return String with urn from signature type
         */
        public String getSignatureType() {
            return this.signatureType;
        }

    }

    /**
     * Enumeration for additional profiles
     */
    public enum AdditionalProfiles {

        /**
         * Additional profile type asynchron with urn
         */
        ASYNCHRON("urn:oasis:names:tc:dss:1.0:profiles:asynchronousprocessing"),

        /**
         * Additional profile type batch with urn
         */
        BATCH("http://ais.swisscom.ch/1.0/profiles/batchprocessing"),

        /**
         * Additional profile type on demand certificate with urn
         */
        ON_DEMAND_CERTIFCATE("http://ais.swisscom.ch/1.0/profiles/ondemandcertificate"),

        /**
         * Additional profile type timestamp with urn
         */
        TIMESTAMP("urn:oasis:names:tc:dss:1.0:profiles:timestamping");

        /**
         * Urn of additional profile
         */
        private String profile;

        /**
         * Set urn of additional profile
         *
         * @param s Urn of additional profile
         */
        AdditionalProfiles(String s) {
            this.profile = s;
        }

        /**
         * Get urn name of additional profile
         *
         * @return
         */
        public String getProfileName() {
            return this.profile;
        }

    }

}
