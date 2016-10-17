package otocloud.webserver.register;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

/**
 * RegisterMessage Tester.
 *
 * @author 张野
 * @version 1.0
 * @since <pre>九月 21, 2015</pre>
 */
public class RegisterMessageTest {
    RegisterInfo registerMessage;
    private ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally

    @Before
    public void before() throws Exception {
        registerMessage = new RegisterInfo("my.application","app/login","post");
        registerMessage.setAddress("my.application");
        registerMessage.setMethod("post");
        registerMessage.setUri("app/login");
    }


    @After
    public void after() throws Exception {
    }

    @Test
    public void toJsonTest(){
        try {
            System.out.println(mapper.writeValueAsString(registerMessage));
        } catch (JsonProcessingException e) {
            Assert.fail();
        }
    }

    @Test
    public void it_should_convert_jsonstr_to_object() throws Exception{
        //{"address":"my.application","uri":"app/login","method":"post"}
        String jsonStr = makeRegisterMsgStr();

        RegisterInfo msg = mapper.readValue(jsonStr,RegisterInfo.class);

        Assert.assertTrue(msg.getAddress().equals("my.application"));
        Assert.assertEquals(msg.getUri(), "app/login");
        Assert.assertEquals(msg.getMethod(),"post");
    }

    private String makeRegisterMsgStr(){
        String jsonStr = "{\"address\":\"my.application\",\"uri\":\"app/login\",\"method\":\"post\"}";
        return jsonStr;
    }

} 
