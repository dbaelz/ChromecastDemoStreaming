package de.inovex.chromecast.demostreaming;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String APP_ID = "APP_ID";

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MenuItem mMediaRouteItem;
    private MediaRouteButton mMediaRouteButton;
    private MediaRouter.Callback mMediaRouterCallback;
    private Cast.Listener mCastListener;
    private CastDevice mDevice;
    private GoogleApiClient mApiClient;

    private RemoteMediaPlayer mRemoteMediaPlayer;
    private MediaMetadata mMediaMetadata;
    private Button mPauseButton;
    private boolean isPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(APP_ID)).build();
        mMediaRouterCallback = new CustomMediaRouterCallback();

        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
            @Override
            public void onStatusUpdated() {
                MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                isPlaying = mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING;
            }
        });

        mPauseButton = (Button)findViewById(R.id.control_pause);
        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mApiClient != null) && (mApiClient.isConnected()) && (mRemoteMediaPlayer != null)) {
                    try {
                        if (isPlaying) {
                            mPauseButton.setText(getResources().getString(R.string.control_playvideo));
                            mRemoteMediaPlayer.pause(mApiClient);
                        } else {
                            mPauseButton.setText(getResources().getString(R.string.control_pausevideo));
                            mRemoteMediaPlayer.play(mApiClient);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);

        mMediaRouteItem = menu.findItem(R.id.action_mediaroute_cast);
        mMediaRouteButton = (MediaRouteButton) mMediaRouteItem.getActionView();
        mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mApiClient != null) {
            Cast.CastApi.launchApplication(mApiClient, APP_ID).setResultCallback(
                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                        @Override
                        public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                            Status status = applicationConnectionResult.getStatus();
                            if (status.isSuccess()) {
                                try {
                                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);

                                    mMediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
                                    mMediaMetadata.putString(MediaMetadata.KEY_TITLE, "Demo Video");
                                    MediaInfo mediaInfo = new MediaInfo.Builder(
                                            "http://example.com/video.mp4")
                                            .setContentType("video/mp4")
                                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                                            .setMetadata(mMediaMetadata)
                                            .build();
                                    try {
                                        mRemoteMediaPlayer.load(mApiClient, mediaInfo, true)
                                                .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                                                    @Override
                                                    public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                                                        Status status = mediaChannelResult.getStatus();
                                                        if (status.isSuccess()) {
                                                            mPauseButton.setVisibility(View.VISIBLE);
                                                        }
                                                    }
                                                });
                                    } catch (Exception e) {
                                        Log.e(TAG, "Problem while loading media", e);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                endSession();
                            }
                        }
                    }
            );
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void startSession() {
        mCastListener = new Cast.Listener() {
            @Override
            public void onApplicationDisconnected(int errorCode) {
                endSession();
            }
        };

        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(mDevice, mCastListener);
        mApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();
    }

    private void endSession() {
        if (mApiClient != null) {
            mPauseButton.setVisibility(View.GONE);
            Cast.CastApi.stopApplication(mApiClient);
            mApiClient.disconnect();
            mApiClient = null;
            mDevice = null;
        }
    }

    private class CustomMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            mMediaRouteItem.setVisible(true);
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            endSession();
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            mDevice = CastDevice.getFromBundle(route.getExtras());
            startSession();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            mDevice = null;
            endSession();
        }
    }
}
