package com.ruoyi.campus.service;

import java.util.List;
import com.ruoyi.campus.domain.CtUserProfile;

/**
 * 用户校园档案 服务层
 * 
 * @author campus
 */
public interface ICtUserProfileService
{
    public List<CtUserProfile> selectCtUserProfileList(CtUserProfile ctUserProfile);

    public CtUserProfile selectCtUserProfileById(Long profileId);

    public CtUserProfile selectCtUserProfileByUserId(Long userId);

    public int insertCtUserProfile(CtUserProfile ctUserProfile);

    public int updateCtUserProfile(CtUserProfile ctUserProfile);

    public int deleteCtUserProfileByIds(Long[] profileIds);
}
