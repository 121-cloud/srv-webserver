package otocloud.webserver.dispatch;

import io.vertx.ext.web.RoutingContext;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by better on 15/9/16.
 */
public class DefaultAddressImpl implements EventBusAddress {

    private final EventBusTraveller traveller;

    private String address;

    private boolean enabled = true;

    private boolean added;
    private Pattern pattern;
    private Set<String> groups;
    private boolean useNormalisedPath = true;

    public DefaultAddressImpl(EventBusTraveller traveller){
        this.traveller = traveller;
    }

    @Override
    public EventBusAddress at(String address) {
        this.address = address;
        return this;
    }

    @Override
    public EventBusAddress analyze(RoutingContext context) {
        return this;
    }


    @Override
    public EventBusAddress disable() {
        return null;
    }

    @Override
    public EventBusAddress enable() {
        return null;
    }

    @Override
    public EventBusAddress remove() {
        return this;
    }

    @Override
    public String getAddress() {
        return this.address;
    }

    private boolean exactPath;

    private void setAddress(String address) {
        // See if the path contains ":" - if so then it contains parameter capture groups and we have to generate
        // a regex for that
        if (address.indexOf(':') != -1) {
            createPatternRegex(address);
            this.address = address;
        } else {
            if (address.charAt(address.length() - 1) != '*') {
                exactPath = true;
                this.address = address;
            } else {
                exactPath = false;
                this.address = address.substring(0, address.length() - 1);
            }
        }
    }


    private void createPatternRegex(String path) {
        // We need to search for any :<token name> tokens in the String and replace them with named capture groups
        Matcher m =  Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)").matcher(path);
        StringBuffer sb = new StringBuffer();
        groups = new HashSet<>();
        while (m.find()) {
            String group = m.group().substring(1);
            if (groups.contains(group)) {
                throw new IllegalArgumentException("Cannot use identifier " + group + " more than once in pattern string");
            }
            m.appendReplacement(sb, "(?<$1>[^\\.]+)"); //总线地址的分隔符是英文句号".".
            groups.add(group);
        }
        m.appendTail(sb);
        path = sb.toString();
        pattern = Pattern.compile(path);
    }
}
