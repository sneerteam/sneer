package location;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import sneer.location.R;


public class FollowMeActivity extends Activity {

    Intent service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_me);
        service = new Intent(this, LocationService.class);
        startService(service);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final ImageView map = (ImageView) findViewById(R.id.map_view);

        map.post(new Runnable() {
            @Override
            public void run() {
                int width = map.getMeasuredWidth();
                int height = map.getMeasuredHeight();
                new MapDownloader(map, width, height).execute(
                    getMapURL("-29.702400", "-52.439495", "-23.191420", "-46.880960", width, height, false)
                );
            }
        });

    }

    protected String getMapURL(String latitudeA, String longitudeA, String latitudeB, String longitudeB, int width, int height, boolean displayRoute) {
        if (width > height) {
            height = 640 * height/width;
            width = 640;
        }
        else {
            width = 640 * width/height;
            height = 640;
        }

        String url = "https://maps.googleapis.com/maps/api/staticmap";
        url += "?size=" + width + "x" + height + "&scale=2";
        url += "&maptype=roadmap";
        url += "&markers=size:mid%7Ccolor:red%7C" + latitudeA + "," + longitudeA;
        url += "&markers=size:mid%7Ccolor:blue%7C" + latitudeB + "," + longitudeB;
        if (displayRoute) {
            url += "&path=color:0x0000ff%7Cweight:5%7C" + latitudeA + "," + longitudeA + "%7C" + latitudeB + "," + longitudeB;
        }
        return url;
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(service); // Remove this later
    }
}
