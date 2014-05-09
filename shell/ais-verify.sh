#!/bin/sh
# ais-verify.sh
#
# Script to verify the integrity of a file with a detached PKCS#7 signature.
#
# Dependencies: openssl, sed, awk
#
# License: GNU General Public License version 3 or later; see LICENSE.md
# Author: Swisscom (Schweiz) AG

# Error function
error()
{
  [ "$VERBOSE" = "1" -o "$DEBUG" = "1" ] && echo "$@" >&2
  exit 1
}

# Check command line
DEBUG=
VERBOSE=
while getopts "dv" opt; do                      # Parse the options
  case $opt in
    d) DEBUG=1 ;;                               # Debug
    v) VERBOSE=1 ;;                             # Verbose
  esac
done
shift $((OPTIND-1))                             # Remove the options

if [ $# -lt 2 ]; then                           # Parse the rest of the arguments
  echo "Usage: $0 <options> pkcs7"
  echo "  -v         - verbose output"
  echo "  -d         - debug mode"
  echo "  file       - file to verify"
  echo "  pkcs7      - file containing the detached PKCS#7 signature"
  echo
  echo "  Example $0 -v myfile.txt myfile.p7s"
  echo
  exit 1
fi

PWD=$(dirname $0)                               # Get the Path of the script

# Check the dependencies
for cmd in openssl sed awk grep; do
  hash $cmd &> /dev/null
  if [ $? -eq 1 ]; then error "Dependency error: '$cmd' not found" ; fi
done

SIG_CA=$PWD/ais-ca-signature.crt                # Signature CA certificate/chain file
TMP=$(mktemp -u /tmp/_tmp.XXXXXX)               # Temporary file

# File to verify and the results
FILE=$1                                         # File to verify
SIG=$2                                          # File containing the detached signature

# Check existence of needed files
[ -r "$FILE" ]   || error "File to verify ($FILE) missing or not readable"
[ -r "$SIG" ]    || error "Signature file ($SIG) missing or not readable"
[ -r "$SIG_CA" ] || error "CA certificate/chain file ($SIG_CA) missing or not readable"

# Convert the PKCS from PEM to DER
# as this format is supported for both verifications (CMS / TSA)
openssl pkcs7 -inform pem -in $SIG -out $TMP.sig.der -outform der > /dev/null 2>&1
[ -s "$TMP.sig.der" ] || error "Unable to convert PKCS7 in $SIG from PEM to DER"

# Verify the detached signature against original file: CMS
#  -noverify: don't verify signers certificate to avoid expired certificate error for OnDemand
#  stdout and stderr to a file as the -out will not contain the details about the verification itself
openssl cms -verify -inform der -in $TMP.sig.der -content $FILE -out $TMP.sig -CAfile $SIG_CA -noverify -purpose any 1> $TMP.cms.verify 2>&1
RC_CMS=$?                                       # Keep the related errorlevel

# Verify the detached signature against original file: TSA
#  -token_in: indicates that the input is a DER encoded time stamp token (ContentInfo) instead of a time stamp response
#  stdout and stderr to a file as the -out will not contain the details about the verification itself
openssl ts -verify -data $FILE -in $TMP.sig.der -token_in -CAfile $SIG_CA 1> $TMP.tsa.verify 2>&1
RC_TSA=$?                                       # Keep the related errorlevel

if [ "$RC_CMS" = "0" -o "$RC_TSA" = "0" ]; then # Any verification ok
  # Extract the certificates in the signature
  openssl pkcs7 -inform pem -in $SIG -out $TMP.certs.pem -print_certs > /dev/null 2>&1
  [ -s "$TMP.certs.pem" ] || error "Unable to extract the certificates in the signature"
  # Split the certificate list into separate files
  awk -v tmp=$TMP.certs.level -v c=-1 '/-----BEGIN CERTIFICATE-----/{inc=1;c++} inc {print > (tmp c ".pem")}/---END CERTIFICATE-----/{inc=0}' $TMP.certs.pem

  # Find the signers certificate level based on OCSP url presence
  RES_CERT=
  for i in $TMP.certs.level?.pem; do
    if [ -s "$i" ]; then
      CHECK=$(openssl x509 -in $i -ocsp_uri -noout)
      if [ -n "$CHECK" ]; then RES_CERT=$i; fi
    fi
  done
  [ -s "$RES_CERT" ] || error "Unable to find the signers certificate based on the OCSP URI"

  # Signers certificate
  RES_CERT_SUBJ=$(openssl x509 -subject -nameopt utf8 -nameopt sep_comma_plus -noout -in $RES_CERT)
  RES_CERT_ISSUER=$(openssl x509 -issuer -nameopt utf8 -nameopt sep_comma_plus -noout -in $RES_CERT)
  RES_CERT_START=$(openssl x509 -startdate -noout -in $RES_CERT)
  RES_CERT_END=$(openssl x509 -enddate -noout -in $RES_CERT)

  # Get OCSP uri from the signers certificate and verify the revocation status
  OCSP_URL=$(openssl x509 -in $RES_CERT -ocsp_uri -noout)
  # Find the proper issuer certificate in the list
  ISSUER=
  for i in $TMP.certs.level?.pem; do
    if [ -s "$i" ]; then
      RES_TMP=$(openssl x509 -subject -nameopt utf8 -nameopt sep_comma_plus -noout -in $i)
      RES_TMP=$(echo "$RES_TMP" | sed -e 's/subject= /issuer= /')
      if [ "$RES_TMP" = "$RES_CERT_ISSUER" ]; then ISSUER=$i; fi
    fi
  done

  # Verify the revocation status over OCSP
  # -no_cert_verify: don't verify the OCSP response signers certificate at all
  if [ -n "$OCSP_URL" -a -n "$ISSUER" ]; then
    openssl ocsp -CAfile $SIG_CA -issuer $ISSUER -nonce -out $TMP.certs.check -url $OCSP_URL -cert $RES_CERT -no_cert_verify > /dev/null 2>&1
    OCSP_ERR=$?                                   # Keep related errorlevel
    if [ "$OCSP_ERR" = "0" ]; then                # Revocation check completed
      RES_CERT_STATUS=$(sed -n -e 's/.*.certs.level.*: //p' $TMP.certs.check)
     else                                         # -> check not ok
      RES_CERT_STATUS="error, status $OCSP_ERR"   # Details for OCSP verification
    fi
   else
    RES_CERT_STATUS="No OCSP information found in the signers certificate"
  fi

  # Check for embedded elements by decoding the PKCS#7
  openssl cms -cmsout -print -noout -inform der -in $TMP.sig.der -out $TMP.pkcs7.dump

  # OCSP: CMS Advanced Electronic Signatures revocation-values
  # object: id-smime-aa-ets-revocationValues (1.2.840.113549.1.9.16.2.24)
  CHECK=$(grep 1.2.840.113549.1.9.16.2.24 $TMP.pkcs7.dump)
  if [ -n "$CHECK" ]; then
    EMBEDDED_OCSP_1="Yes"
   else
    EMBEDDED_OCSP_1="No"
  fi
  # OCSP: PDF signature certificate revocation information attribute
  # object: undefined (1.2.840.113583.1.1.8)
  CHECK=$(grep 1.2.840.113583.1.1.8 $TMP.pkcs7.dump)
  if [ -n "$CHECK" ]; then
    EMBEDDED_OCSP_2="Yes"
   else
    EMBEDDED_OCSP_2="No"
  fi
  # TSA: object: id-smime-aa-timeStampToken (1.2.840.113549.1.9.16.2.14)
  CHECK=$(grep 1.2.840.113549.1.9.16.2.14 $TMP.pkcs7.dump)
  if [ -n "$CHECK" ]; then
    EMBEDDED_TSA="Yes"
   else
    EMBEDDED_TSA="No"
  fi

  if [ "$VERBOSE" = "1" ]; then                   # Verbose details
    echo "OK on $SIG with following details:"
    echo " Signer       : $RES_CERT_SUBJ"
    echo "                $RES_CERT_ISSUER"
    echo "                validity= $RES_CERT_START $RES_CERT_END"
    echo "                OCSP check= $RES_CERT_STATUS"
    echo " Embedded OCSP: CMS (1.2.840.113549.1.9.16.2.24)= $EMBEDDED_OCSP_1"
    echo "                PDF (1.2.840.113583.1.1.8)= $EMBEDDED_OCSP_2"
    echo " Embedded TSA : $EMBEDDED_TSA"
  fi
 else                                             # -> verification failure
  if [ "$VERBOSE" = "1" ]; then                   # Verbose details
    echo "FAILED on $SIG with following details:"
    echo ">> CMS verification details <<"
    [ -f "$TMP.cms.verify" ] && cat $TMP.cms.verify
    echo ">> TSA verification details <<"
    [ -f "$TMP.tsa.verify" ] && cat $TMP.tsa.verify
  fi
fi

# Debug details
if [ -n "$DEBUG" ]; then
  [ -f "$TMP.cms.verify" ] && echo ">>> $TMP.cms.verify <<<" && cat $TMP.cms.verify
  [ -f "$TMP.tsa.verify" ] && echo ">>> $TMP.tsa.verify <<<" && cat $TMP.tsa.verify
  [ -f "$TMP.certs.check" ] && echo ">>> $TMP.certs.check <<<" && cat $TMP.certs.check
  echo ""
fi

# Cleanups if not DEBUG mode
if [ ! -n "$DEBUG" ]; then
  [ -f "$TMP" ] && rm $TMP
  [ -f "$TMP.certs.pem" ] && rm $TMP.certs.pem
  for i in $TMP.certs.level?.pem; do [ -f "$i" ] && rm $i; done
  [ -f "$TMP.certs.check" ] && rm $TMP.certs.check
  [ -f "$TMP.sig" ] && rm $TMP.sig
  [ -f "$TMP.sig.der" ] && rm $TMP.sig.der
  [ -f "$TMP.cms.verify" ] && rm $TMP.cms.verify
  [ -f "$TMP.tsa.verify" ] && rm $TMP.tsa.verify
  [ -f "$TMP.pkcs7.dump" ] && rm $TMP.pkcs7.dump
fi

exit $RC

#==========================================================
