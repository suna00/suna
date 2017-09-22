package net.ion.ice.core.node;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("nodeHelperService")
public class NodeHelperService  {
    private Logger logger = LoggerFactory.getLogger(NodeHelperService.class);

    @Autowired
    private NodeService nodeService ;

    public void reloadSchema(String resourcePath) throws IOException {
        if(resourcePath.equals("node")){
            saveSchema("classpath:schema/node/**/*.json");
        }else if(resourcePath.equals("test")){
            saveSchema("classpath:schema/test/**/*.json");
        }else {
            saveSchema(resourcePath);
        }
    }

    private void saveSchema(String resourcePath) throws IOException {
        File[] files = new File(resourcePath).listFiles();
        for (File file : files) {
            if (file.getName().equals("nodeType.json")) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if (file.getName().equals("propertyType.json")) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if (file.getName().equals("event.json")) {
                fileNodeSave(file);
            }
        }
        for (File file : files) {
            if (file.getName().endsWith(".json") && !(file.getName().equals("nodeType.json") || file.getName().equals("propertyType.json") || file.getName().equals("event.json"))) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if(file.isDirectory()){
                saveSchema(file.getAbsolutePath());
            }
        }
    }

    private void fileNodeSave(File file) throws IOException {
        Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonFileToList(file) ;
        nodeDataList.forEach(data -> nodeService.saveNode(data));
    }
}
