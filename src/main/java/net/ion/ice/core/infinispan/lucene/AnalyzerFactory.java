package net.ion.ice.core.infinispan.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 5. 16..
 */
public class AnalyzerFactory {

    private static Map<String, Analyzer> analyzerMap = new ConcurrentHashMap<>() ;

    public static Analyzer getAnalyzer(String analyzer){
        if(!analyzerMap.containsKey(analyzer)){
            Analyzer inst = null ;
            switch (analyzer){
                case "simple" :{
                    inst = new SimpleAnalyzer() ;
                    break ;
                }
                case "code" :{
                    inst = new CodeAnalyzer() ;
                    break ;
                }
                case "whitespace" :{
                    inst = new WhitespaceAnalyzer() ;
                    break ;
                }
                case "standard" :{
                    inst = new StandardAnalyzer() ;
                    break ;
                }
                case "cjk" :{
                    inst = new CJKAnalyzer() ;
                    break ;
                }
                case "korean" :{
//                    inst = new KoreanAnalyzer() ;
                    break ;
                }
                default :
                    inst = new SimpleAnalyzer() ;
            }
            analyzerMap.put(analyzer, inst) ;
        }

        return analyzerMap.get(analyzer) ;
    }
}
