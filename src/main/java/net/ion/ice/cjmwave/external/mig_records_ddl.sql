/* MNET / PIP */
DROP TABLE MIG_HISTORY;
CREATE TABLE MIG_HISTORY (
   seq INT PRIMARY KEY                    COMMENT 'PK' AUTO_INCREMENT
  , mig_target VARCHAR(100)               COMMENT 'PIP 이냐 MNET 이냐'
  , target_node VARCHAR(100)              COMMENT 'ALL 이나 대상 노드타입'
  , mig_type VARCHAR(100)                 COMMENT 'SCHEDULE / INIT / MANUAL'
  , mig_parameter VARCHAR(200)            COMMENT 'MANUAL 일 경우, Controller 가 받은 파라미터'
  , request_ip VARCHAR(200)               COMMENT 'MANUAL 일 경우, 요청한 IP'
  , success_cnt INT DEFAULT 0             COMMENT '결과 to NODE 의 성공 건수'
  , fail_cnt INT DEFAULT 0                COMMENT '결과 to NODE 의 실패 건수'
  , task_duration INT DEFAULT 0           COMMENT '마이그레이션 소요 시간'
  , execution_date DATETIME DEFAULT NOW() COMMENT '실행 시점'
);


/* MNET / PIP */
DROP TABLE MIG_DATA_HISTORY;
CREATE TABLE MIG_DATA_HISTORY (
   seq INT PRIMARY KEY                    COMMENT 'PK' AUTO_INCREMENT
  , target_node VARCHAR(200)              COMMENT '대상 노드 타입'
  , data_str LONGTEXT                     COMMENT '넣으려는 Map 의 string'
  , rs TINYINT(1)                         COMMENT '성공(0)/실패(1)' DEFAULT 0
  , created DATETIME                      COMMENT '생성일'
);


/* MNET */
DROP TABLE MSSQL_DUMP_REPORT;
CREATE TABLE MSSQL_DUMP_REPORT (
  seq INT  PRIMARY KEY                    COMMENT 'PK' AUTO_INCREMENT
  , mssqlTable VARCHAR(200)               COMMENT 'Mnet 대상 테이블명'
  , mysqlTable VARCHAR(200)               COMMENT 'ice2 대상 테이블명'
  , successCnt INT DEFAULT 0              COMMENT '성공건수'
  , skippedCnt INT DEFAULT 0              COMMENT '실패건수'
  , jobStarted DATETIME DEFAULT  NOW()    COMMENT '작업 시작시간'
  , jobFinished DATETIME DEFAULT NOW()    COMMENT '작업 종료시간'
  , jobDuration BIGINT DEFAULT 0          COMMENT '작업소요 시간'
);


/*
  개별 데이터에 대한 실패를 기록함
  MYSQL TO NODE FAILURE
*/
DROP TABLE MIG_FAIL_DATA;
CREATE TABLE MIG_FAIL_DATA (
  seq INT PRIMARY KEY                     COMMENT 'PK' AUTO_INCREMENT
  , nodeType VARCHAR(200)                 COMMENT  '대상 노드 타입'
  , nodeId VARCHAR(200)                   COMMENT '대상 노드의 키가 되는 정보'
  , nodeValue LONGTEXT                    COMMENT '실패한 삽입 정보'
  , created DATETIME DEFAULT NOW()        COMMENT '생성일'
);