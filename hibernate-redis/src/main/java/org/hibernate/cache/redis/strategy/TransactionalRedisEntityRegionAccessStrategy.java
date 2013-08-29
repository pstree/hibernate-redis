/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.cache.redis.strategy;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.redis.jedis.JedisClient;
import org.hibernate.cache.redis.regions.RedisEntityRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * TransactionalRedisEntityRegionAccessStrategy
 *
 * @author sunghyouk.bae@gmail.com
 * @since 13. 4. 5. 오후 11:14
 */
@Slf4j
public class TransactionalRedisEntityRegionAccessStrategy
    extends AbstractRedisAccessStrategy<RedisEntityRegion>
    implements EntityRegionAccessStrategy {

    @Getter
    private final JedisClient jedisClient;

    public TransactionalRedisEntityRegionAccessStrategy(RedisEntityRegion region,
                                                        Settings settings) {
        super(region, settings);
        this.jedisClient = region.getJedisClient();
    }

    @Override
    public EntityRegion getRegion() {
        return region();
    }

    @Override
    public Object get(Object key, long txTimestamp) throws CacheException {
        log.trace("캐시 값을 로드합니다. key=[{}], txTimestamp=[{}]", key, txTimestamp);
        try {
            return jedisClient.get(key);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    public boolean putFromLoad(Object key,
                               Object value,
                               long txTimestamp,
                               Object version,
                               boolean minimalPutOverride) throws CacheException {
        log.trace("putFromLoad... key=[{}]", key);
        try {
            if (minimalPutOverride && jedisClient.exists(key))
                return false;
            jedisClient.set(key, value);
            return true;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        return null;
    }

    @Override
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        // nothing to do
    }

    @Override
    public boolean insert(Object key, Object value, Object version) throws CacheException {
        log.trace("캐시 값을 저장합니다... key=[{}]", key);
        try {
            jedisClient.set(key, value);
            return true;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    @Override
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
        log.trace("update cache item... key=[{}]", key);
        try {
            jedisClient.set(key, value);
            return true;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
        throws CacheException {
        return false;
    }

    @Override
    public void remove(Object key) throws CacheException {
        log.trace("캐시를 삭제합니다. key=[{}]", key);
        try {
            jedisClient.delete(key);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }
}
