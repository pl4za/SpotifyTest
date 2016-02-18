package com.pl4za.spotifast;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.pl4za.help.Params;
import com.pl4za.interfaces.NetworkRequests;
import com.pl4za.volley.AppController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jasoncosta on 2/3/2016.
 */
public class SpotifyNetworkRequests {

    private static final String TAG = "SpotifyNetworkRequests";
    private static final int MY_SOCKET_TIMEOUT_MS = 8000;
    private final ArrayList<NetworkRequests> networkRequests = new ArrayList<>();
    private int statusCode;
    private static final SpotifyNetworkRequests INSTANCE = new SpotifyNetworkRequests();

    public static SpotifyNetworkRequests getInstance() {
        return INSTANCE;
    }

    private SpotifyNetworkRequests() {
    }

    public void addNetworkListener(NetworkRequests networkRequests) {
        this.networkRequests.add(networkRequests);
    }

    public void exchangeCodeForToken(final String code) {
        String url = "https://accounts.spotify.com/api/token";
        Response.Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    JSONObject jsonObj = new JSONObject(list);
                    String access_token = jsonObj.getString("access_token");
                    String refresh_token = jsonObj.getString("refresh_token");
                    for (NetworkRequests n : networkRequests) {
                        n.onTokenReceived(access_token, refresh_token);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(Params.TAG_exchangeCodeForToken, "Error: " + error.getMessage());
            }
        };

