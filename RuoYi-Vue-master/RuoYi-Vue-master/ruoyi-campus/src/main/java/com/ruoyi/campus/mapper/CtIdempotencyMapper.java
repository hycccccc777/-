package com.ruoyi.campus.mapper;

import com.ruoyi.campus.domain.CtIdempotency;

/**
 * 下单幂等 数据层
 * 
 * @author campus
 */
public interface CtIdempotencyMapper
{
    /**
     * 抢占幂等键：命中唯一键 uk_ct_idem_key 时会抛 DuplicateKeyException，
     * 由 Service 捕获并转为「重复提交」。
     */
    public int insertCtIdempotency(CtIdempotency ctIdempotency);

    public CtIdempotency selectCtIdempotencyByKey(String idemKey);

    public int updateCtIdempotency(CtIdempotency ctIdempotency);
}
