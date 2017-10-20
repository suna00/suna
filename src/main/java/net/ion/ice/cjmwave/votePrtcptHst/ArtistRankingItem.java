package net.ion.ice.cjmwave.votePrtcptHst;

import java.io.Serializable;

public class ArtistRankingItem implements Serializable{
    
    private String artistId ;
    private Integer voteCount ;


    public ArtistRankingItem(String artistId, Integer artistCount) {
        this.artistId = artistId ;
        this.voteCount = artistCount ;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

    public String getArtistId() {
        return artistId;
    }
}
