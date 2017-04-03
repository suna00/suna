package net.ion.ice.infinispan;

import net.ion.ice.node.Node;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Created by jaeho on 2017. 3. 31..
 */

@Configuration
@ConfigurationProperties(prefix = "infinispan")
public class InfinispanCacheManager {
    private static Logger logger = LoggerFactory.getLogger(InfinispanCacheManager.class);

    private GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder().globalJmxStatistics().allowDuplicateDomains(true).build();
    private EmbeddedCacheManager manager = new DefaultCacheManager(globalConfiguration);

    private String cachePath ;


    private synchronized <K,V> Cache<K,V> cacheInit(String cacheType, long size) {
        manager.defineConfiguration(cacheType, new ConfigurationBuilder()
                .invocationBatching().enable() //마이그 실행시 주석 제거
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
//                .transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL) //마이그 실행시 주석
                .indexing().index(Index.LOCAL)
                .addProperty("hibernate.search.lucene_version", "LUCENE_CURRENT")
                .addProperty("hibernate.search.default.directory_provider", "filesystem")
                .addProperty("hibernate.search.default.exclusive_index_use", "true")
                .addProperty("hibernate.search.default.indexwriter.merge_factor", "5")
                .addProperty("hibernate.search.default.indexwriter.ram_buffer_size", "20")
                .addProperty("hibernate.search.default.indexBase", (cachePath.endsWith("/") ? StringUtils.substringBeforeLast(cachePath, "/") : cachePath) + "/" + cacheType)
                .build());

        Cache<K, V> cache = manager.getCache(cacheType);
        cache.start();
        return cache ;
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
}
