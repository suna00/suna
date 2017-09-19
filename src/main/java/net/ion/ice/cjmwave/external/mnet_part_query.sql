/*
	주기적 쿼리
	- 연관 테이블은 증분 확인값이 존재하지 않기 때문에 in 절로 원천이 변경된 부분에 대해 수행

  REFERENCES
  dbSyncProcessData.json 에서 라인브레이크를 허용하지 않기에
  참고용으로 빼놈
*/

-- 앨범
SELECT
		ALBUM_ID as albumId
	, ALBUM_NM as albumNm
	, ARTIST_IDS as artistId
	, RELEASE_YMD as comotDt
	, RELEASE_COUNTRY_CD as comotCntry
	, ALBUM_TYPE_CD as typeCd
	, (SELECT GROUP_CONCAT(GENRE_CD) FROM MT_ALBUM_GENRE WHERE ALBUM_ID = ALBUM.ALBUM_ID) AS genreCd
	, DISTRIBUTE_COMPANY_NM as dstrbCrcrt
	, ALBUM_SUBNM as subName
	, SERIES_TAG as sersName
	, ALBUM_INTRO as albumDesc
	, (SELECT GROUP_CONCAT(KEYWORD) FROM MT_ALBUM_KEYWORD WHERE ALBUM_ID = ALBUM.ALBUM_ID) as findKywrd
	, (SELECT GROUP_CONCAT(country_cd) FROM MT_ALBUM_COUNTRY WHERE ALBUM_ID = ALBUM.ALBUM_ID) as showCntryCdList
	, (SELECT GROUP_CONCAT(ARTIST_ID) FROM MT_ALBUM_ARTIST WHERE ALBUM_ID = ALBUM.ALBUM_ID) as relArtistIds
	, (SELECT GROUP_CONCAT(SONG_ID) FROM MT_ALBUM_SONGS WHERE ALBUM_ID = ALBUM.ALBUM_ID) as relSongIds
	, 1 as mnetIfTrtYn
	, CREATE_DT as created
	, UPDATE_DT as changed
FROM
	MT_ALBUM ALBUM
WHERE
	CREATE_DT > @{DATETIME.lastUpdated}
OR UPDATE_DT > @{DATETIME.lastUpdated}


-- 앨범 다국어
SELECT
		album_id as albumId
	, lang_cd as langCd
	, album_nm as albumNm
	, album_subnm as subName
	, series_tag as sersName
	, album_intro as albumDesc
	, (SELECT GROUP_CONCAT(keyword) FROM MT_ALBUM_KEYWORD_META WHERE album_id = ALBUM_META.album_id AND lang_cd = ALBUM_META.lang_cd) as findKywrd
FROM
	MT_ALBUM_META ALBUM_META
WHERE
	album_id in @{STRARR.albumIds}


-- 아티스트

SELECT
		ARTIST_ID as artistId
	, ARTIST_NM as artistNm
	, ARTIST_BIRTH_YMD as cretDt
	, ARTIST_NATIONALITY as bpnac
	, ARTIST_GENDER as sex
	, ARTIST_TYPE_CD as typeCd
	, (SELECT GROUP_CONCAT(GENRE_CD) FROM MT_ARTIST_GENRE WHERE ARTIST_ID = ARTIST.ARTIST_ID) as genreCd
	, DEBUT_YMD as debutDt
	, DEBUT_ALBUM_ID as debutAlbum
	, ARTIST_INTRO as artistDesc
	, ARTIST_NM as atvyName
	, (SELECT GROUP_CONCAT(KEYWORD) FROM MT_ARTIST_KEYWORD WHERE ARTIST_ID = ARTIST.ARTIST_ID) as findKywrd
	, (SELECT GROUP_CONCAT(country_cd) FROM MT_ARTIST_COUNTRY WHERE ARTIST_ID = ARTIST.ARTIST_ID) as showCntryCdList
	, DISPLAY_FLG as showYn
	, 1 as mnetIfTrtYn
	, CREATE_DT as created
	, UPDATE_DT as changed
FROM
	MT_ARTIST ARTIST
WHERE
	CREATE_DT > @{DATETIME.lastUpdated}
OR UPDATE_DT > @{DATETIME.lastUpdated}


-- 아티스트 다국어

SELECT
		artist_id as artistId
	, lang_cd as langCd
	, artist_nm as artistNm
	, artist_intro as artistDesc
	, artist_prev_active_nm as atvyName
	, (SELECT GROUP_CONCAT(keyword) FROM MT_ARTIST_KEYWORD_META WHERE artist_id = ARTIST_META.artist_id AND lang_cd = ARTIST_META.lang_cd) as findKywrd
