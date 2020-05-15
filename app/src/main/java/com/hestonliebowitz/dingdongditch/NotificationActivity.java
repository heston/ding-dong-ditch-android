package com.hestonliebowitz.dingdongditch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class NotificationActivity extends AppCompatActivity {

    private String TAG = "NotificationActivity";
    private DataService mData;
    private String currentEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        mData = new DataService(this);
        final ImageView imageView = (ImageView) findViewById(R.id.image);
        Intent intent = getIntent();
        currentEventId = intent.getStringExtra("eventId");

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button unlockButton = (Button) findViewById(R.id.unlock_button);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mData.unlockGate();
            }
        });

        mData.getImage(currentEventId, new OnSuccessListener<Bitmap>() {
            @Override
            public void onSuccess(Bitmap bm) {
                imageView.setImageBitmap(bm);
            }
        }, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                imageView.setImageResource(R.drawable.unavailable);
            }
        });

        bindLastSeenValue();
    }

    private void bindLastSeenValue() {
        DatabaseReference dbRef = mData.getEvent(currentEventId);
        if (dbRef == null) {
            return;
        }

        final TextView occurredAt = findViewById(R.id.occurred_at);

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Event snapshot = dataSnapshot.getValue(Event.class);
                Log.i(TAG, String.format("bindLastSeenValue:onDataChange:%s", snapshot));
                String formattedDate = (snapshot == null) ?
                        getResources().getString(R.string.unknown) :
                        snapshot.getFormattedDate();

                occurredAt.setText(
                        String.format(getString(R.string.occurred_on) + ": %s", formattedDate)
                );
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                occurredAt.setText(R.string.unknown);
                Log.w(TAG, "bindLastSeenValue:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                TaskStackBuilder.create(this)
                        // Add all of this activity's parents to the back stack
                        .addNextIntentWithParentStack(upIntent)
                        // Navigate up to the closest parent
                        .startActivities();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
