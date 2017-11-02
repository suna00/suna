package net.ion.ice.cjmwave.votePrtcptHst.vo;

import java.math.BigDecimal;
import java.util.Date;

public class ArtistSexVO {

    String artistId;
    String sexCd;
    java.math.BigDecimal voteNum;
    java.math.BigDecimal rankNum;
    double voteRate;
    String voteStart;
    String voteEnd;


    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getSexCd() {
        return sexCd;
    }

    public void setSexCd(String sexCd) {
        this.sexCd = sexCd;
    }

    public BigDecimal getVoteNum() {
        return voteNum;
    }

    public void setVoteNum(BigDecimal voteNum) {
        this.voteNum = voteNum;
    }

    public BigDecimal getRankNum() {
        return rankNum;
    }

    public void setRankNum(BigDecimal rankNum) {
        this.rankNum = rankNum;
    }

    public double getVoteRate() {
        return voteRate;
    }

    public void setVoteRate(double voteRate) {
        this.voteRate = voteRate;
    }

    public String getVoteStart() {
        return voteStart;
    }

    public void setVoteStart(String voteStart) {
        this.voteStart = voteStart;
    }

    public String getVoteEnd() {
        return voteEnd;
    }

    public void setVoteEnd(String voteEnd) {
        this.voteEnd = voteEnd;
    }

    @Override
    public int hashCode() {
        if( getArtistId() != null && getSexCd() != null ) return getArtistId().hashCode() + getSexCd().hashCode();
        else {
            if( getArtistId() == null && getSexCd() == null ) return super.hashCode();
            else if( getArtistId() != null ) return getArtistId().hashCode();
            else if( getSexCd() != null ) return getSexCd().hashCode();
            else return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object artistSexVO) {
        if(artistSexVO instanceof ArtistSexVO ) {
            ArtistSexVO artistSexVO2 = (ArtistSexVO)artistSexVO;
            if( this.getArtistId() != null && this.getSexCd() != null &&
                    this.getArtistId().equals(artistSexVO2.getArtistId()) && this.getSexCd().equals(artistSexVO2.getSexCd())
                    ) return true;
            else return false;
        }
        return false;
    }

    public String toString() {
        return     ""
                + " [artistId]" + artistId
                + " [sexCd]" + sexCd
                + " [voteNum]" + voteNum
                + " [rankNum]" + rankNum
                + " [voteRate]" + voteRate
                ;
    }

}
