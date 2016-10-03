package com.lcjian.wechatsimulation;

import com.lcjian.wechatsimulation.job.Job;
import com.lcjian.wechatsimulation.job.JobFactory;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import timber.log.Timber;

public class SmackClient {

    public enum State {
        CONNECTING, CONNECTED, CONNECT_FAILED, RECONNECTING, DISCONNECTING, DISCONNECTED,
        ACCOUNT_CREATING, ACCOUNT_CREATED, ACCOUNT_CREATE_FAILED,
        LOGIN_ING, LOGIN_ED, LOGIN_FAILED
    }

    public interface StateChangeListener {
        void onStateChange(State state);
    }

    private State mState = State.DISCONNECTED;

    private List<StateChangeListener> mStateChangeListeners;

    public void addStateChangeListener(StateChangeListener stateChangeListener) {
        mStateChangeListeners.add(stateChangeListener);
    }

    public void removieStateChangeListener(StateChangeListener stateChangeListener) {
        mStateChangeListeners.remove(stateChangeListener);
    }

    private final static String HOST = "114.215.140.3";

    private final static int PORT = 5222;

    private final static String DOMAIN = "linestoken";

    private String mUsername;

    private String mPassword;

    private XMPPTCPConnection mXMPPTCPConnection;

    private BlockingQueue<Job> mJobsQueue;

    public SmackClient(String username, String password, BlockingQueue<Job> jobsQueue) {
        this.mUsername = username;
        this.mPassword = password;
        this.mJobsQueue = jobsQueue;
        this.mStateChangeListeners = new ArrayList<>();
    }

    private void connect() {
        try {
//            Job job = new ViewArticleJob();
//            JobData jobData = new JobData();
//            jobData.contactNameForArticle = "小辉";
//            jobData.articleUrl = "http://mp.weixin.qq.com/s?__biz=MjM5MTU3MzI3MA==&mid=2650511158&idx=5&sn=630a3e7e47adc586eb7216490520314d&scene=1&srcid=0903t4Y2UNYGckdamkeGmTqh&from=groupmessage&isappinstalled=0#wechat_redirect";
//            jobData.containAd = true;
//            jobData.viewArticleType = 4;
//            job.setJobData(jobData);
//            mJobsQueue.put(job);
            notifySateChange(State.CONNECTING);
            mXMPPTCPConnection = new XMPPTCPConnection(
                    XMPPTCPConnectionConfiguration.builder()
                            .setHost(HOST)
                            .setPort(PORT)
                            .setXmppDomain(JidCreate.domainBareFrom(DOMAIN))
                            .setResource("SmackAndroidClient")
                            .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                            .setDebuggerEnabled(true)
                            .build());

            ProviderManager.addExtensionProvider(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
            ProviderManager.addExtensionProvider(DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceiptRequest.Provider());
            DeliveryReceiptManager.getInstanceFor(mXMPPTCPConnection).setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.ifIsSubscribed);

            ReconnectionManager.getInstanceFor(mXMPPTCPConnection).enableAutomaticReconnection();

            mXMPPTCPConnection.connect();
            ChatManager.getInstanceFor(mXMPPTCPConnection).addChatListener(new ChatManagerListener() {
                @Override
                public void chatCreated(Chat chat, boolean createdLocally) {
                    chat.addMessageListener(new ChatMessageListener() {
                        @Override
                        public void processMessage(Chat chat, Message message) {
                            Job job = JobFactory.createJob(message.getBody());
                            if (job != null) {
                                try {
                                    mJobsQueue.put(job);
                                } catch (InterruptedException e) {
                                    Timber.w(e.getMessage());
                                }
                            }
                        }
                    });
                }
            });
            notifySateChange(State.CONNECTED);
        } catch (InterruptedException | SmackException | IOException | XMPPException e) {
            notifySateChange(State.CONNECT_FAILED);
            Timber.e(e.getMessage());
        }
    }

    private void createAccount() {
        notifySateChange(State.ACCOUNT_CREATING);
        try {
            AccountManager accountManager = AccountManager.getInstance(mXMPPTCPConnection);
            accountManager.sensitiveOperationOverInsecureConnection(true);
            accountManager.createAccount(Localpart.from(mUsername), mPassword);
            notifySateChange(State.ACCOUNT_CREATED);
        } catch (SmackException.NoResponseException
                | XmppStringprepException
                | XMPPException.XMPPErrorException
                | InterruptedException
                | SmackException.NotConnectedException e) {
            if (e instanceof XMPPException.XMPPErrorException
                    && ((XMPPException.XMPPErrorException) e).getXMPPError().getCondition() == XMPPError.Condition.conflict) {
                notifySateChange(State.ACCOUNT_CREATED);
            } else {
                notifySateChange(State.ACCOUNT_CREATE_FAILED);
            }
            Timber.e(e.getMessage());
        }
    }

    private void login() {
        try {
            notifySateChange(State.LOGIN_ING);
            mXMPPTCPConnection.login(mUsername, mPassword);
            notifySateChange(State.LOGIN_ED);
        } catch (XMPPException
                | SmackException
                | InterruptedException
                | IOException e) {
            notifySateChange(State.LOGIN_FAILED);
            Timber.e(e.getMessage());
        }
    }

    private void disconnect() {
        notifySateChange(State.DISCONNECTING);
        mXMPPTCPConnection.disconnect();
        notifySateChange(State.DISCONNECTED);
    }

    public void start() {
        if (mXMPPTCPConnection == null || !mXMPPTCPConnection.isConnected()) {
            connect();
        }
        if (mXMPPTCPConnection != null && mXMPPTCPConnection.isConnected()) {
            createAccount();
        }
        if (mXMPPTCPConnection != null && mState == State.ACCOUNT_CREATED && !mXMPPTCPConnection.isAuthenticated()) {
            login();
        }
    }

    public void stop() {
        disconnect();
    }

    private void notifySateChange(State state) {
        mState = state;
        for (StateChangeListener stateChangeListener : mStateChangeListeners) {
            stateChangeListener.onStateChange(mState);
        }
    }

    public String getUsername() {
        return mUsername;
    }

    public State getState() {
        return mState;
    }

    public void sendMessage(String message) {
        try {
            ChatManager.getInstanceFor(mXMPPTCPConnection).createChat(JidCreate.entityBareFrom("admin@" + DOMAIN)).sendMessage(message);
        } catch (XmppStringprepException | InterruptedException | SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }
}
