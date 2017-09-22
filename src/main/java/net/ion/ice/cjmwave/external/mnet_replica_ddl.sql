/*
============================================================================
============================================================================
================================ ALBUM =====================================
============================================================================
============================================================================
*/

CREATE TABLE MT_ALBUM (
  ALBUM_ID int NOT NULL,
  CREATE_DT Datetime NULL,
  UPDATE_DT Datetime NULL,
  ICPN nvarchar(30) NULL,
  AGENCY_CD nvarchar(50) NULL,
  DISPLAY_FLG nchar(1) NOT NULL,
  PRE_RELEASE_FLG nchar(1) NOT NULL,
  MONOPOLY_FLG nchar(1) NOT NULL,
  ALBUM_TYPE nvarchar(50) NOT NULL,
  ALBUM_TYPE_CD nchar(4) NOT NULL,
  ALBUM_DOMAIN nvarchar(50) NOT NULL,
  ALBUM_DOMAIN_CD nchar(4) NOT NULL,
  ALBUM_INDEX nchar(2) NULL,
  ALBUM_NM nvarchar(300) NOT NULL,
  ALBUM_SUBNM nvarchar(300) NULL,
  ALBUM_NUMBER nvarchar(10) NULL,
  RELEASE_COUNTRY nvarchar(50) NULL,
  RELEASE_COUNTRY_CD nvarchar(3) NULL,
  RELEASE_YMD nchar(8) NULL,
  ALBUM_INTRO Longtext NULL,
  SERIES_TAG nvarchar(100) NULL,
  DISTRIBUTE_COMPANY_ID int NULL,
  DISTRIBUTE_COMPANY_NM nvarchar(200) NULL,
  LABEL_COMPANY_NM nvarchar(200) NULL,
  LABEL_COMPANY_ID int NULL,
  PLANNING_COMPANY_ID int NULL,
  PLANNING_COMPANY_NM nvarchar(200) NULL,
  DISK_TOT_CNT smallint NOT NULL,
  SONG_TOT_CNT smallint NOT NULL,
  ARTIST_IDS nvarchar(100) NULL,
  ARTIST_NMS nvarchar(1000) NULL,
  STAR_CNT int NULL,
  STAR_USER_CNT int NULL,
  LIKE_CNT int NULL,
  POPULAR_CNT int NULL,
  VIEW_CNT int NULL,
  HELLO_FLG nchar(1) NULL,
  BOOM_ID int NULL,
  ADMIN_COMMENT nvarchar(1000) NULL,
  TITLE_SONG_ID int NULL,
  ALBUM_SALE_FLG nchar(1) NULL,
  ALBUM_SALE_RATE nvarchar(10) NULL,
  ALBUM_SALE_PRICE int NULL,
  MQS_SALE_FLG nchar(1) NULL,
  MQS_SALE_PRICE int NULL,
  MQS_ACCOUNT_PRICE int NULL
);


CREATE TABLE MT_ALBUM_SONGS(
  ALBUM_ID int NOT NULL,
  DISK_NO tinyint UNSIGNED NOT NULL,
  TRACK_NO smallint NOT NULL,
  SIDE_TITLE nvarchar(100) NULL,
  SONG_ID int NOT NULL,
  TITLE_FLG nchar(1) NOT NULL,
  HIT_FLG nchar(1) NOT NULL,
  CONSTRAINT PK__MT_ALBUM__39A90E2D0D7A0286 PRIMARY KEY
    (
      ALBUM_ID ASC,
      DISK_NO ASC,
      TRACK_NO ASC
    )
) ;


CREATE TABLE MT_ALBUM_META(
  ALBUM_ID int NOT NULL,
  LANG_CD nchar(4) NOT NULL,
  ALBUM_NM nvarchar(300) NULL,
  ALBUM_SUBNM nvarchar(300) NULL,
  ALBUM_INTRO Longtext NULL,
  SERIES_TAG nvarchar(100) NULL,
  CONSTRAINT PK_MT_ALBUM_META PRIMARY KEY
    (
      ALBUM_ID ASC,
      LANG_CD ASC
    )
) ;


