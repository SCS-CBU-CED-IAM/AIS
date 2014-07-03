/**
 * Class to connect to a server using certificates
 *
 * Created:
 * 03.12.13 KW49 14:51
 * </p>
 * Last Modification:
 * 22.01.2014 11:24
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

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collections;

public class Connect {

    /**
     * The value is used to decide if debug information should be print
     */
    boolean _debugMode = false;

    /**
     * The url of the server to make a connection
     */
    private String _url;

    /**
     * The private key
     */
    private String _privateKey;

    /**
     * Certificate of server where to connect
     */
    private String _serverCert;

    /**
     * Certificate to connect to server
     */
    private String _clientCert;

    /**
     * Connection timeout for server in milli seconds
     */
    private int _timeout;

    /**
     * Constructor to set relevant parameters and add security provider
     *
     * @param url        URL of the server where to connect
     * @param privateKey Private key of the user
     * @param serverCert Certificate of the server where to connect
     * @param clientCert Certificate to connect to the server and it should trust
     * @param timeout    Time for connection timeout in milli seconds
     * @param debug      If debug is set to true debug information will be print out. Otherwise it will not print debug information.
     */
    public Connect(@Nonnull String url, @Nonnull String privateKey, @Nonnull String serverCert, @Nonnull String clientCert,
                         int timeout, boolean debug) {
        this._url = url;
        this._privateKey = privateKey;
        this._serverCert = serverCert;
        this._clientCert = clientCert;
        this._timeout = timeout;
        _debugMode = debug;

        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Get a connection object. This will create key manager, trust manager, socket factory and set timeout.
     *
     * @return
     * @throws KeyManagementException   If SSLContext can not be initialized
     * @throws NoSuchAlgorithmException If no provider supports a TrustManagerFactorySpi implementation for the specified protocol
     * @throws IOException              If the string specifies an unknown protocol or if an I/O exception occurs
     */
    @Nullable
    public URLConnection getConnection() throws KeyManagementException, NoSuchAlgorithmException, IOException {

        KeyManager[] keyManagers = createKeyManagers();
        TrustManager[] trustManagers = createTrustManagers();
        SSLSocketFactory factory = initItAll(keyManagers, trustManagers);
        URLConnection con = createConnectionObject(_url, factory);
        con.setConnectTimeout(_timeout);

        return con;
    }

    /**
     * Creates a connection object
     *
     * @param urlString        String to parse as a URL.
     * @param sslSocketFactory The SSL socket factory
     * @return A URLConnection to the URL. If it is a https connection return type will be a HttpsURLConnection
     * @throws IOException If the string specifies an unknown protocol or if an I/O exception occurs
     */
    private URLConnection createConnectionObject(@Nonnull String urlString, @Nonnull SSLSocketFactory sslSocketFactory)
            throws IOException {

        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }
        return connection;
    }

    /**
     * Inits SSLContext and create a SocketFactoryObject
     *
     * @param keyManagers   Sources of authentication keys
     * @param trustManagers Sources of peer authentication trust decisions
     * @return A SocketFactory object
     * @throws NoSuchAlgorithmException If no provider supports a TrustManagerFactorySpi implementation for the specified protocol
     * @throws KeyManagementException   If SSLContext can not be initialized
     */
    private SSLSocketFactory initItAll(@Nonnull KeyManager[] keyManagers, @Nonnull TrustManager[] trustManagers) throws NoSuchAlgorithmException, KeyManagementException {

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, null);
        SSLSocketFactory socketFactory = context.getSocketFactory();
        return socketFactory;
    }

    /**
     * Create KeyManagers to handle keys
     *
     * @return Array with KeyManager objects
     */
    private KeyManager[] createKeyManagers() {

        KeyManager[] managers = new KeyManager[]{new Connect(_url, _privateKey, _serverCert, _clientCert, _timeout,
                _debugMode).new AliasKeyManager(_clientCert, _privateKey, _serverCert, _debugMode)};

        return managers;
    }

    /**
     * Create TrustManagers to check validation of server certificates. Here TrustManagers will not check if a client is trusted
     *
     * @return Array with Trustmanager object
     */
    @Nullable
    private TrustManager[] createTrustManagers() {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            X509Certificate[] trustedIssuers = null;

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                //not relevant here
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {

                if (chain == null || chain.length < 2) {
                    throw new CertificateException("Error when validating server certificate");
                }

                X509Certificate certToVerify = chain[0];
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                CertPath cp = cf.generateCertPath(Arrays.asList(new X509Certificate[]{certToVerify}));

                TrustAnchor trustAnchor = new TrustAnchor(chain[1], null);

                CertPathValidator cpv = null;
                try {
                    cpv = CertPathValidator.getInstance("PKIX");

                    PKIXParameters pkixParams = new PKIXParameters(Collections.singleton(trustAnchor));
                    pkixParams.setRevocationEnabled(false);

                    CertPathValidatorResult validated = cpv.validate(cp, pkixParams);

                    if (validated == null) {
                        throw new CertificateException("Error when validating server certificate");
                    }

                    trustedIssuers = chain;

                } catch (Exception e) {
                    throw new CertificateException("Error when validating server certificate");
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return trustedIssuers;
            }
        }};

        return trustAllCerts;
    }

    /**
     * Instances manage which certificate-based key pairs are used to authenticate the local side of a secure socket.
     */
    private class AliasKeyManager implements X509KeyManager {

        /**
         * Path of certificate for a client to authenticate to a server
         */
        private String clientCert;

        /**
         * Path of private key
         */
        private String _privateKeyName;

        /**
         * Path of certificate from server to authenticate it
         */
        private String _serverCert;

        /**
         * If value is set to true debug information will be print out otherwise not
         */
        private boolean _debugMode;

        /**
         * Constructor for this class. Sets variables
         *
         * @param clientCert     Path of certificate to authenticate to a server
         * @param privateKeyName Path of private key
         * @param serverCert     Path of certificate from server to authenticate it
         * @param debugMode      If set to true debug information will be print out otherwise not
         */
        public AliasKeyManager(@Nonnull String clientCert, @Nonnull String privateKeyName,
                               @Nonnull String serverCert, boolean debugMode) {

            this.clientCert = clientCert;
            this._privateKeyName = privateKeyName;
            this._serverCert = serverCert;
            this._debugMode = debugMode;
        }

        /**
         * Return path of client certificate to authenticate to a server
         *
         * @param str       Set to null. Is only given because class implements interface
         * @param principal Set to null. Is only given because class implements interface
         * @param socket    Set to null. Is only given because class implements interface
         * @return path of client certificate
         */
        public String chooseClientAlias(@Nullable String[] str, @Nullable Principal[] principal, @Nullable Socket socket) {
            return clientCert;
        }

        /**
         * Return path of server certificate to authenticate the server
         *
         * @param str       Set to null. Is only given because class implements interface
         * @param principal Set to null. Is only given because class implements interface
         * @param socket    Set to null. Is only given because class implements interface
         * @return path of server certificate
         */
        public String chooseServerAlias(@Nullable String str, @Nullable Principal[] principal, @Nullable Socket socket) {
            return _serverCert;
        }

        /**
         * Create X509 certificate chain from client certificate to authenticate against a server
         *
         * @param clientCertFilePath Path of client certificate
         * @return Array with X509 certificates. Null if chain can not be created of client certificate
         */
        @Nullable
        public X509Certificate[] getCertificateChain(String clientCertFilePath) {

            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Certificate certificate = certificateFactory.generateCertificate(new FileInputStream(clientCertFilePath));
                return new X509Certificate[]{(X509Certificate) certificate};
            } catch (Exception e) {
                if (_debugMode)
                    e.printStackTrace();
                return null;
            }
        }

        /**
         * Return path of client certificate to authenticate against a server
         *
         * @param str       Set to null. Is only given because class implements interface
         * @param principal Set to null. Is only given because class implements interface
         * @return Array with only entry. This is the path of client certificate
         */
        public String[] getClientAliases(@Nullable String str, @Nullable Principal[] principal) {
            return new String[]{clientCert};
        }

        /**
         * Return path of client certificate to authenticate against a server
         *
         * @param str       Set to null. Is only given because class implements interface
         * @param principal Set to null. Is only given because class implements interface
         * @return Array with only entry. This is the path of client certificate
         */
        public String[] getServerAliases(String str, Principal[] principal) {
            return new String[]{clientCert};
        }

        /**
         * Create private key from file. Creating the key depends on the type. An existing X509 key file can be immediately
         * convert to a PrivateKey object. If source file is a RSA key it is necessary to create a PEMKeyPair first and
         * afterwards convert to PrivateKey object
         *
         * @param privateKeyPath Path of private key
         * @return PrivateKey if key can be generated otherwise null
         */
        @Nullable
        public PrivateKey getPrivateKey(String privateKeyPath) {

            try {
                BufferedReader br = new BufferedReader(new FileReader(_privateKeyName));

                //if we read a X509 key we will get immediately PrivatekeyInfo if key is a RSA key it is necessary to
                //create a PEMKeyPair first
                PrivateKeyInfo privateKeyInfo = null;
                PEMParser pemParser = null;
                try {
                    pemParser = new PEMParser(br);
                    privateKeyInfo = (PrivateKeyInfo) pemParser.readObject();
                } catch (Exception e) {
                    br.close();
                    br = new BufferedReader(new FileReader(_privateKeyName));
                    pemParser = new PEMParser(br);
                    PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
                    privateKeyInfo = pemKeyPair.getPrivateKeyInfo();
                }

                pemParser.close();
                br.close();

                JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
                java.security.PrivateKey privateKey = jcaPEMKeyConverter.getPrivateKey(privateKeyInfo);

                return privateKey;

            } catch (Exception e) {
                if (_debugMode)
                    e.printStackTrace();

                return null;
            }
        }
    }

}
