package net.ion.ice;

import net.ion.ice.cjmwave.votePrtcptHst.CntryVoteStatsService;
import net.ion.ice.cjmwave.votePrtcptHst.ArtistVoteStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TestController {

    @Autowired
    ArtistVoteStatsService artistVoteStatsService;

    @Autowired
    CntryVoteStatsService cntryVoteStatsService;

    @RequestMapping("/test/artistSex")
    public ModelAndView artistSex() {
        artistVoteStatsService.execArtistVoteStatsBySex(null);
        return null;
    }
    @RequestMapping("/test/artistSexWly")
    public ModelAndView artistSexWly() {
        artistVoteStatsService.execArtistVoteStatsBySexWly(null);
        return null;
    }
    @RequestMapping("/test/artistCntry")
    public ModelAndView artistCntry() {
        artistVoteStatsService.execArtistVoteStatsByCntry(null);
        return null;
    }
    @RequestMapping("/test/artistCntryWly")
    public ModelAndView artistCntryWly() {
        artistVoteStatsService.execArtistVoteStatsByCntryWly(null);
        return null;
    }

    @RequestMapping("/test/cntryVote")
    public ModelAndView cntryVote() {
        cntryVoteStatsService.execCntryVoteStats(null);
        return null;
    }
    @RequestMapping("/test/cntryVoteWly")
    public ModelAndView cntryVoteWly() {
        cntryVoteStatsService.execCntryVoteStatsWly(null);
        return null;
    }
    @RequestMapping("/test/cntryVoteByVoteWly")
    public ModelAndView cntryVoteCntry() {
        cntryVoteStatsService.execCntryVoteStatsByVoteWly(null);
        return null;
    }

}
