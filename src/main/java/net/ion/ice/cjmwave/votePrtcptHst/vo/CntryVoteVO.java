package net.ion.ice.cjmwave.votePrtcptHst.vo;


import java.math.BigDecimal;

public class CntryVoteVO {

    String cntryCd;
    BigDecimal voteNum;
    BigDecimal rankNum;

    double voteRate;
    String voteStart;
    String voteEnd;

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
        if( getCntryCd() != null ) return getCntryCd().hashCode();
        else return super.hashCode();
    }

    @Override
    public boolean equals(Object tmpCntryVO) {
        if(tmpCntryVO instanceof CntryVoteVO) {
            CntryVoteVO artistSexVO2 = (CntryVoteVO)tmpCntryVO;
            if( this.getCntryCd() != null && this.getCntryCd().equals(artistSexVO2.getCntryCd())) return true;
            else return false;
        }
        return false;
    }

    public String toString() {
        return     ""
                + " [cntryCd]" + cntryCd
                + " [voteNum]" + voteNum
                + " [rankNum]" + rankNum
                + " [voteRate]" + voteRate
                ;
    }
}
