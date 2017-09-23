-- =============================== PROCEDURE ALBUM ==========================================

DROP PROCEDURE ice2.recentAlbum;
CREATE PROCEDURE ice2.recentAlbum
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      MT_ALBUM ALBUM
    WHERE
      CREATE_DT > @LastUpdated OR UPDATE_DT > @LastUpdated;
  END;

  -- TEST
  DECLARE @tmp SMALLDATETIME;
  SET @tmp = GETDATE();
  EXEC dbo.recentAlbum @LastUpdated=@tmp;

  -- =============================== PROCEDURE ALBUM ARTIST ===================================
  DROP PROCEDURE ice2.recentAlbumArtist;
CREATE PROCEDURE ice2.recentAlbumArtist
    @AlbumId int
AS
  BEGIN
    SELECT *
    FROM
      MT_ALBUM_ARTIST
    WHERE
      ALBUM_ID = @AlbumId;
  END ;


  -- =============================== PROCEDURE ALBUM COUNTRY ==================================
  DROP PROCEDURE ice2.recentAlbumCountry;
CREATE PROCEDURE ice2.recentAlbumCountry
    @AlbumId int
AS
  BEGIN
    SELECT *
    FROM
      mt_album_country
    WHERE
      album_id = @AlbumId;
  END;

  -- =============================== PROCEDURE ALBUM GENRE ====================================
  DROP PROCEDURE ice2.recentAlbumGenre;
CREATE PROCEDURE ice2.recentAlbumGenre
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      MT_ALBUM_GENRE
    WHERE
      CREATE_DT > @LastUpdated OR UPDATE_DT > @LastUpdated;
  END;


  -- =============================== PROCEDURE ALBUM KEYWORD =================================
  DROP PROCEDURE ice2.recentAlbumKeyword;
CREATE PROCEDURE ice2.recentAlbumKeyword
    @AlbumId int
AS
  BEGIN
    SELECT *
    FROM
      MT_ALBUM_KEYWORD
    WHERE
      ALBUM_ID = @AlbumId;
  END;


  -- =============================== PROCEDURE ALBUM KEYWORD META ============================
  DROP PROCEDURE ice2.recentAlbumKeywordMeta;
CREATE PROCEDURE ice2.recentAlbumKeywordMeta
    @AlbumId int
AS
  BEGIN
    SELECT *
    FROM
      mt_album_keyword_meta
    WHERE
      album_id = @AlbumId;
  END;


  -- =============================== PROCEDURE ALBUM META ====================================
  DROP PROCEDURE ice2.recentAlbumMeta;
CREATE PROCEDURE ice2.recentAlbumMeta
    @AlbumId int
AS
  BEGIN
    SELECT *
    FROM
      mt_album_meta
    WHERE
      album_id = @AlbumId;
  END ;


  -- =============================== PROCEDURE ALBUM SONGS ===================================
  DROP PROCEDURE ice2.recentAlbumSongs;
CREATE PROCEDURE ice2.recentAlbumSongs
    @AlbumId int
AS
  BEGIN
    SELECT *
    FROM
      MT_ALBUM_SONGS
    WHERE
      ALBUM_ID = @AlbumId;
  END ;


  -- =============================== PROCEDURE ARTIST ========================================
  DROP PROCEDURE ice2.recentArtist;
CREATE PROCEDURE ice2.recentArtist
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      MT_ARTIST
    WHERE
      CREATE_DT > @LastUpdated OR UPDATE_DT > @LastUpdated;
  END ;


  -- =============================== PROCEDURE ARTIST COUNTRY ================================
  DROP PROCEDURE ice2.recentArtistCountry;
CREATE PROCEDURE ice2.recentArtistCountry
    @ArtistId int
AS
  BEGIN
    SELECT *
    FROM
      mt_artist_country
    WHERE
      artist_id = @ArtistId;
  END ;


  -- =============================== PROCEDURE ARTIST GENRE =================================
  DROP PROCEDURE ice2.recentArtistGenre;
CREATE PROCEDURE ice2.recentArtistGenre
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      MT_ARTIST_GENRE
    WHERE
      CREATE_DT > @LastUpdated OR UPDATE_DT > @LastUpdated;
  END ;


  -- =============================== PROCEDURE ARTIST KEYWORD ===============================
  DROP PROCEDURE ice2.recentArtistKeyword;
CREATE PROCEDURE ice2.recentArtistKeyword
    @ArtistId int
AS
  BEGIN
    SELECT *
    FROM
      MT_ARTIST_KEYWORD
    WHERE
      ARTIST_ID > @ArtistId;
  END ;


  -- =============================== PROCEDURE ARTIST KEYWORD META ===========================
  DROP PROCEDURE ice2.recentArtistKeywordMeta;
CREATE PROCEDURE ice2.recentArtistKeywordMeta
    @ArtistId int
AS
  BEGIN
    SELECT *
    FROM
      mt_artist_keyword_meta
    WHERE
      artist_id = @ArtistId;
  END ;

  -- =============================== PROCEDURE ARTIST META =============-=====================
  DROP PROCEDURE ice2.recentArtistMeta;
CREATE PROCEDURE ice2.recentArtistMeta
    @ArtistId int
AS
  BEGIN
    SELECT *
    FROM
      mt_artist_meta
    WHERE
      artist_id = @ArtistId;
  END ;


  -- =============================== PROCEDURE MV ============================================
  DROP PROCEDURE ice2.recentMv;
CREATE PROCEDURE ice2.recentMv
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      MT_MV
    WHERE
      CREATE_DT > @LastUpdated OR UPDATE_DT > @LastUpdated;
  END ;


  -- =============================== PROCEDURE MV ARTIST =====================================
  DROP PROCEDURE ice2.recentMvArtist;
