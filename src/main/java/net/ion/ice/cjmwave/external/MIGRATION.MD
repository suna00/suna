### MIGRATION

#### 0. 대상

- Mnet : 앨범, 아티스트, 뮤직비디오, 곡, 차트
- PIP : 프로그램, 영상(클립 및 VOD)

#### 1. 연관 소스 정보 - Mnet

Mnet(MSSQL) to MySQL
```
노드명 : msSqlReplication
노드 경로 : /resources/schema/node/dbSyncMng/
노드 용도 : Mnet MSSQL 로부터 MySQL 로 신규 데이터를 복제
관련 Java Class : 
  - external.mnet.data.MnetDataDumpService.java
```

MySQL to Node
```
노드명 : dbSyncProcess / dbSyncMapper
노드 경로 : /resources/schema/node/dbSyncMng/
노드 용도 : 
  - dbSyncProcess : MySQL 쿼리 결과를 노드로 변환
  - dbSyncMapper : 쿼리 컬럼값 노드 PID 맵핑 정보. 정의되지 않으면 기본 칼럼명으로 맵핑.
관련 Java Class :
  - db.sync.DBSyncService.java
```


#### 2. 연관 소스 정보 - PIP

PIP API(JSON) to Node
```
노드명 : 별도 노드 없음
노드 경로 : 없음
노드 용도 : 없음
관련 Java Class :
  - external.pip.PipApiService
```

#### 3. 연관 소스 정보 - 기타

Mnet 이미지 URL 생성 규칙 
```
Class : external.utils.FileUtils
Method :
  retrieveRemoteFile : URL 로부터 파일 객체 반환
  getMnetFileUrl : Mnet 이미지 URL 반환
```

마이그레이션 결과 리포트 작성 
```
Class : external.utils.MigrationUtils
Method : 
  recordDataCopyRecord : MSSQL to MySQL 복제 결과 저장
  recordResult : MySQL to Node 결과 저장 
```
