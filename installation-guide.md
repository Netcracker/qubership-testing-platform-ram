# Qubership ATP-RAM Installation Guide

## This guide describes Qubership ATP-RAM installation process

### Introduction

The system consists of two services: *ATP RAM* and *ATP RAM Report Receiver*.

### System Requirements

| 	ATP RAM	  | Requests | Limits |
|-----------|----------|--------|
|CPU |	100m |	200m |
|RAM |	1Gi |	1Gi |

| 	ATP RAM	Report Receiver  | Requests | Limits |
|-----------|----------|--------|
|CPU | 	500m   | 	500m  |
|RAM | 	1Gi     | 	1Gi   |

| 	   | ATP RAM | 	ATP RAM Report Receiver |
|-----|---------|--------------------------|
|replicas| 	1      |	2 |

### Database System Requirements

| 	MongoDB	  | Requests | Limits |
|-----------|----------|--------|
|CPU |	100m | 	100m |
|RAM |	1Gi | 	1Gi   |

| 	GridFS  | Requests | Limits |
|-----------|----------|--------|
|CPU | 	100m    | 	100m  |
|RAM | 	512Mi     | 	512Mi   |


### Repositories
* ATP RAM	https://github.com/Netcracker/qubership-testing-platform-ram
* ATP RAM Report receiver	[Service repository isn't public yet]

