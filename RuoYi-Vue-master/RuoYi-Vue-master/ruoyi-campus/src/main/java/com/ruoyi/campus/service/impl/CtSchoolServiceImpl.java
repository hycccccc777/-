package com.ruoyi.campus.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.domain.CtSchool;
import com.ruoyi.campus.mapper.CtSchoolMapper;
import com.ruoyi.campus.service.ICtSchoolService;

/**
 * 学校 服务层实现
 * 
 * @author campus
 */
@Service
public class CtSchoolServiceImpl implements ICtSchoolService
{
    @Autowired
    private CtSchoolMapper ctSchoolMapper;

    @Override
    public List<CtSchool> selectCtSchoolList(CtSchool ctSchool)
    {
        return ctSchoolMapper.selectCtSchoolList(ctSchool);
    }

    @Override
    public CtSchool selectCtSchoolById(Long schoolId)
    {
        return ctSchoolMapper.selectCtSchoolById(schoolId);
    }

    @Override
    public int insertCtSchool(CtSchool ctSchool)
    {
        return ctSchoolMapper.insertCtSchool(ctSchool);
    }

    @Override
    public int updateCtSchool(CtSchool ctSchool)
    {
        return ctSchoolMapper.updateCtSchool(ctSchool);
    }

    @Override
    public int deleteCtSchoolByIds(Long[] schoolIds)
    {
        return ctSchoolMapper.deleteCtSchoolByIds(schoolIds);
    }
}
