package com.yongtuo.site.content.home;
public enum FeaturedKind {
    products("home_featured_product","product",8),cases("home_featured_case","case_study",4),
    articles("home_featured_article","article",3),certificates("home_featured_certificate","certificate",4);
    final String relation,table;final int max;
    FeaturedKind(String relation,String table,int max) { this.relation=relation;this.table=table;this.max=max; }
}
