package net.ion.ice.plugin.excel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @RequestMapping(value = "/excel/downloadForm.json", method = RequestMethod.GET)
    public void downloadForm(HttpServletRequest request, HttpServletResponse response) {
        excelService.downloadForm(request, response);
    }
}
