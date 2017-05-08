package net.ion.ice.core.json;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;


import static org.junit.Assert.*;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonUtilsTest {
    @Test
    public void contains() throws Exception {
        Map<String, Object> data = makeTestData();

        assertTrue(JsonUtils.contains(data, "key1", "value1")) ;
        assertTrue(JsonUtils.contains(data, "key2", 2)) ;
        assertFalse(JsonUtils.contains(data, "key3", "value3")) ;
        assertFalse(JsonUtils.contains(data, "key2", "value2")) ;
        assertFalse(JsonUtils.contains(data, "key1", 1)) ;

        assertTrue(JsonUtils.contains(data, "list", "list1")) ;
        assertTrue(JsonUtils.contains(data, "list", "list2")) ;
        assertFalse(JsonUtils.contains(data, "list", 2)) ;

        assertFalse(JsonUtils.contains(data, "subKey1", "sub1-value1")) ;
        assertTrue(JsonUtils.contains(data, "sub.subKey1", "sub1-value1")) ;

        assertTrue(JsonUtils.contains(data, "subList.subListKey1", "sub2-value1")) ;
        assertFalse(JsonUtils.contains(data, "subList.subKey1", "sub1-value1")) ;
    }

    private Map<String, Object> makeTestData() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("key1", "value1") ;
        data.put("key2", 2);

        Map<String, Object> subData1 = new LinkedHashMap<String, Object>() ;
        subData1.put("subKey1", "sub1-value1") ;
        subData1.put("subKey2", 2) ;

        data.put("sub", subData1) ;

        List<Object> subList1 = new ArrayList<Object>();
        subList1.add("list1");
        subList1.add("list2");

        data.put("list", subList1) ;

        List<Map<String, Object>> subList2 = new ArrayList<Map<String, Object>>() ;

        Map<String, Object> subListData1 = new LinkedHashMap<String, Object>() ;
        subListData1.put("subListkey1", "sub1-value1") ;
        subListData1.put("subListKey2", 2) ;

        subList2.add(subListData1);

        Map<String, Object> subListData2 = new LinkedHashMap<String, Object>() ;
        subListData2.put("subListKey1", "sub2-value1") ;
        subListData2.put("subListKey2", 4) ;

        subList2.add(subListData2);

        data.put("subList", subList2) ;

        return data;
    }

}