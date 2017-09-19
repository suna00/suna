


-- 앨범
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_ALBUM_20170905_full.csv' INTO TABLE MT_ALBUM CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_ALBUM;
DELETE FROM MT_ALBUM;



-- 가사
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Documents/shTest/min_MT_SONG_LYRIC_20170905_full.csv' INTO TABLE MT_SONG_LYRIC
CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_SONG_LYRIC;
DELETE FROM MT_SONG_LYRIC;


-- 앨범 아티스트
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/50000min/min_MT_ALBUM_ARTIST_20170905_full.csv' INTO TABLE MT_ALBUM_ARTIST CHARACTER SET euckr FIELDS TERMINATED BY '|';
DELETE FROM MT_ALBUM_ARTIST;
SELECT * FROM MT_ALBUM_ARTIST;


-- 앨범 장르
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_ALBUM_GENRE_20170905_full.csv' INTO TABLE MT_ALBUM_GENRE CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_ALBUM_GENRE;


-- 앨범 키워드
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/5000min/min_MT_ALBUM_KEYWORD_20170905_full.csv' INTO TABLE MT_ALBUM_KEYWORD CHARACTER SET euckr FIELDS TERMINATED BY '|';
DELETE FROM MT_ALBUM_KEYWORD;
SELECT * FROM MT_ALBUM_KEYWORD;


-- 앨범 키워드 다국어
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/50000min/min_mt_album_keyword_meta_20170905_full.csv' INTO TABLE MT_ALBUM_KEYWORD_META CHARACTER SET euckr FIELDS TERMINATED BY '|';
DELETE FROM MT_ALBUM_KEYWORD_META;
SELECT * FROM MT_ALBUM_KEYWORD_META;


-- 앨범 곡
/* 변환률 100% 이상 없음. site_title 은 원래 없는 값인가? */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_ALBUM_SONGS_20170905_full.csv' INTO TABLE MT_ALBUM_SONGS CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_ALBUM_SONGS;

-- 아티스트
/*
  [2017-09-13 10:44:03] [HY000][1366] Incorrect integer value: '' for column 'DEBUT_SONG_ID' at row 1
  [2017-09-13 10:44:03] [HY000][1366] Incorrect integer value: '' for column 'DEBUT_ALBUM_ID' at row 1
  [2017-09-13 10:44:03] [HY000][1366] Incorrect integer value: '' for column 'FOLLOW_CNT' at row 1
  [2017-09-13 10:44:03] [HY000][1366] Incorrect integer value: '
  [2017-09-13 10:44:03] ' for column 'AID_CNT' at row 1
  에러는 나는데 일단 들어는 갔음
*/
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_ARTIST_20170905_full.csv' INTO TABLE MT_ARTIST CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_ARTIST WHERE ARTIST_ID=433966;
SELECT * FROM MT_ARTIST WHERE ARTIST_ID=433967;
SELECT * FROM MT_ARTIST;


-- 아티스트 장르
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_ARTIST_GENRE_20170905_full.csv' INTO TABLE MT_ARTIST_GENRE CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_ARTIST_GENRE;


-- 아티스트 키워드
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_ARTIST_KEYWORD_20170905_full.csv' INTO TABLE MT_ARTIST_KEYWORD CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_ARTIST_KEYWORD;


-- 뮤직 비디오
/*
  *에러* 91 개 로우 스킵
  LINES TERMINATED BY 'rn' 하면 409 개 들어가다 2개만 들어감 읭??
  1611/2000
*/
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_MV_20170905_full.csv' INTO TABLE MT_MV CHARACTER SET euckr FIELDS TERMINATED BY '|';
DELETE FROM MT_MV;
SELECT * FROM MT_MV;


-- 뮤직 비디오 아티스트
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_MV_ARTIST_20170905_full.csv' INTO TABLE MT_MV_ARTIST CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_MV_ARTIST;


-- 뮤직 비디오 임배디드
/*
   Data truncated for column 'CREATE_DT' at row 132 = 데이터 형식 맞지 않음
   오류는 발생하는데 다 들어감. 데이터 상으로도 CREATE_DT 가 없어서 그런가 봄
*/
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_MV_EMBED_20170905_full.csv' INTO TABLE MT_MV_EMBED CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_MV_EMBED;


