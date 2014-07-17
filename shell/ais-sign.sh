#!/bin/sh
# ais-sign.sh
#
# Script that will produce a detached signature for a file by using
# curl to invoke the Swisscom All-in signing service (AIS).
#
# Dependencies: curl, openssl, sed, date, xmllint, tr, python
#
# License: GNU General Public License version 3 or later; see LICENSE.md
# Author: Swisscom (Schweiz) AG

######################################################################
# User configurable options
######################################################################

# CUSTOMER used to identify to AIS (provided by Swisscom)
CUSTOMER="IAM-Test"
KEY_STATIC="kp1-iam-signer"
KEY_ONDEMAND="OnDemand-Advanced"

# Swisscom AIS credentials
CERT_FILE=$PWD/mycert.crt                       # The certificate that is allowed to access the service
CERT_KEY=$PWD/mycert.key                        # The related key of the certificate


######################################################################
# There should be no need to change anything below
######################################################################

# Error function
error()
{
  [ "$VERBOSE" = "1" -o "$DEBUG" = "1" ] && echo "$@" >&2
  exit 1
}

# Check command line
MSGTYPE=SOAP                                    # Default is SOAP
DEBUG=
VERBOSE=
while getopts "dvt:" opt; do
  case $opt in
    t) MSGTYPE=$OPTARG ;;
    d) DEBUG=1 ;;
    v) VERBOSE=1 ;; 
  esac
done

shift $((OPTIND-1))                             # Remove the options

if [ $# -lt 3 ]; then                           # Parse the rest of the arguments
  echo "Usage: $0 <options> file method pkcs7 [dn] [[msisdn]] [[msg]] [[lang]]"
  echo "  -t value   - message type (SOAP, XML, JSON), default SOAP"
  echo "  -v         - verbose output"
  echo "  -d         - debug mode"
  echo "  file       - file to be signed"
  echo "  method     - digest method (SHA256, SHA384, SHA512)"
  echo "  pkcs7      - output file with detached PKCS#7 (Cryptographic Message Syntax) signature"
  echo "  [dn]       - optional distinguished name for on-demand certificate signing"
  echo "  [[msisdn]] - optional Mobile ID authentication when [dn] is present"
  echo "  [[msg]]    - optional Mobile ID message when [dn] is present"
  echo "  [[lang]]   - optional Mobile ID language (en, de, fr, it) when [dn] is present"
  echo
  echo "  Example $0 -v myfile.txt SHA256 myfile.p7s"
  echo "          $0 -v myfile.txt SHA256 myfile.p7s 'cn=Hans Muster,o=ACME,c=CH'"
  echo "          $0 -v -t JSON myfile.txt SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH'"
  echo "          $0 -v myfile.txt SHA256 myfile.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350"
  echo "          $0 -v myfile.txt SHA256 myfile.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'myserver.com: Sign?' en"
  echo 
  exit 1
fi

PWD=$(dirname $0)                               # Get the Path of the script

# Check the dependencies
for cmd in curl openssl sed date xmllint tr python; do
  hash $cmd &> /dev/null
  if [ $? -eq 1 ]; then error "Dependency error: '$cmd' not found" ; fi
done

# CA certificate file (in PEM format) for curl to verify the peer
SSL_CA=$PWD/ais-ca-ssl.crt

# Create temporary request
INSTANT=$(date +%Y-%m-%dT%H:%M:%S.%N%:z)        # Define instant and request id
REQUESTID=AIS.TEST.$INSTANT
TMP=$(mktemp -u /tmp/_tmp.XXXXXX)               # Request goes here
TIMEOUT_CON=90                                  # Timeout of the client connection

# File to be signed
FILE=$1
[ -r "${FILE}" ] || error "File to be signed ($FILE) missing or not readable"

# Digest method to be used
DIGEST_METHOD=$2
case "$DIGEST_METHOD" in
  SHA256)
    DIGEST_ALGO='http://www.w3.org/2001/04/xmlenc#sha256' ;;
  SHA384)
    DIGEST_ALGO='http://www.w3.org/2001/04/xmldsig-more#sha384' ;;
  SHA512)
    DIGEST_ALGO='http://www.w3.org/2001/04/xmlenc#sha512' ;;
  *)
    error "Unsupported digest method $DIGEST_METHOD, check with $0" ;;
