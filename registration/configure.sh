#!/bin/bash

set -e

echo "Started with args"

client_version_env="$client_version_env" #We should pick this from the jar not as an argument.
crypto_key_env="$crypto_key_env" #key to encrypt the jar files
client_tpm_enabled="$tpm_enabled_env" #Not used as of now
client_certificate="$client_certificate_env" # Not used as of now
client_upgrade_server="$client_upgrade_server_env" #docker hosted url

echo "initalized variables"

#download the certificate at runtime from the certificate store
#wget -O mosip_cer.cer "${client_certificate_url}"

echo "mosip.reg.app.key=${crypto_key_env}" > mosip-application.properties
echo "mosip.reg.version=${client_version_env}" >> mosip-application.properties
echo "mosip.reg.client.url=${client_upgrade_server}/registration-client/" >> mosip-application.properties
echo "mosip.reg.healthcheck.url=${healthcheck_url_env}" >> mosip-application.properties
echo "mosip.reg.rollback.path=../BackUp" >> mosip-application.properties
echo "mosip.reg.cerpath=/cer/mosip_cer.cer" >> mosip-application.properties
echo "mosip.reg.dbpath=db/reg" >> mosip-application.properties
echo "mosip.reg.xml.file.url=${client_upgrade_server}/registration-client/maven-metadata.xml" >> mosip-application.properties
echo "mosip.reg.client.tpm.availability=${client_tpm_enabled}" >> mosip-application.properties

echo "created mosip-application.properties"

mkdir -p /sdkdependency

#unzip Jre to be bundled
unzip /registration-libs/resources/jre/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip
rm /registration-libs/resources/jre/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip
mv /zulu11.41.23-ca-fx-jre11.0.8-win_x64/* /registration-libs/resources/jre/
chmod -R a+x /registration-libs/resources/jre

/usr/local/openjdk-11/bin/java -cp /registration-libs/target/*:/registration-client/target/lib/* io.mosip.registration.cipher.ClientJarEncryption "/registration-client/target/registration-client-${client_version_env}.jar" "${crypto_key_env}" "${client_version_env}" "/registration-libs/target/" "/build_files/${client_certificate}" "/registration-libs/resources/db/reg" "/registration-client/target/registration-client-${client_version_env}.jar" "/registration-libs/resources/rxtx" "/registration-libs/resources/jre" "/registration-libs/resources/batch/run.bat" "/mosip-application.properties" "/sdkdependency"

echo "encryption completed"

cd /registration-client/target/
mv "mosip-sw-${client_version_env}.zip" reg-client.zip
mkdir -p /registration-client/target/bin
cp /registration-client/target/lib/mosip-client.jar /registration-client/target/bin/
cp /registration-client/target/lib/mosip-services.jar /registration-client/target/bin/
/usr/bin/zip -r reg-client.zip bin
/usr/bin/zip -r reg-client.zip lib
/usr/bin/zip reg-client.zip MANIFEST.MF

echo "setting up nginx static content"

mkdir -p /var/www/html/registration-client
mkdir -p /var/www/html/registration-client/${client_version_env}
mkdir -p /var/www/html/registration-client/${client_version_env}/lib

cp /registration-client/target/lib/* /var/www/html/registration-client/${client_version_env}/lib
cp /registration-client/target/MANIFEST.MF /var/www/html/registration-client/${client_version_env}/
cp /build_files/maven-metadata.xml /var/www/html/registration-client/
cp reg-client.zip /var/www/html/registration-client/${client_version_env}/

echo "setting up nginx static content - completed"

/usr/sbin/nginx -g "daemon off;"