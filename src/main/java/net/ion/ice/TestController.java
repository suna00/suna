package net.ion.ice;

import net.ion.ice.cjmwave.votePrtcptHst.ArtistVoteStatsTask;
import net.ion.ice.cjmwave.votePrtcptHst.CntryVoteStatsService;
import net.ion.ice.cjmwave.votePrtcptHst.ArtistVoteStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class TestController {
    private static Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    ArtistVoteStatsService artistVoteStatsService;

    @Autowired
    CntryVoteStatsService cntryVoteStatsService;

    @Autowired
    private ArtistVoteStatsTask artistVoteStatsTask;


    @RequestMapping("/test/artistStat")
    @ResponseBody
    public String runArtistVoteStats(@RequestParam(name = "date", required = false) String dateString) {
//        logger.info(dateString);
        artistVoteStatsTask.artistVoteStatsJob(dateString);
        return "200";
    }


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
    public ModelAndView artistSexWly(HttpServletRequest req, HttpServletResponse res) {
        String sDate = req.getParameter("sdate");
        String eDate = req.getParameter("edate");
        if(sDate != null) artistVoteStatsService.execArtistVoteStatsBySexWly(null, sDate, eDate);
        else artistVoteStatsService.execArtistVoteStatsBySexWly(null, null, null);
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
    public ModelAndView artistCntryWly(HttpServletRequest req, HttpServletResponse res) {
        String sDate = req.getParameter("sdate");
        String eDate = req.getParameter("edate");
        if(sDate != null) artistVoteStatsService.execArtistVoteStatsByCntryWly(null, sDate, eDate);
        else artistVoteStatsService.execArtistVoteStatsByCntryWly(null, null, null);
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
    public ModelAndView cntryVoteWly(HttpServletRequest req, HttpServletResponse res) {
        String sDate = req.getParameter("sdate");
        String eDate = req.getParameter("edate");
        if(sDate != null) cntryVoteStatsService.execCntryVoteStatsWly(null, sDate, eDate);
        else cntryVoteStatsService.execCntryVoteStatsWly(null, null, null);
        return null;
    }
    @RequestMapping("/test/cntryVoteByVoteWly")
    public ModelAndView cntryVoteCntry(HttpServletRequest req, HttpServletResponse res) {
        String sDate = req.getParameter("sdate");
        String eDate = req.getParameter("edate");
        if(sDate != null) cntryVoteStatsService.execCntryVoteStatsByVoteWly(null, sDate, eDate);
        else cntryVoteStatsService.execCntryVoteStatsByVoteWly(null, null, null);
        return null;
    }

}
