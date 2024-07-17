# Camel

Helm chart for installing Registration Processor Camel stage. 

## Install
```console
$ kubectl create namespace regproc
$ helm repo add mosip https://mosip.github.io
$ helm -n regproc install my-release mosip/regproc-camel
```

