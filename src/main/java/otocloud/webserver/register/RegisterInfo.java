package otocloud.webserver.register;

import java.util.Date;

/**
 * Created by better on 15/9/16.
 */
public class RegisterInfo {
    public static final String COMMAND = "command";

    /**
     * [可选]
     * <p/>
     * 指定转发消息类型, 该类型与应用可以接受的消息类型一致.
     * <p/>
     * 若指定为"command", 则表示转发后的消息体类型为 {@link otocloud.common.Command},
     * 此时完整的消息类型为{@linkplain io.vertx.core.eventbus.Message Message}
     * &lt;{@linkplain otocloud.common.Command Command}&gt;.
     * <p/>
     * 如果不设置该参数, 则表示转发后的消息体类型为 {@linkplain io.vertx.core.json.JsonObject JsonObject}.
     */
    private String messageFormat;

    /**
     * (必选)
     * <p/>
     * 事件总线地址, HTTP请求的目标地址, 也是应用的监听地址.
     * 该地址既可以为具体的事件总线地址, 也可以是一个带参数的地址模板.
     * 如果为地址模板, 则需要同时设置 {@link #decoratingAddress}.
     */
    private String address;

    /**
     * [可选]
     * <p/>
     * 事件总线地址对应的行为, 由应用负责处理.
     */
    private String action;

    /**
     * [可选]
     * <p/>
     * 由不同应用提供，用于对address进行自定义解析.
     */
    private String decoratingAddress;

    /**
     * (必选)
     * <p/>
     * HTTP请求的URL.
     */
    private String uri;

    /**
     * (必选)
     * <p/>
     * HTTP请求的方法, post/get/put/delete.
     */
    private String method;

    /**
     * [可选]
     * <p/>
     * 解析HTTP请求的桥接协议.
     */
    private String protocal;

    private String dispatcher; //分发器

    private Date time; //注册时间

    //无参构造函数，用于从JSON反序列化
    public RegisterInfo() {
    }

    public RegisterInfo(String address, String uri, String method) {
        this.address = address;
        this.uri = uri;
        this.method = method;
    }

    /**
     * @return {@link #address}
     */
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getProtocal() {
        return protocal;
    }

    public void setProtocal(String protocal) {
        this.protocal = protocal;
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getDecoratingAddress() {
        return decoratingAddress;
    }

    public void setDecoratingAddress(String decoratingAddress) {
        this.decoratingAddress = decoratingAddress;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessageFormat() {
        return messageFormat;
    }

    public void setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    /**
     * 当 {@link #uri} 和 {@link #method} 都相同时, 认为注册信息相同.
     *
     * @param o 另一个注册信息.
     * @return true, 两个注册信息相同; false, 两个注册信息不同.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegisterInfo info = (RegisterInfo) o;

        if (uri != null ? !uri.equals(info.uri) : info.uri != null) return false;
        return !(method != null ? !method.equals(info.method) : info.method != null);

    }

    /**
     * @return 注册信息的hashcode.
     */
    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RegisterInfo{" +
                "action='" + action + '\'' +
                ", messageFormat='" + messageFormat + '\'' +
                ", address='" + address + '\'' +
                ", decoratingAddress='" + decoratingAddress + '\'' +
                ", uri='" + uri + '\'' +
                ", method='" + method + '\'' +
                ", protocal='" + protocal + '\'' +
                ", dispatcher='" + dispatcher + '\'' +
                ", time=" + time +
                '}';
    }
}
