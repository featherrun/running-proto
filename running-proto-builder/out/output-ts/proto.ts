/**
 * Created by running-proto-builder
 * 2018-07-05 16:49:55
 */
namespace proto {
    export namespace common {
		export class PCommonMsg { //
            static _ID:number = 1;
			mod:number;
			method:number;
			msg:string;
		}
		export class PAttr { //
            static _ID:number = 2;
			stage:number; //场面
			play:number; //表演
			story:number; //剧情
			art:number; //艺术
			joy:number; //娱乐
		}

    }

    export namespace login {
		export class PLogin { //
            static _ID:number = 101;
			name:string; //登录名
			password:string; //密钥
			platform:string; //所属平台
			site:number;
		}
		export class PLoginResult { //
            static _ID:number = 102;
			result:number; //登录结果, 0-成功, 1-失败
			data:proto.login.PLoginData;
		}
		export class PLoginData { //登陆数据
            static _ID:number = 103;
			role:proto.role.PRole;
			actors:proto.actor.PActor[];
			builds:proto.build.PBuild[];
			filmings:proto.filming.PFilming[];
		}
		export enum LoginPlatform { //
			empty=0,
			qq=1,
			wechat=2,
		}
		export const LoginResultType = { //登录结果
			success:0,
			fail:1,
		};

    }

    export namespace role {
		export class PRole { //
            static _ID:number = 201;
			uid:string;
			nickname:string;
			level:number;
			companyLevel:number; //公司等级
			money:number; //美元
		}

    }

    export namespace actor {
		export class PActor { //演员
            static _ID:number = 301;
			uid:string;
			actorId:string;
			name:string;
			time:number;
			level:number;
			attrs:proto.common.PAttr; //属性
		}

    }

    export namespace build {
		export class PBuild { //
            static _ID:number = 401;

		}

    }

    export namespace item {
		export class PItem { //
            static _ID:number = 501;

		}

    }

    export namespace filming {
		export class PFilming { //拍摄
            static _ID:number = 601;
			uid:string;
			step:number; //当前步骤
			time:number;
			dramas:string[]; //可选剧本列表
			dramaId:string; //剧本
			name:string; //自定义剧本名称
			market:number[]; //市场
			actors:string[]; //演员
			style:number; //拍摄类型
			readyTime:number;
			attrs:proto.common.PAttr; //最终属性
			levelAttrs:proto.common.PAttr; //同档电影水平
			showTime:number;
			firstId:number; //首映院线
			mediaScore:number; //媒体评分
			firstBoxOffice:number; //首映票房
			viewerScore:number; //观众口碑
			showEndTime:number;
			boxOffice:number; //总票房
			rewards:string; //奖励
		}
		export class PFilmingSelectDrama { //
            static _ID:number = 602;
			dramaId:string;
			name:string;
		}
		export class PFilmingSelectActor { //
            static _ID:number = 603;
			actors:string[];
		}
		export class PFilmingSelectStyle { //
            static _ID:number = 604;
			style:number;
		}
		export class PFilmingSelectFirst { //
            static _ID:number = 605;
			office:number;
		}
		export const FilmingStep = { //
			selectDrama:1, //选择剧本
			selectActor:2, //选择演员
			selectStyle:3, //选择类型
			filming:4, //拍摄中
			selectFirst:5, //选择首映
			showing:6, //上映中
			incoming:7, //持续收益中
			finish:8, //结束
		};

    }

    export namespace drama {

    }


	export const PacketMod = {
		common:0,
		login:1,
		role:2,
		actor:3,
		build:4,
		item:5,
		filming:6,
		drama:7,
	};

    /*
	export const PacketMethod = {
		//common
		MSG_COMMON_COMMONMSG: 10000, //通用消息
		//login
		CMD_LOGIN_LOGIN: 101, //请求登录
		MSG_LOGIN_LOGINRESULT: 102, //登录结果
		CMD_LOGIN_ENTERMAINSCENECOMPLETED: 201, //进入主场景
		//role
		//actor
		//build
		//item
		//filming
		CMD_FILMING_FILMING: 1, //请求拍摄
		CMD_FILMING_SELECTDRAMA: 2, //选择剧本
		CMD_FILMING_SELECTACTOR: 3, //选择演员
		CMD_FILMING_SELECTSTYLE: 4, //选择类型
		CMD_FILMING_SELECTFIRST: 5, //选择首映
		CMD_FILMING_FILMINGEND: 6, //放映结束、开始持续收益
		//drama
	};
	*/

    export const Action = {
		//proto.common
		msg_CommonMsg: 10000, //0-10000 通用消息
		//proto.login
		cmd_Login: 65637, //1-101 请求登录
		msg_LoginResult: 65638, //1-102 登录结果
		cmd_enterMainSceneCompleted: 65737, //1-201 进入主场景
		//proto.role
		//proto.actor
		//proto.build
		//proto.item
		//proto.filming
		cmd_filming: 393217, //6-1 请求拍摄
		cmd_selectDrama: 393218, //6-2 选择剧本
		cmd_selectActor: 393219, //6-3 选择演员
		cmd_selectStyle: 393220, //6-4 选择类型
		cmd_selectFirst: 393221, //6-5 选择首映
		cmd_filmingEnd: 393222, //6-6 放映结束、开始持续收益
		//proto.drama
    };
}