CREATE TABLE MT_ALBUM_KEYWORD_META(
  ALBUM_ID int NOT NULL,
  LANG_CD nchar(4) NOT NULL,
  SEQ int NOT NULL,
  KEYWORD nvarchar(200) NOT NULL,
  CONSTRAINT PK_MT_ALBUM_KEYWORD_META PRIMARY KEY
    (
      ALBUM_ID ASC,
      LANG_CD ASC,
      SEQ ASC
    )
) ;


CREATE TABLE MT_ALBUM_KEYWORD(
  SEQ int AUTO_INCREMENT NOT NULL,
  ALBUM_ID int NOT NULL,
  KEYWORD nvarchar(200) NULL,
  CONSTRAINT PK_MT_ALBUM_KEYWORD PRIMARY KEY
    (
      SEQ ASC
    )
) ;


CREATE TABLE MT_ALBUM_GENRE(
  GENRE_SEQ int AUTO_INCREMENT NOT NULL,
  GENRE_CD nchar(4) NULL,
  GENRE_NM nvarchar(100) NULL,
  ALBUM_ID int NOT NULL,
  CREATE_DT Datetime NOT NULL,
  UPDATE_DT Datetime NOT NULL,
  CONSTRAINT PK__MT_ALBUM__313A92A902084FDA PRIMARY KEY
    (
      GENRE_SEQ ASC
    )
) ;


CREATE TABLE MT_ALBUM_COUNTRY(
  ALBUM_ID int NOT NULL,
  COUNTRY_CD nchar(3) NOT NULL,
  CONSTRAINT PK_MT_ALBUM_COUNTRY PRIMARY KEY
    (
      ALBUM_ID ASC,
      COUNTRY_CD ASC
    )
) ;


CREATE TABLE MT_ALBUM_ARTIST(
  SEQ int AUTO_INCREMENT NOT NULL,
  ALBUM_ID int NOT NULL,
  ARTIST_ID int NULL,
  ARTIST_SEQ int NULL,
  ARTIST_NM nvarchar(100) NULL,
  DISPLAY_ORD tinyint UNSIGNED NULL,
  CONSTRAINT PK_MT_ALBUM_ARTIST PRIMARY KEY
    (
      SEQ ASC
    )
);

/*
============================================================================
============================================================================
================================ ARTIST =====================================
============================================================================
============================================================================
*/


CREATE TABLE MT_ARTIST_META(
  ARTIST_ID int NOT NULL,
  LANG_CD nchar(4) NOT NULL,
  ARTIST_NM nvarchar(100) NULL,
  ARTIST_INTRO Longtext NULL,
  ARTIST_PREV_ACTIVE_NM nvarchar(100) NULL,
  CONSTRAINT PK_MT_ARTIST_META PRIMARY KEY
    (
      ARTIST_ID ASC,
      LANG_CD ASC
    )
) ;


CREATE TABLE MT_ARTIST_KEYWORD_META(
  ARTIST_ID int NOT NULL,
  LANG_CD nchar(4) NOT NULL,
  SEQ int NOT NULL,
  KEYWORD nvarchar(200) NOT NULL,
  CONSTRAINT PK_MT_ARTIST_KEYWORD_META PRIMARY KEY
    (
      ARTIST_ID ASC,
      LANG_CD ASC,
      SEQ ASC
    )
) ;


CREATE TABLE MT_ARTIST_KEYWORD(
  SEQ int AUTO_INCREMENT NOT NULL,
  KEYWORD nvarchar(200) NULL,
  ARTIST_ID int NOT NULL,
  CONSTRAINT PK__MT_ARTIS__CA1938C0245D67DE PRIMARY KEY
    (
      SEQ ASC
    )
) ;


