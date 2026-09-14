package com.yongtuo.site.casestudy;
import java.util.List;
public record PublicCaseStudy(long id, String slug, String title, String coverImage, String summary,
        String content, String applicationScene, String requirement, String solution,
        String seoTitle, String seoDescription, List<Link> products, List<Link> categories, List<String> imageUrls) {
    public record Link(long id, String slug, String name) { }
}
