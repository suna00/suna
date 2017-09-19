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

#### 4-1. Mnet - flow.초기

0. MySQL `LOAD DATA` 로 덤프 읽어서 레플리카에 밀어넣기
1. `MnetController.retrieveAll()` 실행 (`/migration/mnet/initialData/all` URL 호출)
2. `dbSyncProcess` 의 정보 수행
3. 레포트 작성

#### 4-2. Mnet - flow.스케쥴

0. 스케쥴러에서 특정 시간마다 `ScheduledMnetService.execute()` 수행
1. 레포트로부터 최종 마이그레이션 수행 시간 가져오기
2. `dbSyncProcess` 의 정보 수행
3. 레포트 작성


#### 5-1. PIP - flow.초기 (미구현)

0. `type=all` 로 프로그램 / 영상의 전체 정보 요청
1. 파일 저장
2. 파일 읽어서 노드 생성
3. 레포트 작성

#### 4-2. PIP - flow.스케쥴

0. 스케쥴러에서 특정 시간(아마도 60시간)마다 `ScheduledPipService.execute()` 수행
1. `type=recent&playform=mnetjapan` 으로 API 호출
2. JSON 정보로 노드 생성
3. 레포트 작성