CREATE TABLE MT_ARTIST_GENRE(
  GENRE_SEQ int AUTO_INCREMENT NOT NULL,
  GENRE_CD nchar(4) NOT NULL,
  GENRE_NM nvarchar(100) NULL,
  ARTIST_ID int NOT NULL,
  CREATE_DT Datetime NOT NULL,
  UPDATE_DT Datetime NOT NULL,
  CONSTRAINT PK__MT_ARTIS__313A92A91CBC4616 PRIMARY KEY
    (
      GENRE_SEQ ASC
    )
) ;


CREATE TABLE MT_ARTIST_COUNTRY(
  ARTIST_ID int NOT NULL,
  COUNTRY_CD nchar(3) NOT NULL,
  CONSTRAINT PK_MT_ARTIST_COUNTRY PRIMARY KEY
    (
      ARTIST_ID ASC,
      COUNTRY_CD ASC
    )
) ;


CREATE TABLE MT_ARTIST(
  ARTIST_ID int NOT NULL,
  CREATE_DT Datetime NULL,
  UPDATE_DT Datetime NULL,
  DISPLAY_FLG nchar(1) NULL,
  ARTIST_TYPE nvarchar(50) NOT NULL,
  ARTIST_TYPE_CD nchar(4) NOT NULL,
  ARTIST_DOMAIN nvarchar(50) NOT NULL,
  ARTIST_DOMAIN_CD nchar(4) NOT NULL,
  ARTIST_INDEX nchar(2) NULL,
  ARTIST_NM nvarchar(100) NOT NULL,
  ARTIST_KNM nvarchar(100) NULL,
  ARTIST_ENM nvarchar(100) NULL,
  ARTIST_CNM nvarchar(100) NULL,
  ARTIST_JNM nvarchar(100) NULL,
  ARTIST_ONM nvarchar(100) NULL,
  ARTIST_PREV_ACTIVE_NM nvarchar(100) NULL,
  ARTIST_NATIONALITY nvarchar(50) NULL,
  ARTIST_NATIONALITY_CD nvarchar(3) NULL,
  ARTIST_GENDER nchar(4) NOT NULL,
  ACTIVE_PERIOD_STR nvarchar(300) NULL,
  ARTIST_BIRTH_YMD nchar(8) NULL,
  ARTIST_DEATH_YMD nchar(8) NULL,
  DEBUT_YMD nchar(8) NULL,
  DEBUT_SONG_ID int NULL,
  DEBUT_ALBUM_ID int NULL,
  ARTIST_SITE nvarchar(150) NULL,
  ARTIST_INTRO Longtext NULL,
  ARTIST_ROLE_STR nvarchar(150) NULL,
  MICRO_BLOG_URL nvarchar(200) NULL,
  LIKE_CNT int NULL,
  POPULAR_CNT int NULL,
  VIEW_CNT int NULL,
  FOLLOW_CNT int NULL,
  AID_CNT int NULL,
  CONSTRAINT PK_MT_ARTIST PRIMARY KEY
    (
      ARTIST_ID ASC
    )
) ;


/*
============================================================================
============================================================================
================================ SONG ======================================
============================================================================
============================================================================
*/


