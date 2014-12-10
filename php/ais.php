<?php
/**
 * @version        1.0.0
 * @package        AllinSigningService
 * @copyright      Copyright (C) 2014. All rights reserved.
 * @license        GNU General Public License version 2 or later; see LICENSE.md
 * @author         Swisscom (Schweiz) AG
 * Requirements    PHP 5.3.x, php_soap, php_libxml, OpenSSL
 *
 * Open Issues     Switch from exec openssl to proper OpenSSL PHP function to find signer certificate
 */

error_reporting(E_ALL | E_STRICT);

class AllinSigningService {
    private $customerID;                   // CustomerID provided by Swisscom

    private $client;                       // SOAP client
    const WSDL = 'schema/aisService.wsdl'; // AIS WSDL file

    const TIMEOUT_CON = 10;                // SOAP client connection timeout
    private $base_url;                     // Base URL of the service
    private $response;                     // SOAP client response

    public $resultmajor;                   // Result major
    public $resultminor;                   // Result minor
    public $resultmessage;                 // Result message

    private $ais_signature;                // AIS signature (Base64 encoded)
    public $sig_cert;                      // AIS certificate related to signature
    public $sig_certSubject;               // Signers subject
    public $sig_certMIDSN;                 // Signers Mobile ID serial number in the DN

    private $revocationInformation;        // Type of revocation information to be added
    private $addTimestamp;                 // Add timestamp to the signature?
 
    /**
     * Mobile ID class
     * #params     string    Customer ID provided by Swisscom
     * #params     string    Certificate/key that is allowed to access the service
     * #params     string    Location of Certificate Authority file which should be used to authenticate the identity of the remote peer
     * #params     array     Additional SOAP client options
     * @return     null
     */
    public function __construct($customerID, $cert, $cafile, $myOpts = null) {
        /* Set the Customer Infos */
        $this->customerID = $customerID;

        /* Set SOAP context and options */
        $context = stream_context_create(array(
            'ssl' => array(
                'verify_peer' => true,
                'allow_self_signed' => false,
                'cafile' => $cafile
                )
            ));
        $options = array(
            'trace' => true,
            'exceptions' => false,
            'encoding' => 'UTF-8',
            'soap_version' => SOAP_1_1,
            'local_cert' => $cert,
            'connection_timeout' => self::TIMEOUT_CON,
            'stream_context' => $context
            );
        if (isset($myOpts)) $options = array_merge($options, (array)$myOpts);

        /* Check for provided files existence */
        if (!file_exists($cert)) trigger_error('ais::construct: file not found ' . $cert, E_USER_WARNING);
        if (!file_exists($cafile))  trigger_error('ais::construct: file not found ' . $cafile, E_USER_WARNING);

        /* SOAP client with AIS service */
        $this->setBaseURL('https://ais.swisscom.com');
        $this->client = new SoapClient(dirname(__FILE__) . '/' . self::WSDL, $options);
    }

    private function __doCall($request, $params) {
        $this->resultmajor = '';
        $this->resultminor = '';
        $this->resultmessage = '';

        try {
            /* Call the SOAP function */
            $this->response = $this->client->__soapCall($request, array('parameters' => $params));

            /* SOAP fault ? */
            if (is_soap_fault($this->response)) {
                if (isset($this->response->faultcode))
                    $this->resultmajor = (string)$this->response->faultcode;
                if (isset($this->response->faultstring))
                    $this->resultminor = (string)$this->response->faultstring;
                return(false);
            }

            /* Get the ResultMajor */
            if (isset($this->response->SignResponse->Result->ResultMajor))
                $this->resultmajor = (string)$this->response->SignResponse->Result->ResultMajor;
            /* Get the ResultMinor */
            if (isset($this->response->SignResponse->Result->ResultMinor))
                $this->resultminor = (string)$this->response->SignResponse->Result->ResultMinor;
            /* Get the ResultMessage */
            if (isset($this->response->SignResponse->Result->ResultMessage->_))
                $this->resultmessage = (string)$this->response->SignResponse->Result->ResultMessage->_;

            if ($this->resultmajor === 'urn:oasis:names:tc:dss:1.0:resultmajor:Success') {
                return(true);
            } else {
                return(false);
            }
        } catch (Exception $e) {
            return(false);
        }
    }

