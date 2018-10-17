/**
 * Created by running-proto-builder
 */
package action;

import java.util.concurrent.Callable;

public enum Action {
	//proto.login
	cmd_Login(65637, new action.login.CmdLogin()), //请求登录
	msg_LoginResult(65638, null), //登录结果
	cmd_enterMainSceneCompleted(65737, new action.login.CmdEnterMainSceneCompleted()), //进入主场景
	rpc_SomeRpc(66037, new action.login.RpcSomeRpc()), //RPC测试
	job_call(66137, new action.login.JobCall()), //JOB测试
	//proto.role
	;

	public final int action;
	public final Callable<Object> callable;
	Action(int action, Callable<Object> callable) {
		this.action = action;
		this.callable = callable;
	}
}