        StringRequest fileRequest = new StringRequest(Request.Method.POST, url, jsonListerner, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("grant_type", "authorization_code");
                params.put("code", code);
                params.put("redirect_uri", Params.REDIRECT_URI);
                params.put("client_id", Params.CLIENT_ID);
                params.put("client_secret", Params.CLIENT_SECRET);
                return params;
            }
        };
        fileRequest.setShouldCache(false);
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, Params.TAG_exchangeCodeForToken);
    }

    public void refreshToken(final String refreshToken) {
        String url = "https://accounts.spotify.com/api/token";
        Response.Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    JSONObject jsonObj = new JSONObject(list);
                    String access_token = jsonObj.getString("access_token");
                    for (NetworkRequests n : networkRequests) {
                        n.onTokenRefresh(access_token);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(Params.TAG_refreshToken, "Error: " + error.getMessage());
            }
        };

        StringRequest fileRequest = new StringRequest(Request.Method.POST, url, jsonListerner, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("grant_type", "refresh_token");
                params.put("refresh_token", refreshToken);
                params.put("client_id", Params.CLIENT_ID);
                params.put("client_secret", Params.CLIENT_SECRET);
                return params;
            }
        };
        fileRequest.setShouldCache(false);
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, Params.TAG_refreshToken);
    }

    public void getCurrentUserProfile(final String accessToken) {
        String url = "https://api.spotify.com/v1/me";
        //createPDialog("Loading user...");
        Response.Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    JSONObject jsonObj = new JSONObject(list);
                    String userID = jsonObj.getString("id");
                    String product = jsonObj.getString("product");
                    String profilePicture = "";
                    try {
                        JSONArray profilePictures = jsonObj.getJSONArray("images");
                        profilePicture = profilePictures.getJSONObject(0).getString("url");
                    } catch (JSONException e) {
                        Log.e(TAG, "No profile image");
                    }
                    for (NetworkRequests n : networkRequests) {
                        n.onProfileReceived(userID, product, profilePicture);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(Params.TAG_getCurrentUserProfile, "Error: " + error.getMessage());
                //hidePDialog();
                //refreshToken("getUserFailed");
            }
        };

        StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }

            @Override
            public Priority getPriority() {
                return Priority.NORMAL;
            }
        };
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Adding request to request TRACK_LIST
        AppController.getInstance().addToRequestQueue(fileRequest, Params.TAG_getCurrentUserProfile);
    }

    private void getCurrentUserPlaylists(String nextURL, final ArrayList<Playlist> playlists, final String accessToken) {
        Response.Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    JSONObject json = new JSONObject(list);
                    JSONArray jsonArr = json.getJSONArray("items");
                    int size = jsonArr.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject ids = jsonArr.getJSONObject(i);
                        playlists.add(new Playlist(ids.getString("id"), ids.getString("name"), ids.getJSONObject("owner").getString("id")));
                    }
                    String nextURL = json.getString("next");
                    if (!nextURL.equals("null")) {
                        getCurrentUserPlaylists(nextURL, playlists, accessToken);
                    } else {
                        for (NetworkRequests n : networkRequests) {
                            n.onPlaylistsReceived(playlists);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(Params.TAG_getCurrentUserNextPlaylists, "Error: " + error.getMessage());
            }
        };
        StringRequest fileRequest = new StringRequest(Request.Method.GET, nextURL, jsonListerner, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        fileRequest.setShouldCache(false);
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, Params.TAG_getCurrentUserPlaylists);
    }

    public void getCurrentUserPlaylists(String user_id, final String accessToken) {
        String url = "https://api.spotify.com/v1/users/" + user_id + "/playlists";
        Response.Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    ArrayList<Playlist> playlists = new ArrayList<>();
                    JSONObject json = new JSONObject(list);
                    JSONArray jsonArr = json.getJSONArray("items");
                    int size = jsonArr.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject ids = jsonArr.getJSONObject(i);
                        playlists.add(new Playlist(ids.getString("id"), ids.getString("name"), ids.getJSONObject("owner").getString("id")));
                    }
                    String nextURL = json.getString("next");
                    if (!nextURL.isEmpty()) {
                        getCurrentUserPlaylists(nextURL, playlists, accessToken);
                    } else {
                        for (NetworkRequests n : networkRequests) {
                            n.onPlaylistsReceived(playlists);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(Params.TAG_getCurrentUserPlaylists, "Error: " + error.getMessage());
            }
        };
        StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        fileRequest.setShouldCache(false);
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        if (!user_id.isEmpty()) {
            AppController.getInstance().addToRequestQueue(fileRequest, Params.TAG_getCurrentUserPlaylists);
        }
    }

    public void getArtistPicture(String artistID) {
        String url = "https://api.spotify.com/v1/artists/" + artistID;
        Response.Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    JSONObject json = new JSONObject(list);
                    try {
                        JSONArray artistImages = json.getJSONArray("images");
                        JSONObject artistimageObject = artistImages.getJSONObject(1);
                        String artistImageUrl = artistimageObject.getString("url");
                        for (NetworkRequests n : networkRequests) {
                            n.onRandomArtistPictureURLReceived(artistImageUrl);
                        }
                    } catch (JSONException e) {
                        Log.e("json", "No artist image...");
                        for (NetworkRequests n : networkRequests) {
                            n.onRandomArtistPictureURLReceived("");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                for (NetworkRequests n : networkRequests) {
                    n.onRandomArtistPictureURLReceived("none");
                }
            }
        };
        StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener);
        fileRequest.setShouldCache(false);
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, Params.TAG_getArtistID);

    }

    public void getSelectedPlaylistTracks(String url, final String accessToken, final String etag) {
        Response.Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                for (NetworkRequests n : networkRequests) {
                    n.onPlaylistTracksReceived(list);
                }
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //TODO: 304 = refresh token
            }
        };

        // Main request
        StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                statusCode = response.statusCode;
                Log.i("FragmentTracks", "status: " + statusCode);
                if (statusCode == 200) {
                    Map<String, String> responseHeaders = response.headers;
                    for (NetworkRequests n : networkRequests) {
                        n.onEtagUpdate(responseHeaders.get("ETag"));
                    }
                }
                return super.parseNetworkResponse(response);
            }
        };
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, Params.TAG_getSelectedPlaylistTracks);
    }

}
