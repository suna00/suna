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

### 주요 기능     
* 컨텐츠 모델링
    * 노드타입/프로퍼티타입 설정을 통한 모델링
    * 노드타입
        * 저장소 유형 (repositoryType)
            * 노드(node) : 내부 저장소 기능 지원 (Infinispan)
            * 데이터(data) : 외부 저장소(RDB) 이용 (Oracle, MS-SQL, MySQL, Maria)
            * 빅데이터(bigData) : 외부 NoSQL 이용(Hbase)
            * 인메모리(InMemory) : 분산 인메모리 저장소 이용(Hazelcast)
            * 패스(path) : 노드타입 설정에 대한 트리 경로 
        * eviction :  저장소 유형이 Node인 경우 메모리 로딩 최대 값
    * 프로퍼티타입(propertyType) 
        * Value Type
            * 기본형 : STRING, DATE, INT, LONG, DOUBLE
            * TEXT : CLOB와 같이 대량의 문자열 저장
            * FILE : {“contentType”: “jpeg”, “storePath” : “tid/pid/201707/11/daidafeildiadag.jpg”, “fileName”: “test.jpg”, “fileSize” : 233}
            * CODE : code 속성에 저장된 값을 이용하여 값을 지정 {“value” : “value”, “label” : “label”}
            * REFERENCE : 참조 유형의 id 값을 저장
            * REFERENCED : 자신을 참조하고 있는 노드 목록을 리턴
        * required : 필수 여부
        * idable : 아이디 여부
        * labelable : Label 여부
        * treeable : 트리형태 지원 여부
        * indexable : 인덱싱 여부
        * analyzer : 인덱싱을 하는 경우 적용할 Analyzer 지정
            * simple : Lowercase 만 적용
            * standard : Lowercase와 Stop필터 적용
            * code : Lowercase 적용하고, 공백이나 ‘,’ 문자로 토큰
            * cjk : Lowercase, CJKBigram, StopFilter 적용
        * referenceType : valueType이 REFERENCE 또는 REFERENCED일 경우 참조할 노드 타입 지정
        * referenceValue : 참조 노드 타입의 프로퍼티 타입 지정
        * code : CODE 형에서 사용할 코드 값
        * validationTypes : Validation 체크 값
        * fileHandler : 파일 저장에 사용할 저장소 지정
