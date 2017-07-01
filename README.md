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

                    



### Install from source

First, clone the project:

```bash
$ git clone http://125.131.88.146/pm1200/ice2-core.git <my-project-name>
$ cd <my-project-name>
```

### Running Server


```bash
$ java -jar application.war --spring.profiles.active=dev
$ java -jar application.war --spring.profiles.active=dev-2
$ npm run dev -s      # Compile and launch 
```
실행한 이후에 http://localhost:8081/ 에서 동작을 확인합니다.

### Local Test Run

```bash
$ java -jar build/libs/application.war --spring.profiles.active=dev-2
```
`-jar` 파라미터가 빌드 결과를 볼 수 있도록 합니다.

