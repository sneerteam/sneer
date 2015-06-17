package location;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;

import sneer.android.Message;
import sneer.android.PartnerSession;
import sneer.location.R;


public class FollowMeActivity extends Activity {

    private PartnerSession session;
    private Intent service;
	private String url;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_follow_me);






		startSession();


//		service = new Intent(this, LocationService.class);
//        startService(service);
    }


	private void startSession() {
		session = PartnerSession.join(this, new PartnerSession.Listener() {
			@Override
			public void onUpToDate() {
				refresh();
			}

			@Override
			public void onMessage(Message message) {
				handle(message);
			}
		});
	}

	private void refresh() {
//		Toast.makeText(this, "url->" + url, Toast.LENGTH_SHORT).show();
	}

	private void handle(Message message) {
		url = (String) message.payload();

		if (message.wasSentByMe()) {
			startService(getIntent().<Intent>getParcelableExtra("SEND_MESSAGE").setAction(url));
//			finish();
		}
		else {

		}
	}











	@Override
    protected void onResume() {
		super.onResume();
        final ImageView map = (ImageView) findViewById(R.id.map_view);

        map.post(new Runnable() {
            @Override
            public void run() {
                int width = map.getMeasuredWidth();
                int height = map.getMeasuredHeight();

				setProgressBarIndeterminate(true);
				setProgressBarIndeterminateVisibility(true);
				setProgressBarVisibility(true);

                new MapDownloader(map, width, height, FollowMeActivity.this, session).execute(
                    getMapURL("-29.702400", "-52.439495", "-23.191420", "-46.880960", width, height)
                );
            }
        });
    }



    protected String getMapURL(String latitudeA, String longitudeA, String latitudeB, String longitudeB, int width, int height) {
        if (width > height) {
            width = 640;
            height = 640 * height/width;
        }
        else {
            height = 640;
            width = 640 * width/height;
        }

        String url = "https://maps.googleapis.com/maps/api/staticmap";
        url += "?size=" + width + "x" + height + "&scale=2";
        url += "&maptype=roadmap";
        url += "&markers=size:mid%7Ccolor:red%7C" + latitudeA + "," + longitudeA;
        url += "&markers=size:mid%7Ccolor:blue%7C" + latitudeB + "," + longitudeB;

        return url;
    }

	@Override
	protected void onDestroy() {
		session.close();
		super.onDestroy();
	}

}
