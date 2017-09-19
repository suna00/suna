DROP TABLE program2;
CREATE TABLE program2 (
  programId VARCHAR(150) PRIMARY KEY      COMMENT '프로그램 아이디'
  , programCd VARCHAR(200)                COMMENT '프로그램 코드'
  , title VARCHAR(200)                    COMMENT '프로그램 명'
  , synopsis LONGTEXT                     COMMENT '프로그램 설명'
  , genre VARCHAR(20)                     COMMENT '프로그램 장르'
  , targetAge INT(5)                      COMMENT '시청가능 나이'
  , chId VARCHAR(10)                      COMMENT '채널 아이디'
  , searchKeyword VARCHAR(300)            COMMENT '검색 키워드'
  , startDate VARCHAR(20)                 COMMENT '방송 시작일 8자리'
  , endDate VARCHAR(20)                   COMMENT '방송 종료일 8자리'
  , weekCd VARCHAR(20)                    COMMENT '방송요일 0000001 인데 우리 쪽 요일 코드에 맞게 변경'
  , startTime VARCHAR(20)                 COMMENT '방송 시작 시간 4자리'
  , endTime VARCHAR(20)                   COMMENT '방송 종료 시간 4자리'
  , regDate DATETIME                      COMMENT '등록 일시 14 자리'
  , modifyDate DATETIME                   COMMENT '수정 일시 14 자리'
  , homepageUrl TEXT                      COMMENT '방송사 URL'
  , reviewUrl TEXT                        COMMENT '방송사 다시보기 URL'
  , bbsUrl TEXT                           COMMENT '방송사 게사판 URL'
  , programImg TEXT                       COMMENT '프로그램 16:9 대표 이미지'
  , programPosterImg TEXT                 COMMENT '프로그램 포스터 이미지'
  , programBannerImg TEXT                 COMMENT '프로그램 배너 이미지'
  , programThumbImg TEXT                  COMMENT '프로그램 썸네일 이미지'
  , prsnName VARCHAR(500)                 COMMENT '출연자 이름'
  , prsnFName VARCHAR(500)                COMMENT '출연자 본명'
  , prsnNo VARCHAR(500)                   COMMENT '출연자 코드'
  , actor TEXT                            COMMENT '출연진 , 문자열'
  , director TEXT                         COMMENT '연출진 , 문자열'
  , isUse TINYINT(1) DEFAULT 0            COMMENT '사용여부 YN'

  , owner VARCHAR(100)                    COMMENT '등록자'
  , created DATETIME DEFAULT NOW()        COMMENT '등록일시'
  , modifier VARCHAR(100)                 COMMENT '수정자'
  , changed DATETIME DEFAULT NOW()        COMMENT '수정일시'
  , showYn TINYINT(1) DEFAULT 0           COMMENT '노출 여부'
  , mnetIfTrtYn TINYINT(1) DEFAULT 1      COMMENT 'mnet 인터페이스 처리 여부'
  , relArtistIds LONGTEXT                 COMMENT '연관 아티스트 아이디'
  , relPgmIds LONGTEXT                    COMMENT '연관 프로그램 아이디'
  -- , refdMultiLanginfo LONGTEXT            COMMENT '프로그램 다국어 리스트'
  , showCntryCdList LONGTEXT              COMMENT '노출국가 코드리스트'
);

DROP TABLE pgmVideo2;
CREATE TABLE pgmVideo2
(
  contentId VARCHAR(150) PRIMARY KEY      COMMENT '컨텐츠 아이디'
  , programId VARCHAR(150)                COMMENT '프로그램 아이디'
  , contentTitle VARCHAR(200)             COMMENT '회차 제목'
  , cornerId BIGINT(10)                   COMMENT '부회차 아이디'
  , clipOrder BIGINT(10)                  COMMENT '클립 순서'
  , title VARCHAR(200)                    COMMENT '영상 제목'
  , synopsis LONGTEXT                     COMMENT '영상 내용'
  , prsnName VARCHAR(500)                 COMMENT '출연자 이름'
  , prsnFName VARCHAR(500)                COMMENT '출연자 본명'
  , directPrsn VARCHAR(500)               COMMENT '연출자 목록'
  , prsnNo TEXT                           COMMENT '출연자 코드'
  , searchKeyword VARCHAR(500)            COMMENT '검색키워드'
  , mediaUrl VARCHAR(500)                 COMMENT '영상 URL'
  , itemTypeId VARCHAR(200)               COMMENT '제공 미디어 종류'
  , clipType VARCHAR(20)                  COMMENT '영상 구분 코드 VOD/CLIP'
  , contentType VARCHAR(20)               COMMENT '콘텐츠 타입 정보'
  , broadDate DATETIME                    COMMENT '방영일시'
  , regDate DATETIME                      COMMENT 'PIP 등록일'
  , modifyDate DATETIME                   COMMENT 'PIP 수정일'
  , contentImgUrl VARCHAR(500)            COMMENT '클립영상 대표 이미지 URL'
  , playTime BIGINT(20) DEFAULT 0         COMMENT '플레이 시간(초)'
  , targetAge INT(5)                      COMMENT '시청 가능 나이'
  , adLink VARCHAR(500)                   COMMENT '광고 URL'
  , price VARCHAR(20)                     COMMENT '가격(원)'
  , isMasterClip TINYINT(1) DEFAULT 0     COMMENT '대표 클립 영상 지정 여부'
  , isUse TINYINT(1) DEFAULT 0            COMMENT '사용여부'
  , isFullVod TINYINT(1) DEFAULT 0        COMMENT 'FullVod 유무 체크'

  , mnetIfTrtYn TINYINT(1) DEFAULT 1      COMMENT 'mnet 인터페이스 처리 여부'
  , rcmdContsYn TINYINT(1) DEFAULT 0      COMMENT '추천 여부'
  , owner VARCHAR(100)                    COMMENT '등록자'
  , created DATETIME DEFAULT NOW()        COMMENT '등록일시'
  , modifier VARCHAR(100)                 COMMENT '수정자'
  , changed DATETIME DEFAULT NOW()        COMMENT '수정일시'
  , showCntryCdList LONGTEXT              COMMENT '노출국가 코드리스트'
  , ctgryId VARCHAR(300)                  COMMENT '카테고리 아이디?'
  , subtitlePath TEXT                     COMMENT '자막경로'
);
