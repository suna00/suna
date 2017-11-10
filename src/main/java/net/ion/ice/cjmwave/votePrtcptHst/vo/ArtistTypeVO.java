package net.ion.ice.cjmwave.votePrtcptHst.vo;

import java.math.BigDecimal;

public class ArtistTypeVO {

    String artistId;
    //    String sexCd;
    String typeCd;
    BigDecimal voteNum;
    BigDecimal rankNum;

    double voteRate;
    String voteStart;
    String voteEnd;


    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getTypeCd() {
        return typeCd;
    }

    public void setTypeCd(String typeCd) {
        this.typeCd = typeCd;
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
        if (getArtistId() != null && getTypeCd() != null) return getArtistId().hashCode() + getTypeCd().hashCode();
        else {
            if (getArtistId() == null && getTypeCd() == null) return super.hashCode();
            else if (getArtistId() != null) return getArtistId().hashCode();
            else if (getTypeCd() != null) return getTypeCd().hashCode();
            else return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object artistSexVO) {
        if (artistSexVO instanceof ArtistTypeVO) {
            ArtistTypeVO artistSexVO2 = (ArtistTypeVO) artistSexVO;
            if (this.getArtistId() != null && this.getTypeCd() != null &&
                    this.getArtistId().equals(artistSexVO2.getArtistId()) && this.getTypeCd().equals(artistSexVO2.getTypeCd())
                    ) return true;
            else return false;
        }
        return false;
    }

    public String toString() {
        return ""
                + " [artistId]" + artistId
                + " [typeCd]" + typeCd
                + " [voteNum]" + voteNum
                + " [rankNum]" + rankNum
                + " [voteRate]" + voteRate
                ;
    }
}