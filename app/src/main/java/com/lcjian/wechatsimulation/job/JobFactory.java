package com.lcjian.wechatsimulation.job;

import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.entity.JobAndData;
import com.lcjian.wechatsimulation.job.foreground.AcceptFriendRequestJob;
import com.lcjian.wechatsimulation.job.foreground.AddFriendByContactsJob;
import com.lcjian.wechatsimulation.job.foreground.AddFriendBySearchMobileJob;
import com.lcjian.wechatsimulation.job.foreground.AddFriendNearby;
import com.lcjian.wechatsimulation.job.foreground.ClickLikeOMarketJob;
import com.lcjian.wechatsimulation.job.foreground.CommentOMarketJob;
import com.lcjian.wechatsimulation.job.foreground.CountFriendsJob;
import com.lcjian.wechatsimulation.job.foreground.CreateGroupChatJob;
import com.lcjian.wechatsimulation.job.foreground.CreateMomentJob;
import com.lcjian.wechatsimulation.job.foreground.FollowOfficeAccountJob;
import com.lcjian.wechatsimulation.job.foreground.ForwardSightJob;
import com.lcjian.wechatsimulation.job.foreground.GetRoomQrCodeJob;
import com.lcjian.wechatsimulation.job.foreground.ModifyAccountInfoJob;
import com.lcjian.wechatsimulation.job.foreground.RegisterOMarketJob;
import com.lcjian.wechatsimulation.job.foreground.RegisterWeChatJob;
import com.lcjian.wechatsimulation.job.foreground.ViewArticleJob;

import timber.log.Timber;

public class JobFactory {

    public static Job createJob(String str) {
        JobAndData jobAndData = null;
        try {
            String body = new String(Base64.decode(str.getBytes(), Base64.NO_WRAP));
            Timber.d(body);
            jobAndData = new Gson().fromJson(body, JobAndData.class);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
        if (jobAndData == null) {
            return null;
        }
        Job job = null;
        if (TextUtils.equals("add_friend_by_search_mobile_job", jobAndData.job)) { // 搜索加好友（这里是电话号码，其他的应该也可以）
            job = new AddFriendBySearchMobileJob();
        } else if (TextUtils.equals("create_group_chat_job", jobAndData.job)) { // 创建群聊
            job = new CreateGroupChatJob();
        } else if (TextUtils.equals("forward_sight_job", jobAndData.job)) { // 转发小视频
            job = new ForwardSightJob();
        } else if (TextUtils.equals("get_room_qr_code_job", jobAndData.job)) { // 获取群二维码
            job = new GetRoomQrCodeJob();
        } else if (TextUtils.equals("modify_account_info_job", jobAndData.job)) { // 修改个人信息
            job = new ModifyAccountInfoJob();
        } else if (TextUtils.equals("create_moment_job", jobAndData.job)) { // 发朋友圈
            job = new CreateMomentJob();
        } else if (TextUtils.equals("view_article_job", jobAndData.job)) { // 浏览微信文章 刷阅读
            job = new ViewArticleJob();
        } else if (TextUtils.equals("accept_friend_request_job", jobAndData.job)) {  // 自动接收好友请求
            job = new AcceptFriendRequestJob();
        } else if (TextUtils.equals("count_friends_job", jobAndData.job)) { // 统计好友数量
            job = new CountFriendsJob();
        } else if (TextUtils.equals("add_friend_by_contacts_job", jobAndData.job)) { // 通过通讯录加好友
            job = new AddFriendByContactsJob();
        } else if (TextUtils.equals("add_friend_nearby", jobAndData.job)) { // 附近的人打招呼
            job = new AddFriendNearby();
        } else if (TextUtils.equals("follow_office_account_job", jobAndData.job)) { // 关注公众号
            job = new FollowOfficeAccountJob();
        } else if (TextUtils.equals("register_we_chat_job", jobAndData.job)) { // 注册微信
            job = new RegisterWeChatJob();
        } else if (TextUtils.equals("register_o_market_job", jobAndData.job)) { // 注册OMarket
            job = new RegisterOMarketJob();
        } else if (TextUtils.equals("comment_o_market_job", jobAndData.job)) { // 刷评价OMarket
            job = new CommentOMarketJob();
        } else if (TextUtils.equals("click_like_o_market_job", jobAndData.job)) { // 点赞OMarket评论
            job = new ClickLikeOMarketJob();
        }
        if (job != null) {
            job.setJobData(jobAndData.data);
        }
        return job;
    }
}
