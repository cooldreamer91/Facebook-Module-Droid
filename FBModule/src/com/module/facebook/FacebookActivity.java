package com.module.facebook;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.internal.SessionAuthorizationType;
import com.facebook.internal.SessionTracker;
import com.facebook.model.GraphUser;

/**
 * <p>
 * Basic implementation of an Activity that uses a Session to perform Single
 * Sign On (SSO).
 * </p>
 * 
 * <p>
 * Numerous Activity lifecycle methods are overridden in this class to manage
 * session information. If you override Activity lifecycle methods, be sure to
 * call the appropriate {@code super} method.
 * 
 * <p>
 * The methods in this class are not thread-safe
 * </p>
 */

public class FacebookActivity extends FragmentActivity {

	private static final String SESSION_IS_ACTIVE_KEY = "com.module.facebook.FBActivity.sessionIsActiveKey";
	private static final String GRAPHAPI_REQUEST_ME = "request_me";

	private SessionTracker sessionTracker;

	private String sGraphAPI = "";
	private ProgressDialog progressDlg = null;

	/**
	 * Initializes the state in FacebookActivity. This method will try to
	 * restore the Session if one was saved. If the restored Session object was
	 * the active Session, it will also set the restored Session as the active
	 * Session (unless there's currently an active Session already set).
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Session.StatusCallback callback = new DefaultSessionStatusCallback();
		sessionTracker = new SessionTracker(this, callback);
		if (savedInstanceState != null) {
			Session session = Session.restoreSession(this, null, callback,
					savedInstanceState);
			if (session != null) {
				if (savedInstanceState.getBoolean(SESSION_IS_ACTIVE_KEY)) {
					if (Session.getActiveSession() == null) {
						Session.setActiveSession(session);
					}
				} else {
					sessionTracker.setSession(session);
				}
			}
		}
	}

	/**
	 * 
	 * @param context
	 * @return
	 */

