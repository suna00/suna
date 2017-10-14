DROP PROCEDURE retreiveSingleArtist;
GO
CREATE PROCEDURE retreiveSingleArtist
    @ArtistId int
AS
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
  BEGIN
	SELECT *
	FROM
	  MT_ARTIST
	WHERE
	  ARTIST_ID = @ArtistId;
	OPTION (MAXDOP 1);  
END ;
GO

DROP PROCEDURE retreiveSingleAlbum;
GO
CREATE PROCEDURE retreiveSingleAlbum
    @AlbumId int
AS
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
  BEGIN
	SELECT *
	FROM
	  MT_ALBUM
	WHERE
	  ALBUM_ID = @AlbumId;
    OPTION (MAXDOP 1);
END ;
GO


DROP PROCEDURE retreiveSingleMusicVideo;
GO
CREATE PROCEDURE retreiveSingleMusicVideo
    @MvId int
AS
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
  BEGIN
    SELECT *
    FROM
      MT_MV
    WHERE
      MV_ID = @MvId;
    OPTION (MAXDOP 1);  
END ;
GO


DROP PROCEDURE retreiveSingleSong;
GO
CREATE PROCEDURE retreiveSingleSong
    @SongId int
AS
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
  BEGIN
    SELECT *
    FROM
      MT_SONG
    WHERE
      SONG_ID = @SongId;
    OPTION (MAXDOP 1);  
END ;
GO

-- =============================================================================================


DROP PROCEDURE retreiveMultiArtist;
GO
CREATE PROCEDURE retreiveMultiArtist
    @ArtistId1 int
    @ArtistId2 int
    @ArtistId3 int
    @ArtistId4 int
    @ArtistId5 int
    @ArtistId6 int
    @ArtistId7 int
    @ArtistId8 int
    @ArtistId9 int
    @ArtistId10 int
AS
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
  BEGIN
	SELECT * FROM MT_ARTIST WHERE ARTIST_ID IN (@ArtistId1, @ArtistId2, @ArtistId3, @ArtistId4, @ArtistId5, @ArtistId6, @ArtistId7, @ArtistId8, @ArtistId9, @ArtistId10);
  OPTION (MAXDOP 1); 	
END ;
GO


DROP PROCEDURE retreiveMultiAlbum;
GO
CREATE PROCEDURE retreiveMultiAlbum
    @AlbumId1 int
    @AlbumId2 int
    @AlbumId3 int
    @AlbumId4 int
    @AlbumId5 int
    @AlbumId6 int
    @AlbumId7 int
    @AlbumId8 int
    @AlbumId9 int
    @AlbumId10 int
AS
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
  BEGIN
  	SELECT * FROM MT_ALBUM WHERE MT_ALBUM IN (@AlbumId1, @AlbumId2, @AlbumId3, @AlbumId4, @AlbumId5, @AlbumId6, @AlbumId7, @AlbumId8, @AlbumId9, @AlbumId10);
  OPTION (MAXDOP 1);
END ;
GO


DROP PROCEDURE retreiveMultiMusicVideo;
GO
CREATE PROCEDURE retreiveMultiMusicVideo
    @MvId1 int
    @MvId2 int
    @MvId3 int
    @MvId4 int
    @MvId5 int
    @MvId6 int
    @MvId7 int
    @MvId8 int
    @MvId9 int
    @MvId10 int
AS
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
  BEGIN
  	SELECT * FROM MT_MV WHERE MV_ID IN (@MvId1, @MvId2, @MvId3, @MvId4, @MvId5, @MvId6, @MvId7, @MvId8, @MvId9, @MvId10);
  OPTION (MAXDOP 1);	
END ;
GO


DROP PROCEDURE retreiveMultiSong;
GO
CREATE PROCEDURE retreiveMultiSong
    @SongId1 int
    @SongId2 int
    @SongId3 int
    @SongId4 int
    @SongId5 int
    @SongId6 int
    @SongId7 int
    @SongId8 int
    @SongId9 int
    @SongId10 int
AS
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED
  BEGIN
    SELECT * FROM MT_SONG WHERE SONG_ID IN (@SongId1, @SongId2, @SongId3, @SongId4, @SongId5, @SongId6, @SongId7, @SongId8, @SongId9, @SongId10);
  OPTION (MAXDOP 1);  
END ;
GO