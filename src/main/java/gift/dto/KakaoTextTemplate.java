package gift.dto;

public class KakaoTextTemplate {

    private String object_type = "text";
    private String text;
    private Link link;

    public KakaoTextTemplate(String text, String url) {
        this.text = text;
        this.link = new Link(url, url);
    }

    public static class Link {
        private String web_url;
        private String mobile_web_url;

        public Link(String webUrl, String mobileWebUrl) {
            this.web_url = webUrl;
            this.mobile_web_url = mobileWebUrl;
        }

        public String getWeb_url() { return web_url; }
        public String getMobile_web_url() { return mobile_web_url; }
    }

    public String getObject_type() { return object_type; }
    public String getText() { return text; }
    public Link getLink() { return link; }
}