    /**
     * signature request
     * #params     string    digest to be signed
     * #params     string    digest method
     * #params     string    (optional) DN for OnDemand
     * #params     string    (optional) Mobile Number if DN is set
     * #params     string    (optional) Mobile ID Message
     * #params     string    (optional) Mobile ID Language
     * @return     boolean   true on success, false on failure
     */
    public function sign($digestValue, $digestMethod, $DN='', $msisdn='', $msg='Do you want to sign?', $lang='em') {
        $this->sig_cert = '';
        $this->sig_certSubject = '';
        $this->sig_certMIDSN = '';

        /* Define the base elements */
        $params = array(
            'SignRequest' => array(
                'RequestID' => $this->__createTransID(),
                'Profile' => 'http://ais.swisscom.ch/1.0',
                'OptionalInputs' => array(
                    'ClaimedIdentity' => array(
                        'Name' => $this->customerID
                    ),
                    'SignatureType' => 'urn:ietf:rfc:3369'
                ),
                'InputDocuments' => array(
                    'DocumentHash' => array(
                        'DigestMethod' => array('Algorithm' => $digestMethod),
                        'DigestValue' => $digestValue
        )   )   )   );

        /* Define the AddTimestamp element */
        $addTimestamp = array(
            'SignRequest' => array(
                'OptionalInputs' => array(
                    'AddTimestamp' => array('Type' => 'urn:ietf:rfc:3161')
        )   )   );
        if (isset($this->addTimestamp) && $this->addTimestamp)
            $params = array_merge_recursive($params, (array)$addTimestamp);

        /* Define the OnDemand elements */
        $onDemand = array(
            'SignRequest' => array(
                'OptionalInputs' => array(
                    'AdditionalProfile' => 'http://ais.swisscom.ch/1.0/profiles/ondemandcertificate',
                    'CertificateRequest' => array(
                        'DistinguishedName' => $DN
        )   )   )   );
        if (isset($DN) && $DN !== '')
            $params = array_merge_recursive($params, (array)$onDemand);

        /* Define the Mobile ID setup elements */
        $stepUP = array(
           'SignRequest' => array(
                'OptionalInputs' => array(
                    'CertificateRequest' => array(
                        'StepUpAuthorisation' => array(
                            'MobileID' => array(
                                'Type' => 'http://ais.swisscom.ch/1.0/auth/mobileid/1.0',
                                'MSISDN' => $msisdn,
                                'Message' => $msg,
                                'Language' => $lang
        )   )   )   )   )   );
        if (isset($msisdn) && $msisdn !== '')
            $params = array_merge_recursive($params, (array)$stepUP);

        /* Define the AddRevocationInformation element */
        $addRevocationInformation = array(
            'SignRequest' => array(
                'OptionalInputs' => array(
                    'AddRevocationInformation' => array('Type' => $this->revocationInformation)
        )   )   );
        if (isset($this->revocationInformation) && $this->revocationInformation !== '')
            $params = array_merge_recursive($params, (array)$addRevocationInformation);

        $this->client->__setLocation($this->base_url . '/AIS-Server/ws');
        if (!$this->__doCall('sign', $params)) return(false);

        /* Get the signature and signer details */
        $ais_signature = '';
        if (isset($this->response->SignResponse->SignatureObject->Base64Signature->_)) {
            $ais_signature = (string)$this->response->SignResponse->SignatureObject->Base64Signature->_;

            /* Ensure proper base64 encoding */
            if (base64_decode($ais_signature, true)) $this->ais_signature = $ais_signature;
            else $this->ais_signature = base64_encode($ais_signature);

            /* Get the signer certificate */
            $this->__getSigner($this->ais_signature);
        }

        return(true);
    }

