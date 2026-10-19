package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtUserProfile;

/**
 * 用户校园档案 数据层
 * 
 * @author campus
 */
public interface CtUserProfileMapper
{
    public List<CtUserProfile> selectCtUserProfileList(CtUserProfile ctUserProfile);

    public CtUserProfile selectCtUserProfileById(Long profileId);

    public CtUserProfile selectCtUserProfileByUserId(Long userId);

    public int insertCtUserProfile(CtUserProfile ctUserProfile);

    public int updateCtUserProfile(CtUserProfile ctUserProfile);

    public int deleteCtUserProfileById(Long profileId);

    public int deleteCtUserProfileByIds(Long[] profileIds);
}
