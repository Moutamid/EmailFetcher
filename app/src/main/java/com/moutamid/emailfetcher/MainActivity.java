package com.moutamid.emailfetcher;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fxn.stash.Stash;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.moutamid.emailfetcher.databinding.ActivityMainBinding;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private static final int RC_SIGN_IN = 123;

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "MainActivity";

    GoogleAccountCredential mCredential;
    public static final String[] SCOPES = {GmailScopes.MAIL_GOOGLE_COM};
    public static final String PREF_ACCOUNT_NAME = "accountName";

    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestId()
                .requestScopes(new Scope(GmailScopes.MAIL_GOOGLE_COM))
//                .requestServerAuthCode(getString(R.string.server_client_id))
//                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();

        // Build GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        String accountName = Stash.getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);
            startActivity(new Intent(MainActivity.this, EmailListActivity.class));
            finish();
        }

        binding.signInButton.setOnClickListener(v -> {
            signIn();
//            if (!isGooglePlayServicesAvailable()) {
//                acquireGooglePlayServices();
//            } else {
//                chooseAccount();
//            }
        });

    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(this, android.Manifest.permission.GET_ACCOUNTS)) {
            // Start a dialog from which the user can choose an account
            startActivityForResult(
                    mCredential.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    android.Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        switch (requestCode) {
//            case REQUEST_GOOGLE_PLAY_SERVICES:
//                if (resultCode != RESULT_OK) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                    builder.setMessage(R.string.this_app_requires_google_play_services);
//                    builder.setPositiveButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                            finish();
//                        }
//                    });
//                    AlertDialog dialog = builder.create();
//                    dialog.show();
//                } else {
//                    chooseAccount();
//                }
//                break;
//            case REQUEST_ACCOUNT_PICKER:
//                if (resultCode == RESULT_OK && data != null &&
//                        data.getExtras() != null) {
//                    String accountName =
//                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
//                    if (accountName != null) {
//                        Stash.put(PREF_ACCOUNT_NAME, accountName);
//                        mCredential.setSelectedAccountName(accountName);
//                        startActivity(new Intent(MainActivity.this, EmailListActivity.class));
//                        finish();
//                    }
//                }
//                break;
//            case REQUEST_AUTHORIZATION:
//                if (resultCode == RESULT_OK) {
//                    chooseAccount();
//                }
//                break;
//        }
//    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN && data != null) {
            GoogleSignInResult task = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (task != null) {
                GoogleSignInAccount account = task.getSignInAccount();
                if (account != null) {
                    String accessToken = account.getIdToken();
                    Toast.makeText(this, "Signed in as: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, "accessToken: " + accessToken, Toast.LENGTH_SHORT).show();

                    fetchEmail(account.getEmail());
//                        fetchEmail();
                } else {
                    Toast.makeText(this, "Something went wrong no account found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void fetchEmail() {
//        try {
//            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//            Gmail service = new Gmail.Builder(HTTP_TRANSPORT, GmailQuickstart.JSON_FACTORY, GmailQuickstart.getCredentials(HTTP_TRANSPORT))
//                    .setApplicationName(GmailQuickstart.APPLICATION_NAME)
//                    .build();
//
//            // Print the labels in the user's account.
//            String user = "me";
//            ListLabelsResponse listResponse = service.users().labels().list(user).execute();
//            List<Label> labels = listResponse.getLabels();
//            if (labels.isEmpty()) {
//                System.out.println("No labels found.");
//            } else {
//                System.out.println("Labels:");
//                for (Label label : labels) {
//                    System.out.printf("- %s\n", label.getName());
//                }
//            }
//        } catch (GeneralSecurityException | IOException e) {
//            e.printStackTrace();
//        }
    }

    private void fetchEmail(String email) {
       new Thread(() -> {
           GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                   getApplicationContext(), Collections.singleton(GmailScopes.GMAIL_READONLY));
           credential.setSelectedAccountName(email);
           Gmail service = new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential) // JacksonFactory.getDefaultInstance()
                   .setApplicationName(getString(R.string.app_name))
                   .build();

           try {
               List<Label> labels = service.users().labels().list(email).execute().getLabels();
               for (Label label : labels) {
                   if (label.getName().equalsIgnoreCase("INBOX")) {
                       List<Message> messages = service.users().messages().list(email) // result.getAccount().getEmail(), label.getId()
                               .setQ("is:unread").execute().getMessages();
                       // Process the list of messages (e.g., get subject, sender, etc.)
                       // You can use the `service.users().messages().get(result.getAccount().getEmail(), messageId).execute().getPayload()` method to get the full message details
                       Log.d(TAG, "fetchEmail: size " + messages.size());
                   }
               }
           } catch (IOException e) {
               e.printStackTrace();
           }
       }).start();
    }

}