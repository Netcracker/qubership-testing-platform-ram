# Qubership Testing Platform RAM Service Installation Guide

## This guide describes RAM Service installation process

### Introduction

The system consists of two services: *RAM* and *RAM Report Receiver*.

### System Requirements

| RAM   | Requests | Limits |
|-------|---------:|-------:|
| CPU   | 100m     | 200m   |
| RAM   | 1Gi      | 1Gi    |

| RAM Report Receiver | Requests | Limits |
|---------------------|---------:|-------:|
| CPU                 | 500m     | 500m   |
| RAM                 | 1Gi      | 1Gi    |

|            |  RAM | RAM Report Receiver |
|------------|-----:|--------------------:|
| replicas   |    1 | 2                   |

### Database System Requirements

| MongoDB | Requests | Limits |
|---------|----------|--------|
| CPU     | 100m     | 100m   |
| RAM     | 1Gi      | 1Gi    |

| GridFS | Requests | Limits  |
|--------|----------|---------|
| CPU    | 100m     | 100m    |
| RAM    | 512Mi    | 512Mi   |


### Repositories
* RAM [GitHub Repository](https://github.com/Netcracker/qubership-testing-platform-ram)
* RAM Report receiver [Service repository isn't public yet]