CREATE TABLE MT_SONG(
  SONG_ID int NOT NULL,
  CREATE_DT Datetime NULL,
  UPDATE_DT Datetime NULL,
  ISRC nchar(12) NULL,
  AGENCY_CD nvarchar(50) NULL,
  CP_ID int NULL,
  DISPLAY_FLG nchar(1) NOT NULL,
  SONG_TYPE nvarchar(50) NOT NULL,
  SONG_TYPE_CD nchar(4) NOT NULL,
  SONG_DOMAIN nvarchar(50) NOT NULL,
  SONG_DOMAIN_CD nchar(4) NOT NULL,
  GENRE_CD nchar(4) NULL,
  GENRE_NM nvarchar(100) NULL,
  SONG_VERSION_TYPE nvarchar(50) NULL,
  SONG_VERSION_TYPE_CD nchar(4) NULL,
  MASTER_SONG_ID int NULL,
  SONG_NM nvarchar(500) NOT NULL,
  SONG_SHORT_INFO nvarchar(200) NULL,
  SONG_MEDIA_INFO nvarchar(500) NULL,
  RELEASE_YMD nchar(8) NULL,
  RUNNING_TIME nvarchar(10) NULL,
  ALBUM_ID int NULL,
  ADULT_FLG nchar(1) NOT NULL,
  ST_GB tinyint UNSIGNED NOT NULL,
  DL_GB tinyint UNSIGNED NOT NULL,
  RING_FLG nchar(1) NOT NULL,
  BELL_FLG nchar(1) NOT NULL,
  LDB_FLG nchar(1) NOT NULL,
  LDP_FLG nchar(1) NOT NULL,
  ARTIST_IDS nvarchar(300) NULL,
  ARTIST_NMS nvarchar(1000) NULL,
  IOS_ST_GB tinyint UNSIGNED NOT NULL,
  IOS_DL_GB tinyint UNSIGNED NOT NULL,
  AND_ST_GB tinyint UNSIGNED NOT NULL,
  AND_DL_GB tinyint UNSIGNED NOT NULL,
  DL_TOP_FLG nchar(1) NOT NULL,
  ST_TOP_FLG nchar(1) NOT NULL,
  PSTREAM_URL nvarchar(300) NULL,
  REL_VOD_FLG nchar(1) NOT NULL,
  LIKE_CNT int NULL,
  POPULAR_CNT int NULL,
  VIEW_CNT int NULL,
  HELLO_FLG nchar(1) NULL,
  HB_ST_FLG nchar(1) NULL,
  HB_ST_START_YMD nvarchar(8) NULL,
  HB_ST_END_YMD nvarchar(8) NULL,
  HB_DL_FLG nchar(1) NULL,
  HB_DL_START_YMD nvarchar(8) NULL,
  HB_DL_END_YMD nvarchar(8) NULL,
  SONG_DOWN_SALE_FLG nchar(1) NULL,
  SONG_DOWN_SALE_PRICE int NULL,
  SONG_DOWN_ACCOUNT_PRICE int NULL,
  MQS_SALE_FLG nchar(1) NULL,
  MQS_SALE_PRICE int NULL,
  MQS_ACCOUNT_PRICE int NULL,
  CDQ_SALE_PRICE int NULL,
  CDQ_ACCOUNT_PRICE int NULL,
  CDQ_SALE_FLG nchar(1) NULL,
  CONSTRAINT PK__MT_SONG__7FAA8A4E37703C52 PRIMARY KEY
    (
      SONG_ID ASC
    )
) ;


CREATE TABLE MT_SONG_META(
  SONG_ID int NOT NULL,
  LANG_CD nchar(4) NOT NULL,
  SONG_NM nvarchar(500) NOT NULL,
  SONG_SHORT_INFO nvarchar(200) NULL,
  SONG_MEDIA_INFO nvarchar(500) NULL,
  LYRIC Longtext NULL,
  CONSTRAINT PK_MT_SONG_META PRIMARY KEY
    (
      SONG_ID ASC,
      LANG_CD ASC
    )
);


CREATE TABLE MT_SONG_LYRIC(
  SONG_ID int NOT NULL,
  CREATE_DT Datetime NOT NULL,
  UPDATE_DT Datetime NOT NULL,
  LYRIC Longtext NULL,
  REG_MCODE int NULL,
  REG_USERID nvarchar(20) NULL,
  REG_USERNM nvarchar(20) NULL,
  REG_DT Datetime NULL,
  MOD_MCODE int NULL,
  MOD_USERID nvarchar(20) NULL,
  MOD_USERNM nvarchar(20) NULL,
  MOD_DT Datetime NULL,
  CONSTRAINT PK__MT_SONG___7FAA8A4E42E1EEFE PRIMARY KEY
    (
      SONG_ID ASC
    )
);


CREATE TABLE MT_SONG_KEYWORD_META(
  SONG_ID int NOT NULL,
  LANG_CD nchar(4) NOT NULL,
  SEQ int NOT NULL,
  KEYWORD nvarchar(350) NOT NULL,
  CONSTRAINT PK_MT_SONG_KEYWORD_META PRIMARY KEY
    (
      SONG_ID ASC,
      LANG_CD ASC,
      SEQ ASC
    )
) ;