FROM
	MT_ARTIST_META ARTIST_META
WHERE
	artist_id in @{STRARR.artistIds}


-- 곡

SELECT
		SONG_ID as songId
	, SONG_NM as songNm
	, SONG_SHORT_INFO as subName
	, SONG_MEDIA_INFO as songDesc
	, ARTIST_IDS as artistId
	, ALBUM_ID as albumId
	, RUNNING_TIME as playTime
	, (SELECT LYRIC FROM MT_SONG_LYRIC WHERE SONG_ID = SONG.SONG_ID) as wrds
	, (SELECT GROUP_CONCAT(KEYWORD) FROM MT_SONG_KEYWORD WHERE SONG_ID = SONG.SONG_ID) as findKywrd
	, (SELECT GROUP_CONCAT(country_cd) FROM MT_SONG_COUNTRY WHERE song_id = SONG.SONG_ID) as showCntryCdList
	, (SELECT GROUP_CONCAT(ALBUM_ID) FROM MT_ALBUM_SONGS WHERE SONG_ID = SONG.SONG_ID) as refAlbumIds
	, (SELECT GROUP_CONCAT(ARTIST_ID) FROM MT_SONG_ARTIST WHERE SONG_ID = SONG.SONG_ID) as refArtistIds
	, 1 as mnetIfTrtYn
	, CREATE_DT as created
	, UPDATE_DT as changed
FROM
	MT_SONG SONG
WHERE
	CREATE_DT > @{DATETIME.lastUpdated}
OR UPDATE_DT > @{DATETIME.lastUpdated}


-- 곡 다국어

SELECT
		song_id as songId
	, lang_cd as langCd
	, song_nm as songNm
	, song_short_info as subName
	, song_media_info as songDesc
	, (SELECT GROUP_CONCAT(keyword) FROM MT_SONG_KEYWORD_META WHERE song_id = MSM.song_id) as findKywrd
FROM
	MT_SONG_META MSM
WHERE
	song_id in @{STRARR.songIds}


-- 뮤비

SELECT
		MV_ID as musicVideoId
	, MV_TITLE as musicVideoNm
	, MV_SUBTITLE as subName
	, GENRE_CD as genreCd
	, MV_INTRO as musicVideoDesc
	, (SELECT GROUP_CONCAT(KEYWORD) FROM MT_MV_KEYWORD WHERE MV_ID = MV.MV_ID) as findKywrd
	, RELEASE_YMD as comotDt
	, ARTIST_IDS as artistId
	, (SELECT YOUTUBE_URL FROM MT_MV_EMBED WHERE MV_ID = MV.MV_ID) as youtubeUrl
	, (SELECT TUDOU_URL FROM MT_MV_EMBED WHERE MV_ID = MV.MV_ID) as todouUrl
	, RUNNING_TIME as playTime
	, (SELECT GROUP_CONCAT(country_cd) FROM MT_MV_COUNTRY WHERE mv_id = MV.MV_ID) showCntryCdList
	, 1 as mnetIfTrtYn
	, DISPLAY_FLG as showYn
	, SONG_ID as relSongIds
	, (SELECT GROUP_CONCAT(ARTIST_ID) FROM MT_MV_ARTIST WHERE MV_ID = MV.MV_ID) relArtistIds
	, CREATE_DT as created
	, UPDATE_DT as changed
FROM
	MT_MV MV
WHERE
	CREATE_DT > @{DATETIME.lastUpdated}
OR UPDATE_DT > @{DATETIME.lastUpdated}


-- 뮤비 다국어
SELECT
		mv_id as musicVideoId
	, lang_cd as langCd
	, mv_title as musicVideoNm
	, mv_subtitle as subName
	, mv_intro as musicVideoDesc
	, (SELECT YOUTUBE_URL FROM MT_MV_EMBED WHERE MV_ID = MMM.mv_id) as youtubeUrl
	, (SELECT TUDOU_URL FROM MT_MV_EMBED WHERE MV_ID = MMM.mv_id) as todouUrl
	, (SELECT GROUP_CONCAT(KEYWORD) FROM MT_MV_KEYWORD_META WHERE mv_id = MMM.mv_id) as findKywrd
FROM
	MT_MV_META MMM
WHERE
	mv_id in @{STRARR.mvIds}