CREATE PROCEDURE ice2.recentMvArtist
    @MvId int
AS
  BEGIN
    SELECT *
    FROM
      MT_MV_ARTIST
    WHERE
      MV_ID = @MvId;
  END ;


  -- =============================== PROCEDURE MV COUNTRY =====================================
  DROP PROCEDURE ice2.recentMvCountry;
CREATE PROCEDURE ice2.recentMvCountry
    @MvId int
AS
  BEGIN
    SELECT *
    FROM
      mt_mv_country
    WHERE
      mv_id = @MvId;
  END ;


  -- =============================== PROCEDURE MV EMBED =======================================
  DROP PROCEDURE ice2.recentMvEmbed;
CREATE PROCEDURE ice2.recentMvEmbed
    @MvId int
AS
  BEGIN
    SELECT *
    FROM
      MT_MV_EMBED
    WHERE
      MV_ID = @MvId;
  END ;


  -- =============================== PROCEDURE MV KEYWORD =====================================
  DROP PROCEDURE ice2.recentMvKeyword;
CREATE PROCEDURE ice2.recentMvKeyword
    @MvId int
AS
  BEGIN
    SELECT *
    FROM
      MT_MV_KEYWORD
    WHERE
      MV_ID = @MvId;
  END ;


  -- =============================== PROCEDURE MV KEYWORD META =================================
  DROP PROCEDURE ice2.recentMvKeywordMeta;
CREATE PROCEDURE ice2.recentMvKeywordMeta
    @MvId int
AS
  BEGIN
    SELECT *
    FROM
      mt_mv_keyword_meta
    WHERE
      mv_id = @MvId;
  END ;


  -- =============================== PROCEDURE MV META =========================================
  DROP PROCEDURE ice2.recentMvMeta;
CREATE PROCEDURE ice2.recentMvMeta
    @MvId int
AS
  BEGIN
    SELECT *
    FROM
      mt_mv_meta
    WHERE
      mv_id = @MvId;
  END ;


  -- =============================== PROCEDURE SONG ============================================
  DROP PROCEDURE ice2.recentSong;
CREATE PROCEDURE ice2.recentSong
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      MT_SONG
    WHERE
      CREATE_DT > @LastUpdated OR UPDATE_DT > @LastUpdated;
  END ;


  -- =============================== PROCEDURE SONG ARTIST =====================================
  DROP PROCEDURE ice2.recentSongArtist;
CREATE PROCEDURE ice2.recentSongArtist
    @SongId int
AS
  BEGIN
    SELECT *
    FROM
      MT_SONG_ARTIST
    WHERE
      SONG_ID = @SongId;
  END ;

  /*
  MT_SONG_ALBUM 누락
  */
  -- =============================== PROCEDURE SONG ALBUM ======================================
  DROP PROCEDURE ice2.recentSongAlbum;
CREATE PROCEDURE ice2.recentSongAlbum
    @SongId int
AS
  BEGIN
    SELECT *
    FROM
      MT_SONG_ALBUM
    WHERE
      SONG_ID = @SongId;
  END ;


  -- =============================== PROCEDURE SONG COUNTRY ====================================
  DROP PROCEDURE ice2.recentSongCountry;
CREATE PROCEDURE ice2.recentSongCountry
    @SongId int
AS
  BEGIN
    SELECT *
    FROM
      mt_song_country
    WHERE
      song_id = @SongId;
  END ;


  -- =============================== PROCEDURE SONG KEYWORD ====================================
  DROP PROCEDURE ice2.recentSongKeyword;
CREATE PROCEDURE ice2.recentSongKeyword
    @SongId int
AS
  BEGIN
    SELECT *
    FROM
      MT_SONG_KEYWORD
    WHERE
      SONG_ID = @SongId;
  END ;


  -- =============================== PROCEDURE SONG KEYWORD META ===============================
  DROP PROCEDURE ice2.recentSongKeywordMeta;
CREATE PROCEDURE ice2.recentSongKeywordMeta
    @SongId int
AS
  BEGIN
    SELECT *
    FROM
      mt_song_keyword_meta
    WHERE
      song_id = @SongId;
  END ;


  -- =============================== PROCEDURE SONG LYRIC ======================================
  DROP PROCEDURE ice2.recentSongLyric;
CREATE PROCEDURE ice2.recentSongLyric
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      MT_SONG_LYRIC
    WHERE
      CREATE_DT > @LastUpdated OR UPDATE_DT > @LastUpdated;
  END ;


  -- =============================== PROCEDURE SONG META =======================================
  DROP PROCEDURE ice2.recentSongMeta;
CREATE PROCEDURE ice2.recentSongMeta
    @SongId int
AS
  BEGIN
    SELECT *
    FROM
      mt_song_meta
    WHERE
      song_id = @SongId;
  END ;


  -- =============================== PROCEDURE CHART TOTAL MASTER ==============================
  DROP PROCEDURE ice2.recentChartTotalMst;
CREATE PROCEDURE ice2.recentChartTotalMst
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      T_SM_S3_ChartTotalMst
    WHERE
      RegDate > @LastUpdated OR ModDate > @LastUpdated;
  END ;


  -- =============================== PROCEDURE CHHART TOTAL LIST ===============================
  DROP PROCEDURE ice2.recentAlbumChartTotalLst;
CREATE PROCEDURE ice2.recentAlbumChartTotalLst
    @LastUpdated smalldatetime
AS
  BEGIN
    SELECT *
    FROM
      T_SM_S3_ChartTotalMst
    WHERE
      RegDate > @LastUpdated;
  END ;


  COMMIT ;

  SELECT * FROM MT_ARTIST;
