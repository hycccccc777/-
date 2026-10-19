package com.ruoyi.campus.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.domain.CtAddress;
import com.ruoyi.campus.mapper.CtAddressMapper;
import com.ruoyi.campus.service.ICtAddressService;

/**
 * 收货地址 服务层实现
 * 
 * @author campus
 */
@Service
public class CtAddressServiceImpl implements ICtAddressService
{
    @Autowired
    private CtAddressMapper ctAddressMapper;

    @Override
    public List<CtAddress> selectCtAddressList(CtAddress ctAddress)
    {
        return ctAddressMapper.selectCtAddressList(ctAddress);
    }

    @Override
    public CtAddress selectCtAddressById(Long addressId)
    {
        return ctAddressMapper.selectCtAddressById(addressId);
    }

    @Override
    public int insertCtAddress(CtAddress ctAddress)
    {
        return ctAddressMapper.insertCtAddress(ctAddress);
    }

    @Override
    public int updateCtAddress(CtAddress ctAddress)
    {
        return ctAddressMapper.updateCtAddress(ctAddress);
    }

    @Override
    public int deleteCtAddressByIds(Long[] addressIds)
    {
        return ctAddressMapper.deleteCtAddressByIds(addressIds);
    }
}
