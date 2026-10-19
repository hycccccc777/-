package com.ruoyi.campus.service;

import java.util.List;
import com.ruoyi.campus.domain.CtAddress;

/**
 * 收货地址 服务层
 * 
 * @author campus
 */
public interface ICtAddressService
{
    public List<CtAddress> selectCtAddressList(CtAddress ctAddress);

    public CtAddress selectCtAddressById(Long addressId);

    public int insertCtAddress(CtAddress ctAddress);

    public int updateCtAddress(CtAddress ctAddress);

    public int deleteCtAddressByIds(Long[] addressIds);
}
