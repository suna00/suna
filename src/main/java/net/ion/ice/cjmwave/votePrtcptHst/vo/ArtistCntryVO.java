package net.ion.ice.cjmwave.votePrtcptHst.vo;

import java.math.BigDecimal;

public class ArtistCntryVO {

    String artistId;
    //    String sexCd;
    String cntryCd;
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

    public String getCntryCd() {
        return cntryCd;
    }

    public void setCntryCd(String cntryCd) {
        this.cntryCd = cntryCd;
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
        if (getArtistId() != null && getCntryCd() != null) return getArtistId().hashCode() + getCntryCd().hashCode();
        else {
            if (getArtistId() == null && getCntryCd() == null) return super.hashCode();
            else if (getArtistId() != null) return getArtistId().hashCode();
            else if (getCntryCd() != null) return getCntryCd().hashCode();
            else return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object artistSexVO) {
        if (artistSexVO instanceof ArtistCntryVO) {
            ArtistCntryVO artistSexVO2 = (ArtistCntryVO) artistSexVO;
            if (this.getArtistId() != null && this.getCntryCd() != null &&
                    this.getArtistId().equals(artistSexVO2.getArtistId()) && this.getCntryCd().equals(artistSexVO2.getCntryCd())
                    ) return true;
            else return false;
        }
        return false;
    }

    public String toString() {
        return ""
                + " [artistId]" + artistId
                + " [cntryCd]" + cntryCd
                + " [voteNum]" + voteNum
                + " [rankNum]" + rankNum
                + " [voteRate]" + voteRate
                ;
    }
}