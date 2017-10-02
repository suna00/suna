package net.ion.ice.core.infinispan;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.FacetEncodingType;
import org.hibernate.search.cfg.EntityMapping;
import org.hibernate.search.cfg.FieldMapping;
import org.hibernate.search.cfg.IndexedMapping;
import org.hibernate.search.cfg.SearchMapping;

import java.lang.annotation.ElementType;

public class SearchMappingFactory {
    private final String cacheType ;

    public SearchMappingFactory(String cacheType) {
        this.cacheType = cacheType ;
    }

    public SearchMapping create() {
        SearchMapping mapping = new SearchMapping() ;
        EntityMapping entityMapping = mapping.entity(Node.class);
        IndexedMapping indexedMapping =  entityMapping.indexed() ;
        NodeType nodeType  = NodeUtils.getNodeType(cacheType) ;
        if(nodeType == null)
            return mapping ;

        FieldMapping propertiesMapping = indexedMapping.property("facet", ElementType.FIELD).field().analyze(Analyze.NO) ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            if(!pt.isIndexable()) continue;
            if(pt.isSortable()){
                propertiesMapping.field().analyze(Analyze.NO).name(pt.getPid() + "_sort").analyze(Analyze.NO).facet().name(pt.getPid() + "_sort").encoding(FacetEncodingType.STRING);
            }
        }

        return mapping ;
    }
}
