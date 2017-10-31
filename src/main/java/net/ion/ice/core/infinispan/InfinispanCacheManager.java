package net.ion.ice.core.infinispan;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeHistory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.cfg.Environment;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.spi.ProgrammaticSearchMappingProvider;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * Created by jaeho on 2017. 3. 31..
 */

@Configuration
@ConfigurationProperties(prefix = "infinispan")
public class InfinispanCacheManager {
    private static Logger logger = LoggerFactory.getLogger(InfinispanCacheManager.class);

    private static GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder().globalJmxStatistics().allowDuplicateDomains(true).build();
    private static EmbeddedCacheManager manager = new DefaultCacheManager(globalConfiguration);

    private String cachePath ;


    private synchronized <K,V> Cache<K,V> cacheInit(String cacheType, long size) {
        Properties properties = new Properties() ;

        properties.put(Environment.MODEL_MAPPING, new SearchMappingFactory(cacheType).create()) ;
        properties.put(Environment.INDEX_METADATA_COMPLETE, "false");
        properties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
        properties.put("hibernate.search.default.directory_provider", "filesystem");
        properties.put(Environment.EXCLUSIVE_INDEX_USE, "true");
        properties.put("hibernate.search.default.indexwriter.merge_factor", "5");
        properties.put("hibernate.search.default.indexwriter.ram_buffer_size", "20");
        properties.put(Environment.INDEX_UNINVERTING_ALLOWED, "true") ;
        properties.put("hibernate.search.default.indexBase", (cachePath.endsWith("/") ? StringUtils.substringBeforeLast(cachePath, "/") : cachePath) + "/" + cacheType) ;


        synchronized (cacheType) {
            manager.defineConfiguration(cacheType, new ConfigurationBuilder()
//                .invocationBatching().enable() //마이그 실행시 주석 제거
                    .eviction().strategy(EvictionStrategy.LRU).size(size)
                    .persistence().passivation(false)
                    .addSingleFileStore()
                    .preload(true)
                    .shared(false)
                    .fetchPersistentState(true)
                    .purgeOnStartup(false)
                    .ignoreModifications(false)
                    .location(cachePath)
//                .transaction().transactionMode(TransactionMode.TRANSACTIONAL).lockingMode(LockingMode.OPTIMISTIC) //마이그 실행시 주석 제거
                    .transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL) //마이그 실행시 주석
                    .indexing().index(Index.LOCAL)
                    .withProperties(properties)
                    .build());

            Cache<K, V> cache = manager.getCache(cacheType);
            cache.start();
            return cache;
        }
    }


    public void setCachePath(String cachePath) {
        this.cachePath = cachePath ;
    }


    public <K,V> Cache<K,V> getCache(String cacheType, int size) {
        Cache<K,V> cache = manager.getCache(cacheType, false) ;
        if(cache == null){
            cache = cacheInit(cacheType, size) ;
        }
        return cache;
    }

    public Cache<String, NodeHistory> getHistoryCache(String cacheType, int size){
        Cache<String, NodeHistory> cache = manager.getCache(cacheType + "_history", false) ;
        if(cache == null){
            cache = cacheInit(cacheType, size) ;
        }
        return cache;
    }
    
    private static String[] times = new String[]{"10", "30", "60", "300", "3600"} ;


    @PostConstruct
    public void initLocalCache(){
        for(String time : times){
            getLocalCache(time) ;
        }
    }

    private static <K,V> Cache<K,V> initLocalCache(String time){
        long idle = Integer.parseInt(time) * 1000L ;
        manager.defineConfiguration(time, new ConfigurationBuilder()
                .eviction().strategy(EvictionStrategy.LIRS).maxEntries(5000).expiration().wakeUpInterval(30001).lifespan(idle).maxIdle(idle)
                .build());
        Cache<K, V> cache = manager.getCache(time);
        cache.start();

        return cache ;
    }


    public static <K,V> Cache<K,V> getLocalCache(String time) {
        Cache<K,V> cache = manager.getCache(time, false) ;
        if(cache == null){
            cache = initLocalCache(time) ;
        }
        return cache;
    }
    
    
}
