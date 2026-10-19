package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtAddress;

/**
 * 收货地址 数据层
 * 
 * @author campus
 */
public interface CtAddressMapper
{
    public List<CtAddress> selectCtAddressList(CtAddress ctAddress);

    public CtAddress selectCtAddressById(Long addressId);

    public int insertCtAddress(CtAddress ctAddress);

    public int updateCtAddress(CtAddress ctAddress);

    public int deleteCtAddressById(Long addressId);

    public int deleteCtAddressByIds(Long[] addressIds);
}
