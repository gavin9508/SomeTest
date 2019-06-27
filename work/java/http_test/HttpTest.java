package httpTest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author:gavin
 * @date:2019/6/27 13 50
 * @Description
 **/
public class HttpTest {
    public static void main(String[] args) {
        String url = "https://tcc.taobao.com/cc/json/mobile_tel_segment.htm?tel=17600055066";
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet http = new HttpGet(url);

        HttpResponse response = null;
        try {
            response = httpclient.execute(http);
            HttpEntity entity = response.getEntity();
            if (entity != null) {

                //getResponse
                InputStream in = entity.getContent();
                byte[] data = transformInputstream(in);
                String context = new String(data, "GBK");
                System.out.println(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] transformInputstream(InputStream input) throws Exception {
        byte[] byt = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b = 0;
        b = input.read();
        while (b != -1) {
            baos.write(b);
            b = input.read();
        }
        byt = baos.toByteArray();
        return byt;
    }
}
