package net.ion.ice.core.query;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.query.facet.Facet;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

public class FacetTerm {
    private String field ;
    private List<String> rangeList;
    private List<Facet> facets;
    private String fieldName;

    public FacetTerm(String field, Object value) {
        this.field = field ;
        this.fieldName = field ;
        if(value != null){
            rangeList = new ArrayList<>() ;
            for(String range : StringUtils.split(value.toString(), ",")){
                rangeList.add(range.trim()) ;
            }
        }
    }


    public String getFieldName() {
        return fieldName;
    }

    public String getName() {
        return field;
    }

    public boolean isDiscrete() {
        return rangeList == null || rangeList.size() == 0;
    }

    public List<String> getRangeList() {
        return rangeList;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public List<Facet> getFacets() {
        return facets;
    }
}
