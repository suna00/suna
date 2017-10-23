package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.data.bind.NodeBindingUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service("searchWordService")
public class SearchWordService {

    public ExecuteContext countList(ExecuteContext context) {
        Map<String, Object> paramData = context.getData();
        String page = paramData.get("page") == null ? "1" : paramData.get("page").toString();
        String pageSize = paramData.get("pageSize") == null ? "10" : paramData.get("pageSize").toString();
        String createdAbove = paramData.get("created_above") == null ? "" : paramData.get("created_above").toString();
        String createdBelow = paramData.get("created_below") == null ? "" : paramData.get("created_below").toString();

        NodeBindingService nodeBindingService = NodeBindingUtils.getNodeBindingService();
        NodeBindingInfo nodeBindingInfo = nodeBindingService.getNodeBindingInfo("searchWordHistory");
        JdbcTemplate jdbcTemplate = nodeBindingInfo.getJdbcTemplate();


        StringBuffer query = new StringBuffer();
        query.append("SELECT name, COUNT(name) cnt ");
        query.append("FROM searchwordhistory ");
        if (!StringUtils.isEmpty(createdAbove) && StringUtils.isEmpty(createdBelow)) {
            query.append("WHERE created >= DATE_FORMAT('"+createdAbove+"', '%Y-%m-%d') ");
        } else if (!StringUtils.isEmpty(createdAbove) && !StringUtils.isEmpty(createdBelow)) {
            query.append("WHERE created >= DATE_FORMAT('"+createdAbove+"', '%Y-%m-%d') ");
            query.append("AND created < DATE_ADD(DATE_FORMAT('"+createdBelow+"', '%Y-%m-%d'), INTERVAL 1 DAY) ");
        } else if (StringUtils.isEmpty(createdAbove) && !StringUtils.isEmpty(createdBelow)) {
            query.append("WHERE created < DATE_ADD(DATE_FORMAT('"+createdBelow+"', '%Y-%m-%d'), INTERVAL 1 DAY) ");
        }
        query.append("GROUP BY name ");
        query.append("ORDER BY cnt DESC ");

        StringBuffer totalQuery = new StringBuffer();
        totalQuery.append("SELECT COUNT(t.name) ");
        totalQuery.append("FROM ("+query.toString()+") t ");

        Integer totalCount = jdbcTemplate.queryForObject(totalQuery.toString(), Integer.class);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(query.toString());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("totalCount", totalCount);
        item.put("resultCount", list.size());
        item.put("pageSize", Integer.parseInt(pageSize));
        item.put("pageCount", (int) Math.ceil(totalCount / Integer.parseInt(pageSize)));
        item.put("currentPage", Integer.parseInt(page));
        item.put("items", list);

        context.setResult(item);

        return context;
    }

}