esac

# Calculate the hash to be signed
DIGEST_VALUE=$(openssl dgst -binary -$DIGEST_METHOD $FILE | openssl enc -base64 -A)

# Target file
PKCS7_RESULT=$3
[ -f "$PKCS7_RESULT" ] && error "Target file $PKCS7_RESULT already exists"

# OnDemand subject distinguished name
ONDEMAND_DN=$4

# Set Claimed ID (customer name + key entity)
[ -n "$ONDEMAND_DN" ] && CLAIMED_ID=$CUSTOMER:$KEY_ONDEMAND || CLAIMED_ID=$CUSTOMER:$KEY_STATIC

# Optional step up authentication with Mobile ID
MID=""                                          # MobileID step up by default off
MID_MSISDN=$5                                   # MSISDN
MID_MSG=$6                                      # Optional DTBS
[ ! -n "$MID_MSG" ] && MID_MSG="Sign it?"
MID_LANG=$7                                     # Optional Language
[ ! -n "$MID_LANG" ] && MID_LANG="EN"
if [ -n "$MID_MSISDN" ]; then                   # MobileID step up?
  case "$MSGTYPE" in
    SOAP|XML) 
      MID='
        <sc:StepUpAuthorisation>
            <sc:MobileID Type="http://ais.swisscom.ch/1.0/auth/mobileid/1.0">
                <sc:MSISDN>'$MID_MSISDN'</sc:MSISDN>
                <sc:Message>'$MID_MSG'</sc:Message>
                <sc:Language>'$MID_LANG'</sc:Language>
            </sc:MobileID>
        </sc:StepUpAuthorisation>' ;;
    JSON) 
      MID='
        "sc.StepUpAuthorisation": {
            "sc.MobileID": {
                "@Type": "http://ais.swisscom.ch/1.0/auth/mobileid/1.0",
                "sc.MSISDN": "'$MID_MSISDN'",
                "sc.Message": "'$MID_MSG'",
                "sc.Language": "'$MID_LANG'"
            }
         },' ;;
  esac
fi

# Optional On-Demand certificate signature
if [ -n "$ONDEMAND_DN" ]; then
  case "$MSGTYPE" in
    SOAP|XML) 
      ONDEMAND='
        <AdditionalProfile>http://ais.swisscom.ch/1.0/profiles/ondemandcertificate</AdditionalProfile>
        <sc:CertificateRequest>
            '$MID'
            <sc:DistinguishedName>'$ONDEMAND_DN'</sc:DistinguishedName>
        </sc:CertificateRequest>' ;;
    JSON) 
      ONDEMAND='
        "AdditionalProfile": "http://ais.swisscom.ch/1.0/profiles/ondemandcertificate", 
        "sc.CertificateRequest": {
            '$MID'
            "sc.DistinguishedName": "'$ONDEMAND_DN'" 
        },' ;;
  esac
fi

