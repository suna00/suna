package net.ion.ice.cjmwave.votePrtcptHst;

import java.util.Comparator;

public class ArtistRankingCompare implements Comparator<ArtistRankingItem> {
    @Override
    public int compare(ArtistRankingItem o1, ArtistRankingItem o2) {
        return o2.getVoteCount().compareTo(o1.getVoteCount());
    }
}
