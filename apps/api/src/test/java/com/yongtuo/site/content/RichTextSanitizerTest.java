package com.yongtuo.site.content;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
class RichTextSanitizerTest {
    @Test void stripsScriptsEventsDangerousUrlsAndForeignMarkup() {
        String cleaned=RichTextSanitizer.clean("<script>alert(1)</script><p onclick='evil()'>Safe</p>"
                +"<img src='https://example.com/a.jpg' onerror='evil()'><a href='javascript:alert(1)'>Link</a>"
                +"<svg onload='evil()'><script>alert(2)</script></svg><iframe src='https://example.com'></iframe>");
        assertThat(cleaned).contains("Safe","https://example.com/a.jpg","Link")
                .doesNotContain("<script","onclick","onerror","javascript:","<svg","<iframe","alert(");
    }
    @Test void preservesTablesListsImagesAndSafeLinksIdempotently() {
        String source="<h2>Heading</h2><table><tbody><tr><td colspan='2'>12 mm</td></tr></tbody></table>"
                +"<ul><li>Item</li></ul><img src='https://example.com/a.jpg' alt='Part'><a href='/products/example'>Product</a>";
        String cleaned=RichTextSanitizer.clean(source);
        assertThat(cleaned).contains("<table>","<td colspan=\"2\">12 mm</td>","<ul>","<img", "href=\"/products/example\"");
        assertThat(RichTextSanitizer.clean(cleaned)).isEqualTo(cleaned);
        assertThat(RichTextSanitizer.clean(null)).isNull();
    }
}