case "$MSGTYPE" in
  # MessageType is SOAP. Define the Request
  SOAP)
    REQ_SOAP='
      <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"                  
                     xmlns:ais="http://service.ais.swisscom.com/">
          <soap:Body>
              <ais:sign>
                  <SignRequest RequestID="'$REQUESTID'" Profile="http://ais.swisscom.ch/1.0"
                               xmlns="urn:oasis:names:tc:dss:1.0:core:schema"
                               xmlns:dsig="http://www.w3.org/2000/09/xmldsig#"   
                               xmlns:sc="http://ais.swisscom.ch/1.0/schema">
                      <OptionalInputs>
                          <ClaimedIdentity>
                              <Name>'$CLAIMED_ID'</Name>
                          </ClaimedIdentity>
                          <SignatureType>urn:ietf:rfc:3369</SignatureType>
                          '$ONDEMAND'
                          <AddTimestamp Type="urn:ietf:rfc:3161"/>
                          <sc:AddRevocationInformation Type="BOTH"/>
                      </OptionalInputs>
                      <InputDocuments>
                          <DocumentHash>
                              <dsig:DigestMethod Algorithm="'$DIGEST_ALGO'"/>
                              <dsig:DigestValue>'$DIGEST_VALUE'</dsig:DigestValue>
                          </DocumentHash>
                      </InputDocuments>
                  </SignRequest>
              </ais:sign>
          </soap:Body>
      </soap:Envelope>'
    # store into file
    echo "$REQ_SOAP" > $TMP.req ;;

  # MessageType is XML. Define the Request
  XML)
    REQ_XML='
      <SignRequest RequestID="'$REQUESTID'" Profile="http://ais.swisscom.ch/1.0" 
                   xmlns="urn:oasis:names:tc:dss:1.0:core:schema"
                   xmlns:dsig="http://www.w3.org/2000/09/xmldsig#"   
                   xmlns:sc="http://ais.swisscom.ch/1.0/schema">
          <OptionalInputs>
              <ClaimedIdentity>
                  <Name>'$CLAIMED_ID'</Name>
              </ClaimedIdentity>
              <SignatureType>urn:ietf:rfc:3369</SignatureType>
              '$ONDEMAND'
              <AddTimestamp Type="urn:ietf:rfc:3161"/>
              <sc:AddRevocationInformation Type="BOTH"/>
          </OptionalInputs>
          <InputDocuments>
              <DocumentHash>
                  <dsig:DigestMethod Algorithm="'$DIGEST_ALGO'"/>
                  <dsig:DigestValue>'$DIGEST_VALUE'</dsig:DigestValue>
              </DocumentHash>
          </InputDocuments>
      </SignRequest>'
    # store into file
    echo "$REQ_XML" > $TMP.req ;;
    
  # MessageType is JSON. Define the Request
  JSON)
    REQ_JSON='{
      "SignRequest": {
          "@RequestID": "'$REQUESTID'",
          "@Profile": "http://ais.swisscom.ch/1.0",
          "OptionalInputs": {
              "ClaimedIdentity": {
                  "Name": "'$CLAIMED_ID'"
              },
              "SignatureType": "urn:ietf:rfc:3369",
              '$ONDEMAND'
              "AddTimestamp": {"@Type": "urn:ietf:rfc:3161"},
              "sc.AddRevocationInformation": {"@Type": "BOTH"}
          },
          "InputDocuments": {"DocumentHash": {
              "dsig.DigestMethod": {"@Algorithm": "'$DIGEST_ALGO'"},
              "dsig.DigestValue": "'$DIGEST_VALUE'" 
          }}
      }}'
    # store into file
    echo "$REQ_JSON" > $TMP.req ;;
    
  # Unknown message type
  *)
    error "Unsupported message type $MSGTYPE, check with $0" ;;
    
esac

# Check existence of needed files
[ -r "${SSL_CA}" ]    || error "CA certificate/chain file ($CERT_CA) missing or not readable"
[ -r "${CERT_KEY}" ]  || error "SSL key file ($CERT_KEY) missing or not readable"
[ -r "${CERT_FILE}" ] || error "SSL certificate file ($CERT_FILE) missing or not readable"

# Define cURL Options according to Message Type
case "$MSGTYPE" in
  SOAP)
    URL=https://ais.swisscom.com/AIS-Server/ws
    HEADER_ACCEPT="Accept: application/xml"
    HEADER_CONTENT_TYPE="Content-Type: text/xml;charset=utf-8"
    CURL_OPTIONS="--data" ;;
  XML)
    URL=https://ais.swisscom.com/AIS-Server/rs/v1.0/sign
    HEADER_ACCEPT="Accept: application/xml"
    HEADER_CONTENT_TYPE="Content-Type: application/xml;charset=utf-8"
    CURL_OPTIONS="--request POST --data" ;;
  JSON)
    URL=https://ais.swisscom.com/AIS-Server/rs/v1.0/sign
    HEADER_ACCEPT="Accept: application/json"
    HEADER_CONTENT_TYPE="Content-Type: application/json;charset=utf-8"
    CURL_OPTIONS="--request POST --data-binary" ;;
esac

# Call the service
http_code=$(curl --write-out '%{http_code}\n' --silent \
  $CURL_OPTIONS @$TMP.req \
  --header "${HEADER_ACCEPT}" --header "${HEADER_CONTENT_TYPE}" \
  --cert $CERT_FILE --cacert $SSL_CA --key $CERT_KEY \
  --output $TMP.rsp --trace-ascii $TMP.curl.log \
  --connect-timeout $TIMEOUT_CON \
  $URL)

# Results
RC=$?