	public String getHashKey(Context context) {
		String hashKey = "";
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					"com.module.facebook", PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				hashKey = Base64.encodeToString(md.digest(), Base64.DEFAULT);
				Log.i("FBModule", "Hash Key = " + hashKey);
			}
		} catch (NameNotFoundException e) {
			// TODO: handle exception
		} catch (NoSuchAlgorithmException e) {

		}
		return hashKey;
	}

	/**
	 * Called when the activity that was launched exits. This method manages
	 * session information when a session is opened. If this method is
	 * overridden in subclasses, be sure to call
	 * {@code super.onActivityResult(...)} first.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Session session = getSession();
		if (session != null) {
			session.onActivityResult(this, requestCode, resultCode, data);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sessionTracker.stopTracking();
	}

	/**
	 * This method will save the session state so that it can be restored during
	 * onCreate.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Session currentSession = sessionTracker.getSession();
		Session.saveSession(currentSession, outState);
		outState.putBoolean(SESSION_IS_ACTIVE_KEY,
				sessionTracker.isTrackingActiveSession());
	}

	// METHOD TO BE OVERRIDDEN

	/**
	 * Called when the session state changes. Override this method to take
	 * action on session state changes.
	 * 
	 * @param state
	 *            the new Session state
	 * @param exception
	 *            any exceptions that occurred during the state change
	 */
	protected void onSessionStateChange(SessionState state, Exception exception) {
		if (state.isOpened()) {
			if (sGraphAPI == GRAPHAPI_REQUEST_ME) {
				showProgressDlg(getResources().getString(
						R.string.progressdlg_fetching_user_info));
				requestMe();
			}
		} else {
			Log.i("Facebook", "Session State = " + state);
		}
	}

	/**
	 * Use the supplied Session object instead of the active Session.
	 * 
	 * @param newSession
	 *            the Session object to use
	 */
	protected void setSession(Session newSession) {
		sessionTracker.setSession(newSession);
	}

	// ACCESSORS (CANNOT BE OVERRIDDEN)

	/**
	 * Gets the current session for this Activity
	 * 
	 * @return the current session, or null if one has not been set.
	 */
	protected final Session getSession() {
		return sessionTracker.getSession();
	}

	/**
	 * Determines whether the current session is open.
	 * 
	 * @return true if the current session is open
	 */
	protected final boolean isSessionOpen() {
		return sessionTracker.getOpenSession() != null;
	}

	/**
	 * Gets the current state of the session or null if no session has been
	 * created.
	 * 
	 * @return the current state of the session
	 */
	protected final SessionState getSessionState() {
		Session currentSession = sessionTracker.getSession();
		return (currentSession != null) ? currentSession.getState() : null;
	}

	/**
	 * Gets the access token associated with the current session or null if no
	 * session has been created.
	 * 
	 * @return the access token
	 */
	protected final String getAccessToken() {
		Session currentSession = sessionTracker.getOpenSession();
		return (currentSession != null) ? currentSession.getAccessToken()
				: null;
	}

	/**
	 * Gets the date at which the current session will expire or null if no
	 * session has been created.
	 * 
	 * @return the date at which the current session will expire
	 */
	protected final Date getExpirationDate() {
		Session currentSession = sessionTracker.getOpenSession();
		return (currentSession != null) ? currentSession.getExpirationDate()
				: null;
	}

	/**
	 * Closes the current session.
	 */
	protected final void closeSession() {
		Session currentSession = sessionTracker.getOpenSession();
		if (currentSession != null) {
			currentSession.close();
		}
	}

	/**
	 * Closes the current session as well as clearing the token cache.
	 */
	protected final void closeSessionAndClearTokenInformation() {
		Session currentSession = sessionTracker.getOpenSession();
		if (currentSession != null) {
			currentSession.closeAndClearTokenInformation();
		}
	}

	/**
	 * Gets the permissions associated with the current session or null if no
	 * session has been created.
	 * 
	 * @return the permissions associated with the current session
	 */
	protected final List<String> getSessionPermissions() {
		Session currentSession = sessionTracker.getSession();
		return (currentSession != null) ? currentSession.getPermissions()
				: null;
	}

	/**
	 * Opens a new session. This method will use the application id from the
	 * associated meta-data value and an empty list of permissions.
	 * <p>
	 * If no session exists for this Activity, or if the current session has
	 * been closed, this will create a new Session object and set it as the
	 * active session. If a session exists for this Activity but is not yet
	 * open, this will try to open the session. If a session is already open for
	 * this Activity, this does nothing.
	 * </p>
	 */
	protected final void openSession() {
		openSessionForRead(null, null);
	}

	/**
	 * Opens a new session with read permissions. If either applicationID or
	 * permissions is null, this method will default to using the values from
	 * the associated meta-data value and an empty list respectively.
	 * <p>
	 * If no session exists for this Activity, or if the current session has
	 * been closed, this will create a new Session object and set it as the
	 * active session. If a session exists for this Activity but is not yet
	 * open, this will try to open the session. If a session is already open for
	 * this Activity, this does nothing.
	 * </p>
	 * 
	 * @param applicationId
	 *            the applicationID, can be null
	 * @param permissions
	 *            the permissions list, can be null
	 */
	protected final void openSessionForRead(String applicationId,
			List<String> permissions) {
		if (applicationId == null) {
			applicationId = getResources().getString(R.string.app_id);
		}
		openSessionForRead(applicationId, permissions,
				SessionLoginBehavior.SSO_WITH_FALLBACK,
				Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
	}

	/**
	 * Opens a new session with read permissions. If either applicationID or
	 * permissions is null, this method will default to using the values from
	 * the associated meta-data value and an empty list respectively.
	 * <p>
	 * If no session exists for this Activity, or if the current session has
	 * been closed, this will create a new Session object and set it as the
	 * active session. If a session exists for this Activity but is not yet
	 * open, this will try to open the session. If a session is already open for
	 * this Activity, this does nothing.
	 * </p>
	 * 
	 * @param applicationId
	 *            the applicationID, can be null
	 * @param permissions
	 *            the permissions list, can be null
	 * @param behavior
	 *            the login behavior to use with the session
	 * @param activityCode
	 *            the activity code to use for the SSO activity
	 */
	protected final void openSessionForRead(String applicationId,
			List<String> permissions, SessionLoginBehavior behavior,
			int activityCode) {
		openSession(applicationId, permissions, behavior, activityCode,
				SessionAuthorizationType.READ);
	}

	/**
	 * Opens a new session with publish permissions. If the applicationID is
	 * null, this method will default to using the value from the associated
	 * meta-data value. The permissions list cannot be null.
	 * <p>
	 * If no session exists for this Activity, or if the current session has
	 * been closed, this will create a new Session object and set it as the
	 * active session. If a session exists for this Activity but is not yet
	 * open, this will try to open the session. If a session is already open for
	 * this Activity, this does nothing.
	 * </p>
	 * 
	 * @param applicationId
	 *            the applicationID, can be null
	 * @param permissions
	 *            the permissions list, cannot be null
	 */
	protected final void openSessionForPublish(String applicationId,
			List<String> permissions) {
		if (applicationId == null) {
			applicationId = getResources().getString(R.string.app_id);
		}
		openSessionForPublish(applicationId, permissions,
				SessionLoginBehavior.SSO_WITH_FALLBACK,
				Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
	}

	/**
	 * Opens a new session with publish permissions. If the applicationID is
	 * null, this method will default to using the value from the associated
	 * meta-data value. The permissions list cannot be null.
	 * <p>
	 * If no session exists for this Activity, or if the current session has
	 * been closed, this will create a new Session object and set it as the
	 * active session. If a session exists for this Activity but is not yet
	 * open, this will try to open the session. If a session is already open for
	 * this Activity, this does nothing.
	 * </p>
	 * 
	 * @param applicationId
	 *            the applicationID, can be null
	 * @param permissions
	 *            the permissions list, cannot be null
	 * @param behavior
	 *            the login behavior to use with the session
	 * @param activityCode
	 *            the activity code to use for the SSO activity
	 */
	protected final void openSessionForPublish(String applicationId,
			List<String> permissions, SessionLoginBehavior behavior,
			int activityCode) {
		openSession(applicationId, permissions, behavior, activityCode,
				SessionAuthorizationType.PUBLISH);
	}

	private void openSession(String applicationId, List<String> permissions,
			SessionLoginBehavior behavior, int activityCode,
			SessionAuthorizationType authType) {
		Session currentSession = sessionTracker.getSession();
		if (currentSession == null || currentSession.getState().isClosed()) {
			Session session = new Session.Builder(this).setApplicationId(
					applicationId).build();
			Session.setActiveSession(session);
			currentSession = session;
		}
		if (!currentSession.isOpened()) {
			Session.OpenRequest openRequest = new Session.OpenRequest(this)
					.setPermissions(permissions).setLoginBehavior(behavior)
					.setRequestCode(activityCode);
			if (SessionAuthorizationType.PUBLISH.equals(authType)) {
				currentSession.openForPublish(openRequest);
			} else {
				currentSession.openForRead(openRequest);
			}
		}
	}

	/**
	 * Request Me : Load My FaceBook Info
	 * If active session is empty, then it opens session with "email" permission automatically.
	 * After session is opened, this functions is called again.
	 */
	protected void requestMe() {
		List<String> perms = Arrays.asList("email");
		Session currentSession = sessionTracker.getSession();
		if (currentSession == null || currentSession.getState().isClosed()
				|| !currentSession.getPermissions().containsAll(perms)) {
			Log.i("FBModule", "Active Session is null");
			sGraphAPI = GRAPHAPI_REQUEST_ME;
			openSessionForRead(getResources().getString(R.string.app_id), perms);
			return;
		}
		Log.i("FBModule", "Permissions"
				+ Session.getActiveSession().getPermissions());
		showProgressDlg(getResources().getString(
				R.string.progressdlg_fetching_user_info));
		Request req = Request.newMeRequest(Session.getActiveSession(),
				new Request.GraphUserCallback() {
					@Override
					public void onCompleted(GraphUser user, Response response) {
						onRequestMeCompleted(user, response);
						dismissProgressDlg();
					}
				});
		Request.executeBatchAsync(req);
	}

	protected void onRequestMeCompleted(GraphUser user, Response response) {

	}

	/**
	 * 
	 */

	private void showProgressDlg(String msg) {
		if (progressDlg == null) {
			progressDlg = new ProgressDialog(this);

		}
		progressDlg.setMessage(msg);
		progressDlg.show();
	}

	private void dismissProgressDlg() {
		if (progressDlg != null) {
			progressDlg.dismiss();
		}
	}

	/**
	 * The default callback implementation for the session.
	 */
	private class DefaultSessionStatusCallback implements
			Session.StatusCallback {

		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			FacebookActivity.this.onSessionStateChange(state, exception);
		}

	}
}
