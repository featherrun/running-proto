/**
 * Created by running-proto-builder
 */
package proto.login;

/***/
public enum LoginPlatform {
	empty((short) 0, "empty"),
	qq((short) 1, "qq"),
	wechat((short) 2, "wechat"),
	;

    public final short code;
    public final String name;
    LoginPlatform(short code, String name) {
        this.code = code;
        this.name = name;
    }
}
