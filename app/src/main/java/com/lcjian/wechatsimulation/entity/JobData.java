package com.lcjian.wechatsimulation.entity;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

public class JobData {

    @SerializedName("unique_id")
    public String uniqueId;

    // 使用电话号码添加好友
    @SerializedName("mobile")
    public String mobile;
    @SerializedName("validation_message")
    public String validationMessage;

    // 修改个人信息
    @SerializedName("account_avatar")
    public String accountAvatar;
    @SerializedName("account_name")
    public String accountName;
    @SerializedName("account_gender")   // 0 男 1 女
    public Integer accountGender;
    @SerializedName("account_sign")
    public String accountSign;

    // 发朋友圈
    @SerializedName("moment_photos")
    public List<String> momentPhotos;
    @SerializedName("moment_text")
    public String momentText;

    // 创建群聊
    @SerializedName("group_chat_ad_member")
    public String groupChatAdMember;
    @SerializedName("group_chat_room_owner")
    public String groupChatRoomOwner;
    @SerializedName("group_chat_member_count")
    public Integer groupChatMemberCount;
    @SerializedName("group_chat_room_name")
    public String groupChatRoomName;

    // 转发小视频
    @SerializedName("room_name_for_forward_sight_from")
    public String roomNameForForwardSightFrom;
    @SerializedName("room_name_for_forward_sight_to")
    public String roomNameForForwardSightTo;

    // 获取群聊房间二维码
    @SerializedName("room_name_for_getting_qr")
    public String roomNameForGettingQr;
    // 获取群聊房间二维码 (返回给服务器的)
    @SerializedName("room_name_qr_local_directory")
    public String roomNameQrLocalDirectory;
    @SerializedName("room_name_qr_img_str")
    public String roomNameQrImgStr;

    // 点击文章广告
    @SerializedName("article_url")
    public String articleUrl;
    @SerializedName("contain_ad")
    public Boolean containAd;
    @SerializedName("view_article_type")
    public Integer viewArticleType; // 1:view, 2:view and click ad, 3:view and click like, 4:view and click like and click ad, 5:get max scroll y
    @SerializedName("max_scroll_y")
    public Integer maxScrollY;

    // 更新自己或者微信时的链接
    @SerializedName("apk_url")
    public String apkUrl;

    // 关注公众号
    @SerializedName("office_account")
    public String officeAccount;

    // 获取客户端消息 (返回给服务器的)
    @SerializedName("we_chat_version_name")
    public String weChatVersionName;
    @SerializedName("app_version_name")
    public String appVersionName;
    @SerializedName("have_root_permission")
    public Boolean haveRootPermission;
    @SerializedName("accessibility_opened")
    public Boolean accessibilityOpened;

    // 微信好友数量 (返回给服务器的)
    @SerializedName("friends_count")
    public Integer friendsCount;

    // 登录账号(获取登陆账号)
    @SerializedName("number")
    public String userName;

    // 注册微信
    @SerializedName("nick_name")
    public String nickName;
    @SerializedName("password")
    public String password;
    @SerializedName("phone_no")
    public String phoneNo;

    // 执行请求url
    @SerializedName("url")
    public String url;
    @SerializedName("header")
    public HashMap<String, String> headers;
    @SerializedName("body")
    public HashMap<String, String> params;
    @SerializedName("method")
    public String method;
    // 请求url的结果 (返回给服务器的)
    @SerializedName("response")
    public String response;

    // 注册Oppo
    @SerializedName("phone_no_o_market")
    public String phoneNoOMarket;
    @SerializedName("password_o_market")
    public String passwordOMarket;

    // 刷Oppo应用市场和点赞
    @SerializedName("apk_url_o_market") // only for 刷Oppo应用市场
    public String apkUrlOMarket;
    @SerializedName("package_name_o_market")
    public String packageNameOMarket;
    @SerializedName("comment_o_market")
    public String commentOMarket;

    // 定位
    @SerializedName("longitude")
    public Double longitude;      // 经度
    @SerializedName("latitude")
    public Double latitude;       // 纬度
}
