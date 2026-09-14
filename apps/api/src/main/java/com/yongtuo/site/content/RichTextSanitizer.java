package com.yongtuo.site.content;

import com.yongtuo.site.common.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;

/** Managed body fragments only; attributes and plain-text fields have their own DTO boundaries. */
public final class RichTextSanitizer {
    private static final Safelist POLICY=Safelist.relaxed()
            .addTags("h1","h2","h3","h4","h5","h6","figure","figcaption")
            .addAttributes("td","colspan","rowspan").addAttributes("th","colspan","rowspan","scope")
            .removeProtocols("a","href","ftp")
            .addProtocols("a","href","tel")
            .addEnforcedAttribute("a","rel","nofollow noopener noreferrer")
            .preserveRelativeLinks(true);
    private RichTextSanitizer() { }
    public static String clean(String html) {
        if(html==null) return null;
        if(html.length()>1000000) throw new BusinessException(34004,"Rich text is too large",HttpStatus.BAD_REQUEST);
        return Jsoup.clean(html,"https://sanitizer.invalid/",POLICY,new Document.OutputSettings().prettyPrint(false));
    }
}
