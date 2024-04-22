package com.moutamid.emailfetcher;

import static com.moutamid.emailfetcher.MainActivity.PREF_ACCOUNT_NAME;
import static com.moutamid.emailfetcher.MainActivity.REQUEST_AUTHORIZATION;
import static com.moutamid.emailfetcher.MainActivity.REQUEST_GOOGLE_PLAY_SERVICES;
import static com.moutamid.emailfetcher.MainActivity.SCOPES;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fxn.stash.Stash;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.moutamid.emailfetcher.databinding.ActivityEmailListBinding;
import com.moutamid.emailfetcher.model.Message;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

public class EmailListActivity extends AppCompatActivity {
    ActivityEmailListBinding binding;
    List<Message> messageList;
    MessagesAdapter messagesAdapter;
    GoogleAccountCredential mCredential;
    Gmail mService;
    Utils mUtils;
    String pageToken = null;
    boolean isFetching = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailListBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        mService = null;

        mUtils = new Utils(this);

        String accountName = Stash.getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);

//            HttpTransport transport = AndroidHttp.newCompatibleTransport();
         //   JsonFactory jsonFactory = new AndroidJsonFactory(); // TODO JacksonFactory.getDefaultInstance()
//            mService = new com.google.api.services.gmail.Gmail.Builder(
//                    transport, jsonFactory, mCredential)
//                    .setApplicationName("MailBox App")
//                    .build();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        messageList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(this, messageList);

        getMessagesFromDB();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    if (!isFetching && mUtils.isDeviceOnline()) {
                        getMessagesFromDB();
                    } else
                        mUtils.showSnackbar(binding.getRoot(), getString(R.string.device_is_offline));
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.app_requires_auth);
                    builder.setPositiveButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                break;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void getMessagesFromDB() {
//        refreshMessages.setRefreshing(true);
//        this.messageList.clear();
//        this.messageList.addAll(SQLite.select().from(Message.class).queryList());
//        this.messagesAdapter.notifyDataSetChanged();
//        this.refreshMessages.setRefreshing(false);

        if (mUtils.isDeviceOnline())
            new GetEmailsTask(true).execute();
        else
            mUtils.showSnackbar(binding.getRoot(), getString(R.string.device_is_offline));
    }

    private static final String TAG = "EmailListActivity";
    @SuppressLint("StaticFieldLeak")
    private class GetEmailsTask extends AsyncTask<Void, Void, List<Message>> {

        private int itemCount = 0;
        private boolean clear;
        private Exception mLastError = null;

        GetEmailsTask(boolean clear) {
            this.clear = clear;
        }

        @Override
        protected List<Message> doInBackground(Void... voids) {
            isFetching = true;
            List<Message> messageListReceived = null;

            try {
                String user = "me";
                String query = "in:inbox";
                ListMessagesResponse messageResponse = mService.users().messages().list(user).setQ(query).setMaxResults(20L).setPageToken(pageToken).execute();
                pageToken = messageResponse.getNextPageToken();

                messageListReceived = new ArrayList<>();
                List<com.google.api.services.gmail.model.Message> receivedMessages = messageResponse.getMessages();
                for (com.google.api.services.gmail.model.Message message : receivedMessages) {
                    com.google.api.services.gmail.model.Message actualMessage = mService.users().messages().get(user, message.getId()).execute();

                    Message newMessage = getMessage(actualMessage);
                    messageListReceived.add(newMessage);
                    itemCount++;
                }
            } catch (Exception e) {
                Log.w(TAG, e);
                mLastError = e;
                cancel(true);
            }

            return messageListReceived;
        }

        @Override
        protected void onPostExecute(List<Message> output) {
            isFetching = false;

            if (output != null && !output.isEmpty()) {
                if (clear) {
                    messageList.clear();
                    messageList.addAll(output);
                    messagesAdapter.notifyDataSetChanged();
                } else {
                    int listSize = messageList.size();
                    messageList.addAll(output);
                    messagesAdapter.notifyItemRangeInserted(listSize, itemCount);
                }
                
            } else {
                
                mUtils.showSnackbar(binding.getRoot(), getString(R.string.fetch_failed));
            }
        }

        @Override
        protected void onCancelled() {
            isFetching = false;

            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    mUtils.showSnackbar(binding.getRoot(), getString(R.string.an_error_occurred));
                }
            } else {
                mUtils.showSnackbar(binding.getRoot(), getString(R.string.an_error_occurred));
            }
        }

    }

    @NonNull
    private Message getMessage(com.google.api.services.gmail.model.Message actualMessage) throws JSONException {
        Map<String, String> headers = new HashMap<>();
        for (MessagePartHeader messagePartHeader : actualMessage.getPayload().getHeaders())
            headers.put(
                    messagePartHeader.getName(), messagePartHeader.getValue()
            );

        Message newMessage = new Message(
                actualMessage.getLabelIds(),
                actualMessage.getSnippet(),
                actualMessage.getPayload().getMimeType(),
                headers,
                actualMessage.getPayload().getParts(),
                actualMessage.getInternalDate(),
                mUtils.getRandomMaterialColor(),
                actualMessage.getPayload()
        );
        return newMessage;
    }

}