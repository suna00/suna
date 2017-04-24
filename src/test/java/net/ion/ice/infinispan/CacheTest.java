package net.ion.ice.infinispan;

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
import org.junit.Test;

/**
 * Created by jaeho on 2017. 4. 21..
 */
public class CacheTest {
    private GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder().globalJmxStatistics().allowDuplicateDomains(true).build();
    private EmbeddedCacheManager manager = new DefaultCacheManager(globalConfiguration);

    @Test
    public void beanCache(){
        manager.defineConfiguration("cacheTest", new ConfigurationBuilder()
//                .invocationBatching().enable() //마이그 실행시 주석 제거
                .eviction().strategy(EvictionStrategy.LRU).size(1000)
                .persistence().passivation(false)
                .addSingleFileStore()
                .preload(true)
                .shared(false)
                .fetchPersistentState(true)
                .purgeOnStartup(false)
                .ignoreModifications(false)
                .location("/resource/cache")
//                .transaction().transactionMode(TransactionMode.TRANSACTIONAL).lockingMode(LockingMode.OPTIMISTIC) //마이그 실행시 주석 제거
                .transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL) //마이그 실행시 주석
                .indexing().index(Index.LOCAL)
                .addProperty("hibernate.search.lucene_version", "LUCENE_CURRENT")
                .addProperty("hibernate.search.default.directory_provider", "filesystem")
                .addProperty("hibernate.search.default.exclusive_index_use", "true")
                .addProperty("hibernate.search.default.indexwriter.merge_factor", "5")
                .addProperty("hibernate.search.default.indexwriter.ram_buffer_size", "20")
                .addProperty("hibernate.search.default.indexBase", ("/resource/cache".endsWith("/") ? StringUtils.substringBeforeLast("/resource/cache", "/") : "/resource/cache") + "/" + "cacheTest")
                .build());

        Cache<String, CacheTestData> cache = manager.getCache("cacheTest");
        cache.start();


        CacheTestData data1 = new CacheTestData() ;
        data1.setId("data1");
        data1.setString1("string1");
        data1.setNumber1(100L);
        data1.setValue1("value1");

        cache.put("data1", data1) ;


        CacheTestData data2 = new CacheTestData() ;
        data2.setId("data2");
        data2.setText1("text2");
        data2.setString1("string2");
        data2.setNumber1(200L);
        data2.setValue1("value2");

        cache.put("data2", data2) ;

    }
}
