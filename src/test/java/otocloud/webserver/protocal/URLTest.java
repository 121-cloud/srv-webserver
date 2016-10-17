package otocloud.webserver.protocal;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * zhangyef@yonyou.com on 2015-11-10.
 */
public class URLTest {
    @Test
    public void it_should_get_uri() {
        String url = "http://localhost:8080/api/app/func/id?para0=0&para1=1";

        try {
            URI uri = new URI(url);
            System.out.println(uri.getPath());
            System.out.println(uri.toURL());
            System.out.println(uri.toString());

            URL toURL = uri.toURL();
            System.out.println(toURL.getQuery());

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