-- 뮤직 비디오 키워드
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/5000min/min_MT_MV_KEYWORD_20170905_full.csv' INTO TABLE MT_MV_KEYWORD CHARACTER SET euckr FIELDS TERMINATED BY '|';
DELETE FROM MT_MV_KEYWORD;
SELECT * FROM MT_MV_KEYWORD;


-- 곡
/*
  [2017-09-13 11:21:25] [HY000][1366] Incorrect integer value: '' for column 'CDQ_SALE_PRICE' at row 16
  500 로우 들어감
*/
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_SONG_20170905_full.csv' INTO TABLE MT_SONG CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_SONG;


-- 곡 아티스트
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/50000min/min_MT_SONG_ARTIST_20170905_full.csv' INTO TABLE MT_SONG_ARTIST CHARACTER SET euckr FIELDS TERMINATED BY '|';
DELETE FROM MT_SONG_ARTIST;
SELECT * FROM MT_SONG_ARTIST;


-- 곡 키워드
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_SONG_KEYWORD_20170905_full.csv' INTO TABLE MT_SONG_KEYWORD CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_SONG_KEYWORD;

-- 곡 가사
/*
  * 에러 *
  엑셀 컨텐츠 중 특정 문자가 있으면 | 랑 붙어서 파싱 안되는 듯
  개행문자 때문에 안됨

  51 / 2000
*/
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_MT_SONG_LYRIC_20170905_full.csv' INTO TABLE MT_SONG_LYRIC CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_SONG_LYRIC;


-- 앨범 국가
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_mt_album_country_20170905_full.csv' INTO TABLE MT_ALBUM_COUNTRY CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_ALBUM_COUNTRY;


-- 아티스트 키워드 다국어
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_mt_artist_keyword_meta_20170905_full.csv' INTO TABLE MT_ARTIST_KEYWORD_META CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_ARTIST_KEYWORD_META;

-- 아티스트 다국어
/*
  * 에러 *
  [2017-09-13 11:44:09] [01000][1261] Row 14 doesn't contain data for all columns
  1189 / 2000
*/
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_mt_artist_meta_20170905_full.csv' INTO TABLE MT_ARTIST_META CHARACTER SET euckr FIELDS TERMINATED BY '|';


-- 뮤직 비디오 국가
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_mt_mv_country_20170905_full.csv' INTO TABLE MT_MV_COUNTRY CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_MV_COUNTRY;


-- 뮤직 비디오 키워드 다국어
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/5000min/min_mt_mv_keyword_meta_20170905_full.csv' INTO TABLE MT_MV_KEYWORD_META CHARACTER SET euckr FIELDS TERMINATED BY '|';
DELETE FROM MT_MV_KEYWORD_META;
SELECT * FROM MT_MV_KEYWORD_META;


-- 뷰직 비디오 다국어
/*
  * 에러 *
  1854 / 2000
*/
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_mt_mv_meta_20170905_full.csv' INTO TABLE MT_MV_META CHARACTER SET euckr FIELDS TERMINATED BY '|';


-- 곡 국가
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_mt_song_country_20170905_full.csv' INTO TABLE MT_SONG_COUNTRY CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_SONG_COUNTRY;


-- 곡 키워드 다국어
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_mt_song_keyword_meta_20170905_full.csv' INTO TABLE MT_SONG_KEYWORD_META CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_SONG_KEYWORD_META;


-- 곡 다국어
/* 변환률 100% 이상 없음 */
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Desktop/CJ_migration/2000min/min_mt_song_meta_20170905_full.csv' INTO TABLE MT_SONG_META CHARACTER SET euckr FIELDS TERMINATED BY '|';
SELECT * FROM MT_SONG_META;

-- 차트 마스터
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Documents/shTest/min_T_SM_S3_ChartTotalMst_20170919_full.csv' INTO TABLE T_SM_S3_ChartTotalMst CHARACTER SET euckr FIELDS TERMINATED BY '<|:>';
DELETE FROM T_SM_S3_ChartTotalMst;
SELECT * FROM T_SM_S3_ChartTotalMst;


-- 차트 데이터
LOAD DATA LOCAL INFILE '/Users/juneyoungoh/Documents/shTest/min_T_SM_S3_ChartTotalLst_20170919_full.csv' INTO TABLE T_SM_S3_ChartTotalLst CHARACTER SET euckr FIELDS TERMINATED BY '<|:>';
DELETE FROM T_SM_S3_ChartTotalLst;
SELECT * FROM T_SM_S3_ChartTotalLst;
