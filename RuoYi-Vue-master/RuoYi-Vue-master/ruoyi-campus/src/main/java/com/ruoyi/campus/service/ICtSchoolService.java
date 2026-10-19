package com.ruoyi.campus.service;

import java.util.List;
import com.ruoyi.campus.domain.CtSchool;

/**
 * 学校 服务层
 * 
 * @author campus
 */
public interface ICtSchoolService
{
    public List<CtSchool> selectCtSchoolList(CtSchool ctSchool);

    public CtSchool selectCtSchoolById(Long schoolId);

    public int insertCtSchool(CtSchool ctSchool);

    public int updateCtSchool(CtSchool ctSchool);

    public int deleteCtSchoolByIds(Long[] schoolIds);
}