    /**
     * timestamp request
     * #params     string    digest to be signed
     * #params     string    digest method
     * @return     boolean   true on success, false on failure
     */
    public function timestamp($digestValue, $digestMethod) {
        $this->sig_cert = '';
        $this->sig_certSubject = '';
        $this->sig_certMIDSN = '';

        /* Define the base elements */
        $params = array(
            'SignRequest' => array(
                'RequestID' => $this->__createTransID(),
                'Profile' => 'http://ais.swisscom.ch/1.0',
                'OptionalInputs' => array(
                    'ClaimedIdentity' => array(
                        'Name' => $this->customerID
                    ),
                    'SignatureType' => 'urn:ietf:rfc:3161',
                    'AdditionalProfile' => 'urn:oasis:names:tc:dss:1.0:profiles:timestamping'
                ),
                'InputDocuments' => array(
                    'DocumentHash' => array(
                        'DigestMethod' => array('Algorithm' => $digestMethod),
                        'DigestValue' => $digestValue
        )   )   )   );

        /* Define the AddTimestamp element */
        $addTimestamp = array(
            'SignRequest' => array(
                'OptionalInputs' => array(
                    'AddTimestamp' => array('Type' => 'urn:ietf:rfc:3161')
        )   )   );
        if (isset($this->addTimestamp) && $this->addTimestamp)
            $params = array_merge_recursive($params, (array)$addTimestamp);

        /* Define the AddRevocationInformation element */
        $addRevocationInformation = array(
            'SignRequest' => array(
                'OptionalInputs' => array(
                    'AddRevocationInformation' => array('Type' => $this->revocationInformation)
        )   )   );
        if (isset($this->revocationInformation) && $this->revocationInformation !== '')
            $params = array_merge_recursive($params, (array)$addRevocationInformation);

        $this->client->__setLocation($this->base_url . '/AIS-Server/ws');
        $this->client->setParams($this->customerID, 'TSA', $this->revocationInformation, false);
        if (!$this->__doCall('sign', $params)) return(false);

        /* Get the signature and signer details */
        $ais_signature = '';
        if (isset($this->response->SignResponse->SignatureObject->Timestamp->RFC3161TimeStampToken)) {
            $ais_signature = (string)$this->response->SignResponse->SignatureObject->Timestamp->RFC3161TimeStampToken;

            /* Ensure proper base64 encoding */
            if (base64_decode($ais_signature, true)) $this->ais_signature = $ais_signature;
            else $this->ais_signature = base64_encode($ais_signature);

            /* Get the signer certificate */
            $this->__getSigner($this->ais_signature);
        }

        return(true);
    }

    /**
     * __createTransID - Creates a unique Transaction ID
     * @return     string
     */
    private function __createTransID() {
        $trans_id = 'AIS.PHP.'.rand(89999, 10000).'.'.rand(8999, 1000);
        
        return($trans_id);
    }

    /**
     * addRevocationInformation - Defines the type of revocation information to be added
     * #params     string    Type ('PADES', 'CADES', 'BOTH', '')
     */
    public function addRevocationInformation($revocation) {
        $type = strtoupper((string)$revocation);
        switch ($revocation) {
            case 'PADES':
                $this->revocationInformation = 'PADES';
                break;
            case 'CADES':
                $this->revocationInformation = 'CADES';
                break;
            case 'BOTH':
                $this->revocationInformation = 'BOTH';
                break;
            default:
                $this->revocationInformation = '';
        }
    }

    /**
     * addTimestamp - Defines if the time stamp should be added or not
     * #params     boolean
     */
    public function addTimestamp($timestamp) {
        $timestamp = (boolean)$timestamp;
        if ($timestamp) $this->addTimestamp = true;
                   else $this->addTimestamp = false;
    }

    /**
     * setBaseURL - Sets the base URL for the location of the service
     * #params     string    Base URL
     */
    public function setBaseURL($url) {
        $this->base_url = (string)$url;
    }

    /**
     * getLastRequest - Returns last request to Mobile ID service
     * @return     string
     */
    public function getLastRequest() {
        return($this->prettyXML($this->client->__getLastRequest()));
    }

