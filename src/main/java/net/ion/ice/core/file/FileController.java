package net.ion.ice.core.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by jaeho on 2017. 6. 30..
 */
@Controller
public class FileController {

    @Autowired
    private FileService fileService ;



    @GetMapping("/file/{tid}/{pid}/{year}/{day}/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String tid, @PathVariable String pid, @PathVariable String year, @PathVariable String day, @PathVariable String filename) {

        Resource file = fileService.loadAsResource(tid, pid, tid + "/" + pid + "/" +  year + "/" + day + "/" + filename);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+file.getFilename()+"\"")
                .body(file);
    }

    @GetMapping("/image/{tid}/{pid}/{year}/{day}/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveImage(@PathVariable String tid, @PathVariable String pid, @PathVariable String year, @PathVariable String day, @PathVariable String filename) {
        Resource file = fileService.loadAsResource(tid, pid, tid + "/" + pid + "/" +  year + "/" + day + "/" + filename);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, FileUtils.getContentType(filename))
                .body(file);
    }

    @GetMapping("/{tid}/{pid}/{year}/{day}/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile2(@PathVariable String tid, @PathVariable String pid, @PathVariable String year, @PathVariable String day, @PathVariable String filename) {
        Resource file = fileService.loadAsResource(tid, pid, tid + "/" + pid + "/" +  year + "/" + day + "/" + filename);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, FileUtils.getContentType(filename))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+file.getFilename()+"\"")
                .body(file);
    }

}
