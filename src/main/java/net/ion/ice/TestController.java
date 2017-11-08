package net.ion.ice;

import net.ion.ice.cjmwave.votePrtcptHst.CntryVoteStatsService;
import net.ion.ice.cjmwave.votePrtcptHst.ArtistVoteStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class TestController {

    @Autowired
    ArtistVoteStatsService artistVoteStatsService;

    @Autowired
    CntryVoteStatsService cntryVoteStatsService;

    @RequestMapping("/test/artistSex")
    public ModelAndView artistSex(HttpServletRequest req, HttpServletResponse res) {
        String statDate = req.getParameter("date");
        if( statDate != null ) {
            if( statDate.indexOf(",") != -1 ) {
                String[] sDates = statDate.split(",");
                for( String date: sDates) {
                    artistVoteStatsService.execArtistVoteStatsBySex(null, date);
                }
            }
            else artistVoteStatsService.execArtistVoteStatsBySex(null, statDate);
        }
        else artistVoteStatsService.execArtistVoteStatsBySex(null, null);
        return null;
    }
    @RequestMapping("/test/artistSexWly")
    public ModelAndView artistSexWly() {
        artistVoteStatsService.execArtistVoteStatsBySexWly(null);
        return null;
    }
    @RequestMapping("/test/artistCntry")
    public ModelAndView artistCntry(HttpServletRequest req, HttpServletResponse res) {
        String statDate = req.getParameter("date");
        if( statDate != null ) {
            if( statDate.indexOf(",") != -1 ) {
                String[] sDates = statDate.split(",");
                for( String date: sDates) {
                    artistVoteStatsService.execArtistVoteStatsByCntry(null, date);
                }
            }
            else artistVoteStatsService.execArtistVoteStatsByCntry(null, statDate);
        }
        else artistVoteStatsService.execArtistVoteStatsByCntry(null, null);
        return null;
    }
    @RequestMapping("/test/artistCntryWly")
    public ModelAndView artistCntryWly() {
        artistVoteStatsService.execArtistVoteStatsByCntryWly(null);
        return null;
    }

    @RequestMapping("/test/cntryVote")
    public ModelAndView cntryVote(HttpServletRequest req, HttpServletResponse res) {
        String statDate = req.getParameter("date");
        if( statDate != null ) {
            if( statDate.indexOf(",") != -1 ) {
                String[] sDates = statDate.split(",");
                for( String date: sDates) {
                    cntryVoteStatsService.execCntryVoteStats(null, date);
                }
            }
            else cntryVoteStatsService.execCntryVoteStats(null, statDate);
        }
        else cntryVoteStatsService.execCntryVoteStats(null, null);

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
