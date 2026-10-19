package com.ruoyi.campus.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.domain.CtUserProfile;
import com.ruoyi.campus.mapper.CtUserProfileMapper;
import com.ruoyi.campus.service.ICtUserProfileService;

/**
 * 用户校园档案 服务层实现
 * 
 * @author campus
 */
@Service
public class CtUserProfileServiceImpl implements ICtUserProfileService
{
    @Autowired
    private CtUserProfileMapper ctUserProfileMapper;

    @Override
    public List<CtUserProfile> selectCtUserProfileList(CtUserProfile ctUserProfile)
    {
        return ctUserProfileMapper.selectCtUserProfileList(ctUserProfile);
    }

    @Override
    public CtUserProfile selectCtUserProfileById(Long profileId)
    {
        return ctUserProfileMapper.selectCtUserProfileById(profileId);
    }

    @Override
    public CtUserProfile selectCtUserProfileByUserId(Long userId)
    {
        return ctUserProfileMapper.selectCtUserProfileByUserId(userId);
    }

    @Override
    public int insertCtUserProfile(CtUserProfile ctUserProfile)
    {
        return ctUserProfileMapper.insertCtUserProfile(ctUserProfile);
    }

    @Override
    public int updateCtUserProfile(CtUserProfile ctUserProfile)
    {
        return ctUserProfileMapper.updateCtUserProfile(ctUserProfile);
    }

    @Override
    public int deleteCtUserProfileByIds(Long[] profileIds)
    {
        return ctUserProfileMapper.deleteCtUserProfileByIds(profileIds);
    }
}
