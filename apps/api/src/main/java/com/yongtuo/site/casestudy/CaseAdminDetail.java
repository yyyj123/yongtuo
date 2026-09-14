package com.yongtuo.site.casestudy;
import java.util.List;
public record CaseAdminDetail(CaseStudy base, List<Long> productIds, List<Long> categoryIds, List<String> imageUrls) { }