if [ "$RC" = "0" -a "$http_code" = "200" ]; then
  case "$MSGTYPE" in
    SOAP|XML)
      # SOAP/XML Parse Result
      RES_MAJ=$(sed -n -e 's/.*<ResultMajor>\(.*\)<\/ResultMajor>.*/\1/p' $TMP.rsp)
      RES_MIN=$(sed -n -e 's/.*<ResultMinor>\(.*\)<\/ResultMinor>.*/\1/p' $TMP.rsp)
      RES_MSG=$(cat $TMP.rsp | tr '\n' ' ' | sed -n -e 's/.*<ResultMessage.*>\(.*\)<\/ResultMessage>.*/\1/p')
      sed -n -e 's/.*<Base64Signature.*>\(.*\)<\/Base64Signature>.*/\1/p' $TMP.rsp > $TMP.sig.base64 ;;
    JSON)
      # JSON Parse Result
      RES_MAJ=$(sed -n -e 's/^.*"ResultMajor":"\([^"]*\)".*$/\1/p' $TMP.rsp)
      RES_MIN=$(sed -n -e 's/^.*"ResultMinor":"\([^"]*\)".*$/\1/p' $TMP.rsp)
      RES_MSG=$(cat $TMP.rsp | sed 's/\\\//\//g' | sed 's/\\n/ /g' | sed -n -e 's/^.*"ResultMessage":{\([^}]*\)}.*$/\1/p')
      sed -n -e 's/^.*"Base64Signature":{"@Type":"urn:ietf:rfc:3369","$":"\([^"]*\)".*$/\1/p' $TMP.rsp | sed 's/\\//g' > $TMP.sig.base64 ;;
  esac 

  if [ -s "${TMP}.sig.base64" ]; then
    # Decode signature if present
    openssl enc -base64 -d -A -in $TMP.sig.base64 -out $TMP.sig.der
    [ -s "${TMP}.sig.der" ] || error "Unable to decode Base64Signature"
    # Save PKCS7 content to target
    openssl pkcs7 -inform der -in $TMP.sig.der -out $PKCS7_RESULT
  fi

  # Status and results
  if [ "$RES_MAJ" = "urn:oasis:names:tc:dss:1.0:resultmajor:Success" ]; then
    RC=0                                                # Ok
    if [ "$VERBOSE" = "1" ]; then                       # Verbose details
      echo "OK on $FILE with following details:"
      echo " Digest       : $DIGEST_VALUE"
      echo " Result major : $RES_MAJ with exit $RC"
    fi
   else
    RC=1                                                # Failure
    if [ "$VERBOSE" = "1" ]; then                       # Verbose details
      echo "FAILED on $FILE with following details:"
      echo " Result major   : $RES_MAJ with exit $RC"
      echo " Result minor   : $RES_MIN"
      echo " Result message : $RES_MSG"
    fi
  fi

 else                                                 # -> error in curl call
  CURL_ERR=$RC                                          # Keep related error
  RC=2                                                  # Force returned error code
  if [ "$VERBOSE" = "1" ]; then                         # Verbose details
    echo "FAILED on $FILE with following details:"
    echo " curl error : $CURL_ERR"
    echo " http error : $http_code"
  fi
fi

# Debug details
if [ -n "$DEBUG" ]; then
  [ -f "$TMP.req" ] && echo ">>> $TMP.req <<<" && cat $TMP.req | ( [ "$MSGTYPE" != "JSON" ] && xmllint --format - || python -m json.tool )
  [ -f "$TMP.curl.log" ] && echo ">>> $TMP.curl.log <<<" && cat $TMP.curl.log | grep '==\|error'
  [ -f "$TMP.rsp" ] && echo ">>> $TMP.rsp <<<" && cat $TMP.rsp | ( [ "$MSGTYPE" != "JSON" ] && xmllint --format - || python -m json.tool ) 
  echo ""
fi

# Cleanups if not DEBUG mode
if [ ! -n "$DEBUG" ]; then
  [ -f "$TMP.req" ] && rm $TMP.req
  [ -f "$TMP.curl.log" ] && rm $TMP.curl.log
  [ -f "$TMP.rsp" ] && rm $TMP.rsp
  [ -f "$TMP.sig.base64" ] && rm $TMP.sig.base64
  [ -f "$TMP.sig.der" ] && rm $TMP.sig.der
fi

exit $RC

#==========================================================
