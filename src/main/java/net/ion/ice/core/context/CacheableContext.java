package net.ion.ice.core.context;

import net.ion.ice.core.query.QueryResult;

public interface CacheableContext {
    String getCacheTime();

    QueryResult makeCacheResult();
}
