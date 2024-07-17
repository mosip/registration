# Group2 stage

Helm chart for installing Registration Processor Group 2 stage.

## Install
```console
$ kubectl create namespace regproc
$ helm repo add mosip https://mosip.github.io
$ helm -n regproc install my-release mosip/regproc-group2
```

