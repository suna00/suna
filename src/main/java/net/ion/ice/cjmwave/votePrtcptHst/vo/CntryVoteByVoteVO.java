package net.ion.ice.cjmwave.votePrtcptHst.vo;


import java.math.BigDecimal;

public class CntryVoteByVoteVO {

    Long voteSeq;
    String cntryCd;
    BigDecimal voteNum;
    BigDecimal rankNum;

    double voteRate;

    public Long getVoteSeq() {
        return voteSeq;
    }

    public void setVoteSeq(Long voteSeq) {
        this.voteSeq = voteSeq;
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

    @Override
    public int hashCode() {
        if( getVoteSeq() != null && getCntryCd() != null ) return getVoteSeq().hashCode() + getCntryCd().hashCode();
        else return super.hashCode();
    }

    @Override
    public boolean equals(Object tmpCntryVO) {
        if(tmpCntryVO instanceof CntryVoteByVoteVO) {
            CntryVoteByVoteVO artistSexVO2 = (CntryVoteByVoteVO)tmpCntryVO;
            if(this.getVoteSeq() != null && this.getVoteSeq().equals(artistSexVO2.getVoteSeq()) &&
                    this.getCntryCd() != null && this.getCntryCd().equals(artistSexVO2.getCntryCd())) return true;
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