    /**
     * getLastResponse - Returns last response from Mobile ID service
     * @return     string
     */
    public function getLastResponse() {
        return($this->prettyXML($this->client->__getLastResponse()));
    }

    /**
     * getLastSignature - Returns last signature response
     * @return     string
     */
    public function getLastSignature() {
        return($this->ais_signature);
    }

    /**
     * __getSigner - Retrieves the signer certifcate of the signature
     * #params     string    Base64 encoded signature
     * @return     boolean   
     */
    private function __getSigner($signature) {
        assert('is_string($signature)');

        /* Define temporary files */
        $tmpfile = tempnam(sys_get_temp_dir(), '_ais_');
        $file_sig      = $tmpfile . '.sig';
        $file_sig_cert = $tmpfile . '.crt';

        /* Chunk spliting the signature into temp PEM file */
        $pem = chunk_split($signature, 64, "\n");
        $pem = "-----BEGIN PKCS7-----\n".$pem."-----END PKCS7-----\n";
        file_put_contents($file_sig, $pem);

        /* Signature checks must explicitly succeed */
        $ok = false;

        /* Get the signer certificate(s) */
//TODO: change to PHP openssl_pkcs7_verify
        $output = exec("openssl pkcs7 -inform pem -in $file_sig -print_certs -out $file_sig_cert 2>&1");
        if (file_exists($file_sig_cert)) {
            /* Get the certificates and put them into an array */
            $certs_parse = file_get_contents($file_sig_cert);
            /* Keep content between ----BEGIN and ----END */
            $certs_parse = preg_replace('/.*?(--+[^-]+-+.*?--+[^-]+-+)/s', '$1', $certs_parse);
            /* Prepare the separator */
            $certs_parse = str_replace('----------', '-----####-----', $certs_parse);
            /* Explode the certs */
            $certs = explode('####', $certs_parse);
            unset($certs_parse);

            /* Find the signer certificate based on shortest validity time */
            $ref_validTo_time_t = PHP_INT_MAX;
            foreach ($certs as $cert) {
                $certificate = openssl_x509_parse($cert);
                if (isset($certificate['validTo_time_t'])) {
                    if ($certificate['validTo_time_t'] <= $ref_validTo_time_t) {
                        $ref_validTo_time_t = $certificate['validTo_time_t'];
                        $this->sig_cert = $cert;
            }   }   }
            $certificate = openssl_x509_parse($this->sig_cert);

            /* Get the signer name */
            $this->sig_certSubject = $certificate['name'];
            /* Parse the subjectAltName */
            $subjectAltName = '';
            if (isset($certificate['extensions']['subjectAltName'])) {
                $subjectAltName = $certificate['extensions']['subjectAltName'];
                // Format: 'DirName: serialNumber = ID-16981fa2-8998-4125-9a93-5fecbff74515, name = "+41798...", description = test.ch: Signer le document?, pseudonym = MIDCHEGU8GSH6K83'
                $subjectAltNameArray = explode(', ', $subjectAltName);
                foreach ($subjectAltNameArray as $value) {
                    if (preg_match("/pseudonym = (.*)/", $value, $match))
                        $this->sig_certMIDSN = $match[1];
                }
            };
            $ok = true;
        } else {
            trigger_error('ais::_getSigner: get the signer error ' . openssl_error_string(), E_USER_WARNING);
            $ok = false;
        }
       
        /* Cleanup of temporary files */ 
        if (file_exists($tmpfile))        unlink($tmpfile);
        if (file_exists($file_sig))       unlink($file_sig);
        if (file_exists($file_sig_cert))  unlink($file_sig_cert);

        return($ok);
    }

    /**
     * prettyXML - Returns pretty formated XML
     * #params     string    XML
     * @return     string
     */
    private function prettyXML($xml) {
        $dom = new DOMDocument;
        $dom->preserveWhiteSpace = FALSE;
        if (! $xml == null) {
            $dom->loadXML($xml);
            $dom->formatOutput = TRUE;
            return($dom->saveXml());
        }
        return("");
    }

}

?>
