package net.ion.ice.core.node;


import net.ion.ice.core.data.bind.NodeBindingUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class NodeQuery {

    protected String tid;
    protected NodeType nodeType;
    protected List<String> matchingSearchList = new ArrayList<>();
    protected List<String> notMatchingSearchList = new ArrayList<>();
    protected List<String> aboveSearchList = new ArrayList<>();
    protected List<String> belowSearchList = new ArrayList<>();
    protected List<String> sortingList = new ArrayList<>();

    protected NodeQuery() {}

    public static NodeQuery build(String tid) {
        NodeQuery nodeQuery = new NodeQuery();
        nodeQuery.tid = tid;
        nodeQuery.nodeType = NodeUtils.getNodeType(tid);
        return nodeQuery;
    }

    public NodeQuery matching(String key, String value) {
        if (!StringUtils.isEmpty(key)) {
            String queryKey = String.format("%s_matching", key);
            String queryValue = value == null ? "" : value;
            matchingSearchList.add(queryKey+"="+queryValue);
        }
        return this;
    }

    public NodeQuery notMatching(String key, String value) {
        if (!StringUtils.isEmpty(key)) {
            String queryKey = String.format("%s_notMatching", key);
            String queryValue = value == null ? "" : value;
            notMatchingSearchList.add(queryKey+"="+queryValue);
        }
        return this;
    }

    public NodeQuery above(String key, String value) {
        if (!StringUtils.isEmpty(key)) {
            String queryKey = String.format("%s_above", key);
            String queryValue = value == null ? "" : value;
            aboveSearchList.add(queryKey+"="+queryValue);
        }
        return this;
    }

    public NodeQuery below(String key, String value) {
        if (!StringUtils.isEmpty(key)) {
            String queryKey = String.format("%s_below", key);
            String queryValue = value == null ? "" : value;
            belowSearchList.add(queryKey+"="+queryValue);
        }
        return this;
    }

    public NodeQuery sorting(String... values) {
        if (values != null) {
            for (String value : values) {
                sortingList.add(value);
            }
        }
        return this;
    }

    public List<?> getList() {
        NodeType nodeType = this.nodeType;

        if (nodeType == null) {
            return new ArrayList<>();
        } else {
            List<String> queryTextList = new ArrayList<>();
            queryTextList.addAll(this.matchingSearchList);
            queryTextList.addAll(this.notMatchingSearchList);
            queryTextList.addAll(this.aboveSearchList);
            queryTextList.addAll(this.belowSearchList);
            if (!this.sortingList.isEmpty()) queryTextList.add("sorting="+StringUtils.join(this.sortingList, ","));

            if (StringUtils.equals(nodeType.getRepositoryType(), "node")) {
                return NodeUtils.getNodeList(this.tid, StringUtils.join(queryTextList, "&"));
            } else {
                return NodeBindingUtils.getNodeBindingService().list(this.tid, StringUtils.join(queryTextList, "&"));
            }
        }
    }
}