* 이벤트
    * 노드에 발생하는 모든 트랜잭션을 관리하며, 관리 화면에서의 버튼에 대응
    * 기본 이벤트 : create, update, delete, allEvent
    * 추가 이벤트 정의 가능
    * EventAction : 해당 이벤트 발생시에 실제 실행되는 프로세스
        * action : 액션 아이디 
        * actionType : 액션 유형
            * service : actionBody에 서비스.메소드 형태로 저장하며, @Service("eventService”) 로 등록된 Annotation의 명칭과 해당 서비스에서 executeContext를 변수로 받는 메소드를 찾아서 실행
            * update : Insert, Update, Delete 등의 SQL 실행
            * select : Select SQL 실행
            * call : DB의 function이나 procedure 실행
            * function : 캐시 싱크 등과 같이 미리 정의된 기능 호출
        * actionBody : 액션 유형별로 호출 명령어나 SQL을 저장
            * {{:abc}} 형태로 파라미터 지정
            * @{abc} 형태로 sql 파라미터 지정 
    * EventListener : 이벤트 발생시에 비동기적으로 실행되는 프로세스
        * tid : 이벤트를 구독할 노드 타입
* 검색
    * Node - 인덱싱을 통한 루씬 검색 지원
        * 컨텐츠 유형별 상세 검색
            * Matching, Widcard, Below, Above 등 검색 기능 제공
            * 다중 Sorting 지원 : sorting=title asc, name desc
            * 검색 결과 그룹핑 기능 지원
        * 다국어 검색 지원
        * 통합 검색 지원
    * Data - SQL 동적 생성하여 검색 지원
    * 다중 컨텐츠 유형간의 검색 기능 지원
        * Nested Search 기능 지원
        * Has Child 검색 기능 지원
* Asset 관리
    * 이미지 관리
        * 이미지 포맷별 메타데이터 추출 기능
        * 이미지 변환 (포맷, 사이즈 등)
        * 이미지 편집 (온라인 편집 지원)
    * 오디오 관리
        * 오디오 변환 (포맷, 사이즈 등)
    * 비디오 관리
        * 비디오 변환 (포맷, 사이즈 등)
        * 썸네일 이미지 추출
        * 비디오 스크립트 추출 (자막 추출)
    * 문서 관리
        * 오피스 문서에 대한 PDF 변환 기능 지원
        * 오피스 문서에 대한 검색 기능 지원
* 클러스터 기능
    * 분산 In-Memory 관리
        * Distributed Map (복제 지원)
        * Replicated Map (동일 데이터 저장)
    * 분산 관리 기능
        * Atomic Long (분산 시퀀스)
        * Lock 
        * Queue (분산 큐)
        * Topic (분산 메시징)
* 부가  기능
    * 권한
        * ROLE  기반 권한 관리 기능 제공
            * 관리, 컨텐츠 작성, 검수 등 필요한 ROLE을 정의
            * 사용자 또는 사용자 그룹에 ROLE 부여(다중 부여 가능)
            * 메뉴, 컨텐츠 유형, 버튼, 이벤트 별 권한 부여(이용가능 ROLE 매핑)하여 권한 부여
        * 컨텐츠별 권한 관리 기능 제공
            * 작성자 기준 RULE 베이스 권한 부여
            * Ex) 작성자 및 관리자는 조회/수정/삭제 가능, 작성자 소속 사용자 그룹은 조회/수정 가능, 그외 사용자는 조회만 가능 등
    * 히스토리관리
        * Node 수정 시마다 버전 생성 및 저장
        * 이전 버전과 비교하여 변경된 속성(Property)만 선별하여 저장
        * 이를 이용하여 속성별 변경 이력 추출 가능
    * 다국어 지원
        * 속성별 다국어 추가 저장 기능 지원
        * 사용자 또는 클라이언트 로케일 설정에 따라서 해당 언어 또는 기본 설정언어(영어) 값 리턴
            * ex) {"title" : "Title"} or {"title" : "제목"}
        * 등록된 전체 다국어 리턴
            * ex) {"title" : {"en" : "Title", "ko" : "제목"}}
    * 워크플로우 관리
        * 에셋 변환 워크플로우 관리 
            * 비디오, 이미지 등 파일에 대한 입수/변환 등의 워크플로우 설정
        * 승인룰 관리
            * 컨텐츠에 대한 승인요청/검수/승인/반려 등의 승인룰 설정 지원
* 입수/변환/배포 기능
    * 입수 및 변환 관리
        * 입수처 관리 : 입수 대상 시스템 관리 - FTP, SFTP, API, 핫폴더 지원
        * 입수 스케줄 관리 : 입수 대상 시스템 연동 스케줄 관리
        * 입수 변환 관리 : 입수 대상별 입수 데이터에 대한 변환 기능 제공
            * 입수 대상 포맷 : JSON, XML, CSV 지원
            * ID 매핑 -> 필드 매핑 -> 코드 매핑 -> 포맷팅 단계로 변환 수행
    * 배포 관리
        * 배포처 관리 : 배포 대상 시스템 관리 
        * 배포 스케쥴 관리
        * 배포 변환 관리 : 배포 대상 포맷별 템플릿 기반의 배포 관리 기능 지원
* 통계 기능
    * 생산 통계 
        * 저작자별, 소속그룹별, 컨텐츠 유형별 생산 통계
        * 시간대별, 일별, 주별, 월별 통계
    * 이용 통계
        * 이용자별, 소속그룹별, 컨텐츠 유형별 이용 통계
        * 시간대별, 일별, 주별, 월별 통계
* 로그 분석

### 스키마 리로드
     개발 중 스키마 json을 변경하고, 즉시 적용하기 위해서 /helper/reloadSchema.json?filePath= 호출 가능
     ex) http://localhost:8080/helper/reloadSchema.json?filePath=/Users/jaeho/IdeaProjects/ice2-core/src/main/resources/schema/node
     