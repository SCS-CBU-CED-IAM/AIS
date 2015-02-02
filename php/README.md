php-ais
=======

Contains a PHP class for the All-in Signing service to invoke:

* Signature Request (Static / OnDemand)
* Timestamp Request

## Dependencies

The class is using:

* SoapClient class (http://www.php.net/manual/en/class.soapclient.php) in the WSDL mode
* OpenSSL package and class (http://www.php.net/manual/en/openssl.requirements.php)
* aisService.wsdl

## Client based certificate authentication

The file that must be specified in the initialisation refers to the local_cert and must contain both certificates, privateKey and publicKey in the same file (`cat mycert.crt mycert.key > mycertandkey.crt`).

Example of content:
````
-----BEGIN PRIVATE KEY-----
...
-----END PRIVATE KEY-----
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
````


## Connection options

Proxy support by passing additional SoapClient options.

````
$myoptions = array(
    'proxy_host'     => "localhost",
    'proxy_port'     => 8080,
    'proxy_login'    => "some_name",
    'proxy_password' => "some_password"
);
$ais = AllinSigningService($customerID, $certandkey, $ca_ssl, $myoptions);
````

Refer to the SoapClient::SoapClient options on http://www.php.net/manual/en/soapclient.soapclient.php

## Usage

Sample use of the class:

````
<?php

require_once dirname(__FILE__) . '/ais.php';

error_reporting(E_ALL);

/* Disable SOAP WSDL caching */
ini_set("soap.wsdl_cache_enabled", "0");

/* Environment */
$certandkey          = dirname(__FILE__) . '/mycertandkey.crt';
$ca_ssl              = dirname(__FILE__) . '/ais-ca-ssl.crt';
$customerID          = 'IAM-Test';
$customerID_static   = $customerID . ':kp1-iam-signer';
$customerID_ondemand = $customerID . ':OnDemand-Advanced';

/* Value to be signed */
$digestValue = hash_file('sha256', 'sample.pdf', true);
$digestMethod = 'http://www.w3.org/2001/04/xmlenc#sha256';

/* Signatures */
echo "========= AllinSigningService:TSA ==========" . PHP_EOL;
echo 'DigestValue: ' . base64_encode($digestValue) . PHP_EOL;
echo 'DigestMethod: ' . $digestMethod . PHP_EOL;
$ais = new AllinSigningService($customerID, $certandkey, $ca_ssl);
$ais->addRevocationInformation('BOTH');

if ($ais->timestamp($digestValue, $digestMethod)) {
  echo "========= AllinSigningService:RESULT ==========" . PHP_EOL;
  echo 'Signed by: ' . $ais->sig_certSubject . PHP_EOL;
  echo 'Signature: ' . $ais->getLastSignature() . PHP_EOL;
}
echo "========= AllinSigningService:STATUS =========" . PHP_EOL;
echo 'ResultMajor:   ' . $ais->resultmajor . PHP_EOL;
echo 'ResultMinor:   ' . $ais->resultminor . PHP_EOL;
echo 'ResultMessage: ' . $ais->resultmessage . PHP_EOL;
unset($ais);


echo PHP_EOL . PHP_EOL;
echo "========= AllinSigningService:Static ==========" . PHP_EOL;
echo 'DigestValue: ' . base64_encode($digestValue) . PHP_EOL;
echo 'DigestMethod: ' . $digestMethod . PHP_EOL;
$ais = new AllinSigningService($customerID_static, $certandkey, $ca_ssl);
$ais->addRevocationInformation('BOTH');
$ais->addTimestamp(true);

if ($ais->sign($digestValue, $digestMethod)) {
  echo "========= AllinSigningService:RESULT ==========" . PHP_EOL;
  echo 'Signed by: ' . $ais->sig_certSubject . PHP_EOL;
  echo 'Signature: ' . $ais->getLastSignature() . PHP_EOL;
}
echo "========= AllinSigningService:STATUS =========" . PHP_EOL;
echo 'ResultMajor:   ' . $ais->resultmajor . PHP_EOL;
echo 'ResultMinor:   ' . $ais->resultminor . PHP_EOL;
echo 'ResultMessage: ' . $ais->resultmessage . PHP_EOL;
unset($ais);


echo PHP_EOL . PHP_EOL;
echo "========= AllinSigningService:OnDemand ==========" . PHP_EOL;
echo 'DigestValue: ' . base64_encode($digestValue) . PHP_EOL;
echo 'DigestMethod: ' . $digestMethod . PHP_EOL;
$ais = new AllinSigningService($customerID_ondemand, $certandkey, $ca_ssl);
$ais->addRevocationInformation('BOTH');
$ais->addTimestamp(true);

if ($ais->sign($digestValue, $digestMethod, 'cn=hans.muster@acme.ch,c=ch', '+41791234567', 'acme.ch: Sign the document Sample.pdf?', 'en')) {
//if ($ais->sign($digestValue, $digestMethod, 'cn=hans.muster@acme.ch,c=ch', '+41791234567', 'acme.ch: Sign the document Sample.pdf?', 'en', 'MIDCHEGU8GSH6K83')) {
  echo "========= AllinSigningService:RESULT ==========" . PHP_EOL;
  echo 'Signed by: ' . $ais->sig_certSubject . PHP_EOL;
  echo 'SN of DN: ' . $ais->sig_certMIDSN . PHP_EOL;
  echo 'Signature: ' . $ais->getLastSignature() . PHP_EOL;
}
echo "========= AllinSigningService:STATUS =========" . PHP_EOL;
echo 'ResultMajor:   ' . $ais->resultmajor . PHP_EOL;
echo 'ResultMinor:   ' . $ais->resultminor . PHP_EOL;
echo 'ResultMessage: ' . $ais->resultmessage . PHP_EOL;
unset($ais);
````
