### 00. MNET 초기 마이그레이션

아래 리스트 순차대로 실행합니다

소스 플로우 :
0. `MnetController.java` :: URL 기반 실행 
1. `DBSyncService.java` :: dbSyncProcess 노드 참조해서 쿼리 실행 결과로 노드 생성
2. `MigrationUtils.java` :: MIG_HISTORY 에 마이그레이션 레포트 제출

특이 :
- 원격에 이미지가 없어도(FileNotFoundException) 노드 생성

[[ 실행 순서 ]]

0. CSV 를 MYSQL 로 옮기기 : `/net/ion/ice/cjmwave/external/mssql_load_data.sql`

1. 트리거 실행 : `{{Base URL}}/migration/mnet/initialData/{{ all / artist / album / song / mv / chart }}`

2. `MIG_HISTORY` / `NODE_CREATION_FAIL` 테이블에 리포트 작성

### 01. MNET 주기 마이그레이션

아래 리스트 순차대로 실행합니다.

소스 플로우 :
0. `ScheduleNodeRunner.java` :: ScheduledMnetService.execute() 주기 실행
1. `ScheduledMnetService.java` :: MnetDataDumpService 실행
2. `MnetDataDumpService.java` :: msSqlReplication 노드에 정의된 내용으로 MSSQL 에서 증분을 MYSQL 레플리카 테이블로 이관
3. `MigrationUtils.java` :: `MSSQL_DUMP_REPORT` / `MSSQL_DUMP_FAIL` 에 레포트 작성
4. `DBSyncService.java` :: `MIG_HISTORY` 에서 MNET / SCHEDULE 조건으로 최근에 실행된 날짜 조회 이후, 증분에 대해 노드 생성/수정 실행
5. `MigrationUtils.java` :: `MIG_HISTORY` / `NODE_CREATION_FAIL` 테이블에 레포트 기록

특이 :
- MSSQL 권한 부족으로 본사 94번 서버로 개발
- 추가 요구사항으로 URL 방식으로 서비스 호출
- 이미지 FileNotFoundException 에 대해서 무시하고 노드 생성

[[ 실행순서 ]]

0. 트리거 실행 :: `{{ Base Url }}/migration/mnet/renew/ {{ all /album / artist / song / mv / chart }}`

1. MSSQL to MYSQL

2. MYSQL to NODE

3. 레포트 작성


### 02. 마이그레이션 실패 노드 복구

`NODE_CREATION_FAIL` 테이블을 통하여 실패한 노드에 대한 복구를 실행함.

소스 ::
- `MnetController.java` :: url 기반 실행
- `MnetNodeRecoveryService.java` :  single 과 group 두가지 방식 (group 은 발생 예외 그룹에 해당하는 노드에 대해 모두 재시도, single 은 seq 를 키로 해당 노드만 리로드)

### 03. 마이그레이션 상황 모니터링

예시 URL : `http://125.131.88.147:8080/migration.html`

페이지에서 제공되는 정보는 아래와 같습니다. 
- `MIG_HISTORY` 테이블의 내역 
- `NODE_CREATION_FAIL` 테이블의 내역
- 현재 기동되고 있는 마이그레이션 목록입니다.

### 99. 정보

정보는 `/resources/schema/datasource/{profile}/dataSource.json` 에 명시된 정보와 물려서 실행됩니다.

```
mnetdb(개발) 172.16.37.250 1433  mmis
mnetdb(운영) 172.16.37.242 6231 mmis
mnetdb(아이온 본사) 125.131.88.94 1500 ice2

mnet 쪽 ID/PW : cjmwave_back / !dpadnpdlqm!@12
본사 쪽 ID/PW : iceuser / iceuser
```