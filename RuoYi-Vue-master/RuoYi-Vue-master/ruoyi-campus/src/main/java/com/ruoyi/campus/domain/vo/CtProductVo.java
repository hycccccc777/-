package com.ruoyi.campus.domain.vo;

import com.ruoyi.campus.domain.CtProduct;

/**
 * 商品视图对象：在商品基础上补充类目/学校/卖家名称，供 /api/v1 前端展示。
 *
 * @author campus
 */
public class CtProductVo extends CtProduct
{
    private static final long serialVersionUID = 1L;

    /** 类目名称 */
    private String categoryName;

    /** 学校名称 */
    private String schoolName;

    /** 学校简称 */
    private String schoolShortName;

    /** 卖家昵称 */
    private String sellerName;

    /** 查询用：成色编码（不影响输出） */
    private String condition;

    /** 查询用：关键词（标题模糊匹配） */
    private String keyword;

    /** 查询用：学校名称或简称 */
    private String school;

    /** 查询用：排序方式（recommended/priceAsc/priceDesc/newest） */
    private String sort;

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getSchoolName()
    {
        return schoolName;
    }

    public void setSchoolName(String schoolName)
    {
        this.schoolName = schoolName;
    }

    public String getSchoolShortName()
    {
        return schoolShortName;
    }

    public void setSchoolShortName(String schoolShortName)
    {
        this.schoolShortName = schoolShortName;
    }

    public String getSellerName()
    {
        return sellerName;
    }

    public void setSellerName(String sellerName)
    {
        this.sellerName = sellerName;
    }

    public String getCondition()
    {
        return condition;
    }

    public void setCondition(String condition)
    {
        this.condition = condition;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public String getSchool()
    {
        return school;
    }

    public void setSchool(String school)
    {
        this.school = school;
    }

    public String getSort()
    {
        return sort;
    }

    public void setSort(String sort)
    {
        this.sort = sort;
    }

    /**
     * 成色中文标签，随对象一起返回，前端无需再维护映射。
     */
    public String getConditionLabel()
    {
        String code = getConditionCode();
        if ("almost_new".equals(code))
        {
            return "几乎全新";
        }
        if ("nine".equals(code))
        {
            return "九成新";
        }
        if ("eight".equals(code))
        {
            return "八成新";
        }
        return code;
    }
}
