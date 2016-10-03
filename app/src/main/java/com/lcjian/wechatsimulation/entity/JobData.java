package com.lcjian.wechatsimulation.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class JobData {

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
    public int accountGender;
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
    public int groupChatMemberCount;
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
    // 获取群聊房间二维码返回给服务器的
    @SerializedName("room_name_qr_local_directory")
    public String roomNameQrLocalDirectory;
    @SerializedName("room_name_qr_img_str")
    public String roomNameQrImgStr;

    // 点击文章广告
    @SerializedName("contact_name_for_article")
    public String contactNameForArticle;
    @SerializedName("article_url")
    public String articleUrl;
    @SerializedName("contain_ad")
    public boolean containAd;
    @SerializedName("view_article_type")
    public int viewArticleType; // 1:view, 2:view and click ad, 3:view and click like, 4:view and click like and click ad

    // 点击文章广告
    @SerializedName("apk_url")
    public String apkUrl;
}
