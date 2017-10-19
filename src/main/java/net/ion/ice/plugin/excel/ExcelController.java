package net.ion.ice.plugin.excel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @RequestMapping(value = "/excel/downloadForm.json", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> downloadForm(WebRequest request, HttpServletResponse response) {
        return excelService.downloadForm(request.getParameterMap(), response);
    }
}
