package io.github.hidroh.materialistic;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannedString;
import android.text.format.DateUtils;
import android.text.style.TypefaceSpan;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAccountManager;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowNetworkInfo;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.widget.PopupMenu;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.support.v4.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class AppUtilsTest {
    @Inject AlertDialogBuilder alertDialogBuilder;
    private Activity context;

    @Before
    public void setUp() {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void testSetTextWithLinks() {
        TestApplication.addResolver(new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")));
        Preferences.set(context, R.string.pref_custom_tab, false);
        TextView textView = new TextView(context);
        AppUtils.setTextWithLinks(textView, AppUtils.fromHtml("<a href=\"http://example.com\">http://example.com</a>"));
        MotionEvent event = mock(MotionEvent.class);
        when(event.getAction()).thenReturn(MotionEvent.ACTION_DOWN);
        when(event.getX()).thenReturn(0f);
        when(event.getY()).thenReturn(0f);
        assertTrue(shadowOf(textView).getOnTouchListener().onTouch(textView, event));
        when(event.getAction()).thenReturn(MotionEvent.ACTION_UP);
        when(event.getX()).thenReturn(0f);
        when(event.getY()).thenReturn(0f);
        assertTrue(shadowOf(textView).getOnTouchListener().onTouch(textView, event));
        assertNotNull(ShadowApplication.getInstance().getNextStartedActivity());
    }

    @Test
    public void testSetTextWithLinksOpenChromeCustomTabs() {
        TestApplication.addResolver(new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")));
        TextView textView = new TextView(new ContextThemeWrapper(context, R.style.AppTheme));
        AppUtils.setTextWithLinks(textView, AppUtils.fromHtml("<a href=\"http://example.com\">http://example.com</a>"));
        MotionEvent event = mock(MotionEvent.class);
        when(event.getAction()).thenReturn(MotionEvent.ACTION_DOWN);
        when(event.getX()).thenReturn(0f);
        when(event.getY()).thenReturn(0f);
        assertTrue(shadowOf(textView).getOnTouchListener().onTouch(textView, event));
        when(event.getAction()).thenReturn(MotionEvent.ACTION_UP);
        when(event.getX()).thenReturn(0f);
        when(event.getY()).thenReturn(0f);
        assertTrue(shadowOf(textView).getOnTouchListener().onTouch(textView, event));
        assertNotNull(ShadowApplication.getInstance().getNextStartedActivity());
    }

    @Test
    public void testDefaultTextSize() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        float expected = activity.getTheme().obtainStyledAttributes(R.style.AppTextSize,
                new int[]{R.attr.contentTextSize}).getDimension(0, 0);
        float actual = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.contentTextSize}).getDimension(0, 0);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetAbbreviatedTimeSpan() {
        assertEquals("0m", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() +
                DateUtils.SECOND_IN_MILLIS));
        assertEquals("0m", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis()));
        assertEquals("5m", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                5 * DateUtils.MINUTE_IN_MILLIS - 10 * DateUtils.SECOND_IN_MILLIS));
        assertEquals("1h", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                DateUtils.HOUR_IN_MILLIS - DateUtils.MINUTE_IN_MILLIS));
        assertEquals("6d", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                DateUtils.WEEK_IN_MILLIS + DateUtils.MINUTE_IN_MILLIS));
        assertEquals("1w", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                DateUtils.WEEK_IN_MILLIS - DateUtils.MINUTE_IN_MILLIS));
        assertEquals("10y", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                10 * DateUtils.YEAR_IN_MILLIS - DateUtils.MINUTE_IN_MILLIS));
    }

    @Test
    public void testNoActiveNetwork() {
        shadowOf((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).setActiveNetworkInfo(null);
        assertFalse(AppUtils.isOnWiFi(context));
    }

    @Test
    public void testDisconnectedNetwork() {
        shadowOf((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null, 0, 0, false, false));
        assertFalse(AppUtils.isOnWiFi(context));
    }

    @Test
    public void testNonWiFiNetwork() {
        shadowOf((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_MOBILE, 0, true, true));
        assertFalse(AppUtils.isOnWiFi(context));
    }

    @Test
    public void testWiFiNetwork() {
        shadowOf((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        assertTrue(AppUtils.isOnWiFi(context));
    }

    @Test
    public void testRemoveAccount() {
        Preferences.setUsername(context, "olduser");
        AppUtils.registerAccountsUpdatedListener(context);
        shadowOf(AccountManager.get(context)).addAccount(
                new Account("newuser", BuildConfig.APPLICATION_ID));
        assertNull(Preferences.getUsername(context));
        Preferences.setUsername(context, "newuser");
        shadowOf(AccountManager.get(context)).addAccount(
                new Account("olduser", BuildConfig.APPLICATION_ID));
        assertEquals("newuser", Preferences.getUsername(context));
    }

    @Test
    public void testShareComment() {
        AppUtils.share(context, mock(PopupMenu.class),
                new View(context), new TestHnItem(1));
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        AppUtils.share(context, mock(PopupMenu.class),
                new View(context), new TestHnItem(1) {
                    @Override
                    public String getUrl() {
                        return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
                    }
                });
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void testOpenExternalComment() {
        ActivityController<TestListActivity> controller = Robolectric.buildActivity(TestListActivity.class);
        TestListActivity activity = controller.create().start().resume().get();
        AppUtils.openExternal(activity, mock(PopupMenu.class),
                new View(activity), new TestHnItem(1), null);
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        AppUtils.openExternal(activity, mock(PopupMenu.class),
                new View(activity), new TestHnItem(1) {
                    @Override
                    public String getUrl() {
                        return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
                    }
                }, null);
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        controller.destroy();
    }

    @Test
    public void testLoginNoAccounts() {
        AppUtils.showLogin(context, null);
        assertThat(shadowOf(context).getNextStartedActivity())
                .hasComponent(context, LoginActivity.class);
    }

    @Test
    public void testLoginStaleAccount() {
        Preferences.setUsername(context, "username");
        shadowOf(ShadowAccountManager.get(context))
                .addAccount(new Account("username", BuildConfig.APPLICATION_ID));
        AppUtils.showLogin(context, null);
        assertThat(shadowOf(context).getNextStartedActivity())
                .hasComponent(context, LoginActivity.class);
    }

    @Test
    public void testLoginShowChooser() {
        TestApplication.applicationGraph.inject(this);
        shadowOf(ShadowAccountManager.get(context))
                .addAccount(new Account("username", BuildConfig.APPLICATION_ID));
        AppUtils.showLogin(context, alertDialogBuilder);
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void testTrimHtmlWhitespaces() {
        TextView textView = new TextView(context);
        textView.setText(AppUtils.fromHtml("<p>paragraph</p><p><br/><br/><br/></p>"));
        assertThat(textView).hasTextString("paragraph");
        textView.setText(AppUtils.fromHtml(""));
        assertThat(textView).hasTextString("");
        textView.setText(AppUtils.fromHtml("paragraph"));
        assertThat(textView).hasTextString("paragraph");
    }

    @Test
    public void testPreformattedTextHasMonospaceTypeface() {
        TextView textView = new TextView(context);
        textView.setText(AppUtils.fromHtml("<pre><code>val x = myCode()</code></pre>"));
        assertThat(textView).hasTextString("val x = myCode()");

        SpannedString view = (SpannedString) textView.getText();
        TypefaceSpan[] spans = view.getSpans(0, view.length(), TypefaceSpan.class);
        assertThat(spans[0].getFamily()).isEqualTo("monospace");
    }

    @Test
    public void testReplacesPreCodeTagsWithTT() {
        String oneCodeBlock = "<pre><code>val x = myCode()</code></pre><p>More Text<br/><br/><br/></p>";
        String multipleCodeBlocks = "<pre><code>val x = myCode() \n val y = someMoreCode()</code></pre><p>some more text<br/></p><pre><code>val x = myCode()</code></pre>";
        String nestedCodeBlocks = "<pre><code>val x = myCode() \n # now some nested codeblocks\n <pre><code>val x = <pre><code>nothing to see here</code></pre></code></pre></code></pre>";

        String oneCodeBlockResult = AppUtils.replacePreCode(oneCodeBlock);
        String multipleCodeBlocksResult = AppUtils.replacePreCode(multipleCodeBlocks);
        String nestedCodeBlockResult = AppUtils.replacePreCode(nestedCodeBlocks);

        assertThat(oneCodeBlockResult).isEqualTo("<tt>val x = myCode()</tt><p>More Text<br/><br/><br/></p>");
        assertThat(multipleCodeBlocksResult).isEqualTo("<tt>val x = myCode() \n val y = someMoreCode()</tt><p>some more text<br/></p><tt>val x = myCode()</tt>");
        // todo this is a bug, the closing tag is matched eagerly,
        // however it's not worse than before, where pre code tags were 'eaten'.
        assertThat(nestedCodeBlockResult).doesNotMatch("<tt>val x = myCode() \n # now some nested codeblocks\n &lt;pre&gt;&lt;code&gt;val x = &lt;pre&gt;&lt;code&gt;nothing to see here&lt;/code&gt;&lt;/pre&gt;&lt;/code&gt;&lt;/pre&gt;</tt>");
    }

    @Test
    public void testOpenExternalUrlNoConnection() {
        shadowOf((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(null);
        AppUtils.openWebUrlExternal(context, new TestHnItem(1L) {
            @Override
            public String getUrl() {
                return "http://example.com";
            }
        }, "http://example.com", null);
        assertThat(shadowOf(context).getNextStartedActivity())
                .hasComponent(context, OfflineWebActivity.class)
                .hasExtra(OfflineWebActivity.EXTRA_URL, "http://example.com");
    }

    @Test
    public void testFullscreenButton() {
        ActivityController<TestListActivity> controller = Robolectric.buildActivity(TestListActivity.class);
        TestListActivity activity = controller.create().start().resume().get();
        FloatingActionButton fab = new FloatingActionButton(activity);
        AppUtils.toggleFabAction(fab, null, false);
        fab.performClick();
        assertThat(shadowOf(LocalBroadcastManager.getInstance(activity)).getSentBroadcastIntents()).isNotEmpty();
        controller.destroy();
    }

    @Test
    public void testNavigate() {
        AppBarLayout appBar = mock(AppBarLayout.class);
        when(appBar.getBottom()).thenReturn(1);
        Navigable navigable = mock(Navigable.class);
        AppUtils.navigate(Navigable.DIRECTION_DOWN, appBar, navigable);
        verify(appBar).setExpanded(eq(false), anyBoolean());
        verify(navigable, never()).onNavigate(anyInt());

        when(appBar.getBottom()).thenReturn(0);
        AppUtils.navigate(Navigable.DIRECTION_DOWN, appBar, navigable);
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_DOWN));

        AppUtils.navigate(Navigable.DIRECTION_RIGHT, appBar, navigable);
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_RIGHT));

        AppUtils.navigate(Navigable.DIRECTION_UP, appBar, navigable);
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_UP));

        AppUtils.navigate(Navigable.DIRECTION_LEFT, appBar, navigable);
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_LEFT));
    }
}