CREATE TABLE MT_SONG_KEYWORD(
  SEQ int AUTO_INCREMENT NOT NULL,
  SONG_ID int NOT NULL,
  KEYWORD nvarchar(350) NULL,
  CONSTRAINT PK__MT_SONG___CA1938C03F115E1A PRIMARY KEY
    (
      SEQ ASC
    )
) ;


CREATE TABLE MT_SONG_COUNTRY(
  SONG_ID int NOT NULL,
  COUNTRY_CD nchar(3) NOT NULL,
  CONSTRAINT PK_MT_SONG_COUNTRY PRIMARY KEY
    (
      SONG_ID ASC,
      COUNTRY_CD ASC
    )
) ;


CREATE TABLE MT_SONG_ARTIST(
  SEQ int AUTO_INCREMENT NOT NULL,
  SONG_ID int NOT NULL,
  ARTIST_ID int NULL,
  ARTIST_SEQ int NULL,
  ARTIST_NM nvarchar(100) NULL,
  DISPLAY_ORD tinyint UNSIGNED NULL,
  CONSTRAINT PK_MS_SONG_ARTIST PRIMARY KEY
    (
      SEQ ASC
    )
) ;



/*
============================================================================
============================================================================
================================ MUSIC VIDEO ===============================
============================================================================
============================================================================
*/


CREATE TABLE MT_MV_META(
  MV_ID int NOT NULL,
  LANG_CD nchar(4) NOT NULL,
  MV_TITLE nvarchar(500) NULL,
  MV_SUBTITLE nvarchar(200) NULL,
  MV_INTRO Longtext NULL,
  CONSTRAINT PK_MT_MV_META PRIMARY KEY
    (
      MV_ID ASC,
      LANG_CD ASC
    )
) ;


CREATE TABLE MT_MV_KEYWORD_META(
  MV_ID int NOT NULL,
  LANG_CD nchar(4) NOT NULL,
  SEQ int NOT NULL,
  KEYWORD nvarchar(350) NOT NULL,
  CONSTRAINT PK_MT_MV_KEYWORD_META PRIMARY KEY
    (
      MV_ID ASC,
      LANG_CD ASC,
      SEQ ASC
    )
) ;


CREATE TABLE MT_MV_KEYWORD(
  SEQ int AUTO_INCREMENT NOT NULL,
  MV_ID int NOT NULL,
  KEYWORD_GB nchar(2) NOT NULL,
  KEYWORD nvarchar(350) NULL,
  CONSTRAINT PK_MT_MV_KEYWORD PRIMARY KEY
    (
      SEQ ASC
    )
) ;


CREATE TABLE MT_MV_EMBED(
  MV_ID int NOT NULL,
  YOUTUBE_URL nvarchar(300) NULL,
  TUDOU_URL nvarchar(300) NULL,
  CREATE_DT Datetime NULL,
  CONSTRAINT PK_MT_MV_EMBED PRIMARY KEY
    (
      MV_ID ASC
    )
) ;


CREATE TABLE MT_MV_COUNTRY(
  MV_ID int NOT NULL,
  COUNTRY_CD nchar(3) NOT NULL,
  CONSTRAINT PK_MT_MV_COUNTRY PRIMARY KEY
    (
      MV_ID ASC,
      COUNTRY_CD ASC
    )
) ;


CREATE TABLE MT_MV_ARTIST(
  SEQ int AUTO_INCREMENT NOT NULL,
  MV_ID int NOT NULL,
  ARTIST_ID int NULL,
  ARTIST_NM nvarchar(100) NULL,
  DISPLAY_ORD tinyint UNSIGNED NULL,
  ARTIST_SEQ int NULL,
  CONSTRAINT PK_MT_MV_ARTIST PRIMARY KEY
    (
      SEQ ASC
    )
) ;


