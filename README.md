# ICE2 CORE 

String Boot를 기반으로 API를 제공하는 Backend 시스템  


## Table of Contents
1. [Features](#features)
1. [Requirements](#requirements)
1. [Getting Started](#getting-started)
1. [Application Structure](#application-structure)


## Features
* [spring-boot](https://projects.spring.io/spring-boot/)


## Requirements
* jdk `^1.8`

## Getting Started

다음과 같이 설치 및 실행합니다 :

### Install plugin

* Lombok 
1. IntelliJ > Preferences > Plugin > keyword in 'lombok' search and install and restart
2. Preferences > Compiler > Annotation Processors > Enable annotaion processing checked

### Running Server     

```bash
 - Intellij 우측 상단 Run/Debug Configuration 
 - Edit Configurations
 - Add New Configuration - > Select Spring Boot
 - Configuration#Tab
   -> Main Class : Ice2Application
   -> Use classpath of module : ice2-core_main
```