CREATE TABLE MT_MV(
  MV_ID int NOT NULL,
  MV_TITLE nvarchar(500) NOT NULL,
  MV_GRADE nchar(4) NULL,
  DISPLAY_FLG nchar(1) NOT NULL,
  CREATE_DT Datetime NULL,
  UPDATE_DT Datetime NULL,
  RELEASE_YMD nchar(8) NULL,
  RUNNING_TIME nvarchar(10) NULL,
  CP_ID int NULL,
  MV_INTRO Longtext NULL,
  AGENCY_CD nvarchar(50) NULL,
  GENRE_NM nvarchar(100) NULL,
  SONG_ID int NULL,
  ARTIST_NMS nvarchar(1000) NULL,
  MV_SUBTITLE nvarchar(200) NULL,
  GENRE_CD nchar(4) NULL,
  MV_GB tinyint UNSIGNED NOT NULL,
  INTERNAL_COMMENT nvarchar(300) NULL,
  ST_GB tinyint UNSIGNED NOT NULL,
  DL_GB tinyint UNSIGNED NOT NULL,
  ARTIST_IDS nvarchar(100) NULL,
  PRODUCTION_COUNTRY_CD nvarchar(3) NULL,
  PRODUCTION_COUNTRY nvarchar(50) NULL,
  IOS_ST_GB tinyint UNSIGNED NOT NULL,
  AND_ST_GB tinyint UNSIGNED NOT NULL,
  STV_ST_GB tinyint UNSIGNED NOT NULL,
  IMG_ID int NULL,
  LIKE_CNT int NULL,
  POPULAR_CNT int NULL,
  VIEW_CNT int NULL,
  FREE_ST_FLG nchar(1) NULL,
  DL_CNT int NULL,
  MV_GRADE_KMRB nchar(4) NULL,
  PLANNING_COMPANY nvarchar(200) NULL,
  CHECK_NO nvarchar(50) NULL,
  RIGHT_COMPANY nvarchar(200) NULL,
  BROAD_COMPANY nvarchar(200) NULL,
  BROAD_YMD nvarchar(20) NULL,
  OST_FLG nchar(1) NULL,
  HD_ST_GB tinyint UNSIGNED NULL,
  VR_VOD_FLG char(1) NULL,
  VERTICAL_YN char(1) NULL,
  CONSTRAINT PK_MT_MV PRIMARY KEY
    (
      MV_ID ASC
    )
) ;


/*
============================================================================
============================================================================
================================ CHARTS ====================================
============================================================================
============================================================================
*/



CREATE TABLE T_SM_S3_ChartTotalMst(
  MstIdx int AUTO_INCREMENT NOT NULL,
  MstYear nvarchar(4) NOT NULL,
  MstMonth nvarchar(2) NOT NULL,
  MstWeek nvarchar(2) NOT NULL,
  MstYyMmDd nvarchar(8) NOT NULL,
  MstFromDate nvarchar(10) NOT NULL,
  MstToDate nvarchar(10) NOT NULL,
  ServiceFlag nchar(1) NOT NULL,
  RegDate Datetime NOT NULL,
  ModDate Datetime NOT NULL,
  ONAIR_DT nvarchar(8) NULL,
  CONSTRAINT PK_SM_S3_ChartTotalMst PRIMARY KEY
    (
      MstIdx DESC
    )
);


CREATE TABLE T_SM_S3_ChartTotalLst(
  Idx int AUTO_INCREMENT NOT NULL,
  chMstID int NOT NULL,
  SongID int NOT NULL,
  Ranking int NOT NULL,
  RankInter int NOT NULL,
  RankInterIcon nchar(1) NOT NULL,
  RankPreWeek int NOT NULL,
  RankPeak int NULL,
  RankDuring int NULL,
  songRanking int NOT NULL,
  albumRanking int NOT NULL,
  musicRanking int NOT NULL,
  researchRanking int NOT NULL,
  pdRanking int NOT NULL,
  smsRanking int NOT NULL,
  RegDate Datetime NOT NULL,
  tvRanking int NOT NULL,
  ageRanking int NULL,
  CONSTRAINT PK_SM_S3_ChartTotalLst PRIMARY KEY
    (
      Idx DESC
    )
);